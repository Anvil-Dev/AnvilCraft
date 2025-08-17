package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil.getTargetItemHandlerList;


@Getter
public class SimpleChuteBlockEntity extends BlockEntity implements IItemHandlerHolder {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };
    @Setter
    private int cooldown = 0;
    private long tickedGameTime;

    public SimpleChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Cooldown", cooldown);
        tag.put("Inventory", itemHandler.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cooldown = tag.getInt("Cooldown");
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }

    /**
     * tick
     */
    @SuppressWarnings({"UnreachableCode", "DuplicatedCode"})
    public void tick() {
        if (level == null) return;
        if (cooldown > 0) cooldown--;
        tickedGameTime = level.getGameTime();
        if (cooldown == 0 && !this.itemHandler.getStackInSlot(0).isEmpty())
            cooldown = AnvilCraft.config.chuteMaxCooldown + 1;
        if (cooldown == 1) {
            BlockPos targetPos = getBlockPos().relative(getOutputDirection());
            BlockEntity targetBE = level.getBlockEntity(targetPos);
            boolean isTargetEmpty = false;
            if (targetBE != null) isTargetEmpty = isTargetEmpty(targetBE);
            // 尝试向朝向容器输出
            List<IItemHandler> targetList = getTargetItemHandlerList(
                targetPos,
                getOutputDirection().getOpposite(),
                level
            );
            if (targetList != null && !targetList.isEmpty()) {
                for (IItemHandler target : targetList) {
                    boolean success = ItemHandlerUtil.exportToTarget(getItemHandler(), 64, stack -> true, target);
                    if (success) {
                        //特判溜槽cd7gt
                        if (isTargetEmpty) setChuteCD(targetBE);
                        break;
                    }
                }
            } else {
                Vec3 center = getBlockPos().relative(getDirection()).getCenter();
                List<ItemEntity> itemEntities = Objects.requireNonNull(getLevel())
                    .getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(getBlockPos().relative(getDirection())),
                        itemEntity -> !itemEntity.getItem().isEmpty()
                    );
                AABB aabb = new AABB(
                    center.add(-0.125, -0.125, -0.125),
                    center.add(0.125, 0.125, 0.125)
                );
                if (getLevel().noCollision(aabb)) {
                    for (int i = 0; i < this.itemHandler.getSlots(); i++) {
                        ItemStack stack = this.itemHandler.getStackInSlot(i);
                        if (stack.isEmpty()) {
                            continue;
                        }
                        int sameItemCount = 0;
                        for (ItemEntity entity : itemEntities) {
                            if (entity.getItem().getItem() == stack.getItem()) {
                                sameItemCount += entity.getItem().getCount();
                            }
                        }
                        if (sameItemCount < stack.getItem().getMaxStackSize(stack)) {
                            ItemStack droppedItemStack = stack.copy();
                            int droppedItemCount =
                                Math.min(stack.getCount(), stack.getMaxStackSize() - sameItemCount);
                            droppedItemStack.setCount(droppedItemCount);
                            stack.setCount(stack.getCount() - droppedItemCount);
                            if (stack.getCount() == 0) stack = ItemStack.EMPTY;
                            ItemEntity itemEntity = new ItemEntity(
                                getLevel(), center.x, center.y, center.z, droppedItemStack, 0, 0, 0);
                            itemEntity.setDefaultPickUpDelay();
                            getLevel().addFreshEntity(itemEntity);
                            this.itemHandler.setStackInSlot(i, stack);
                            break;
                        }

                    }
                }
            }
        }
        level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
    }

    public boolean isTargetEmpty(BlockEntity blockEntity) {
        if (blockEntity instanceof SimpleChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        if (blockEntity instanceof BaseChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        return false;
    }

    private void setChuteCD(BlockEntity targetBE) {
        if (targetBE instanceof BaseChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.config.chuteMaxCooldown - k);
        }
        if (targetBE instanceof SimpleChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.config.chuteMaxCooldown - k);
        }
    }

    private Direction getDirection() {
        if (getLevel() == null) return Direction.DOWN;
        BlockState state = getLevel().getBlockState(getBlockPos());
        if (state.getBlock() instanceof SimpleChuteBlock) {
            return state.getValue(SimpleChuteBlock.FACING);
        }
        return Direction.DOWN;
    }

    /**
     * @return 红石信号强度
     */
    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < itemHandler.getSlots(); ++j) {
            ItemStack itemStack = itemHandler.getStackInSlot(j);
            if (itemStack.isEmpty()) {
                continue;
            }
            ++i;
        }
        return i;
    }

    protected Direction getOutputDirection() {
        return getDirection();
    }

    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }
}
