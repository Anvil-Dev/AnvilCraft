package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.SimpleMagneticChuteBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Objects;

import static dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil.getTargetItemHandlerList;

@Getter
public class SimpleMagneticChuteBlockEntity extends BlockEntity implements IItemHandlerHolder {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };
    @Setter
    private int cooldown = 0;
    private long tickedGameTime;

    public SimpleMagneticChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Cooldown", cooldown);
        tag.put("Inventory", itemHandler.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cooldown = tag.getInt("Cooldown");
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }

    /**
     * tick
     */
    @SuppressWarnings("DuplicatedCode")
    public void tick() {
        if (level == null) return;
        if (cooldown > 0) cooldown--;
        tickedGameTime = level.getGameTime();
        boolean resetCD = false;
        if (cooldown <= 0 && isEnabled()) {
            // 向上方容器输出物品
            BlockPos targetPos = getBlockPos().relative(Direction.UP);
            List<IItemHandler> targetList = getTargetItemHandlerList(
                targetPos,
                Direction.DOWN,
                level
            );
            if (targetList != null && !targetList.isEmpty()) {
                for (IItemHandler target : targetList) {
                    BlockEntity targetBE = level.getBlockEntity(targetPos);
                    boolean setChuteCD = targetBE != null && isTargetEmpty(targetBE);
                    boolean success = ItemHandlerUtil.exportToTarget(getItemHandler(), 64, stack -> true, target);
                    if (success) {
                        if (setChuteCD) setChuteCD(targetBE);
                        resetCD = true;
                        break;
                    }
                }
            } else {
                Vec3 center = getBlockPos().relative(Direction.UP).getCenter();
                AABB aabb = new AABB(
                    center.add(-0.125, -0.125, -0.125),
                    center.add(0.125, 0.125, 0.125)
                );
                if (Objects.requireNonNull(getLevel()).noCollision(aabb)) {
                    for (int i = 0; i < this.itemHandler.getSlots(); i++) {
                        ItemStack stack = this.itemHandler.getStackInSlot(i);
                        if (stack.isEmpty()) continue;
                        List<ItemEntity> itemEntities = getLevel()
                            .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(getBlockPos().relative(Direction.UP)),
                                itemEntity -> !itemEntity.getItem().isEmpty()
                            );
                        int sameItemCount = 0;
                        for (ItemEntity entity : itemEntities) {
                            if (entity.getItem().getItem() == stack.getItem()) {
                                sameItemCount += entity.getItem().getCount();
                            }
                        }
                        if (sameItemCount < stack.getMaxStackSize()) {
                            ItemStack droppedItemStack = stack.copy();
                            int droppedItemCount =
                                Math.min(stack.getCount(), stack.getMaxStackSize() - sameItemCount);
                            droppedItemStack.setCount(droppedItemCount);
                            stack.setCount(stack.getCount() - droppedItemCount);
                            if (stack.getCount() == 0) stack = ItemStack.EMPTY;
                            ItemEntity itemEntity = new ItemEntity(
                                getLevel(), center.x, center.y, center.z, droppedItemStack, 0, 0, 0
                            );
                            itemEntity.setDeltaMovement(0, 0.25, 0);
                            itemEntity.setDefaultPickUpDelay();
                            getLevel().addFreshEntity(itemEntity);
                            this.itemHandler.setStackInSlot(i, stack);
                            resetCD = true;
                            break;
                        }
                    }
                }
            }

            // 从下方吸引物品（磁性效果）
            if (!inventoryFull()) {
                BlockPos belowPos = getBlockPos().relative(Direction.DOWN);
                List<ItemEntity> itemEntities = Objects.requireNonNull(getLevel())
                    .getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(belowPos),
                        itemEntity -> !itemEntity.getItem().isEmpty()
                    );
                for (ItemEntity itemEntity : itemEntities) {
                    ItemStack itemStack = itemEntity.getItem();
                    ItemStack remaining = ItemHandlerHelper.insertItem(this.itemHandler, itemStack, true);
                    if (remaining.getCount() == itemStack.getCount()) continue;
                    // 给掉落物一个向上速度，表现磁性吸引效果
                    itemEntity.setDeltaMovement(
                        itemEntity.getDeltaMovement().x,
                        0.3,
                        itemEntity.getDeltaMovement().z
                    );
                    ItemHandlerHelper.insertItem(this.itemHandler, itemEntity.getItem(), false);
                    itemEntity.setItem(remaining);
                    resetCD = true;
                }
            }
        }
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
        if (resetCD) cooldown = AnvilCraft.CONFIG.chuteMaxCooldown;
    }

    private boolean isTargetEmpty(BlockEntity blockEntity) {
        if (blockEntity instanceof SimpleChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        if (blockEntity instanceof BaseChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        if (blockEntity instanceof SimpleMagneticChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        return false;
    }

    private void setChuteCD(BlockEntity targetBE) {
        if (targetBE instanceof BaseChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.CONFIG.chuteMaxCooldown - k);
        }
        if (targetBE instanceof SimpleChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.CONFIG.chuteMaxCooldown - k);
        }
        if (targetBE instanceof SimpleMagneticChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.CONFIG.chuteMaxCooldown - k);
        }
    }

    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < itemHandler.getSlots(); ++j) {
            ItemStack itemStack = itemHandler.getStackInSlot(j);
            if (itemStack.isEmpty()) continue;
            ++i;
        }
        return i;
    }

    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    private boolean isEnabled() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof SimpleMagneticChuteBlock)) return true;
        return state.getValue(SimpleMagneticChuteBlock.ENABLED);
    }

    private boolean inventoryFull() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack itemstack = itemHandler.getStackInSlot(i);
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) return false;
        }
        return true;
    }
}
