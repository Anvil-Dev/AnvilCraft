package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

import static dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil.getSourceItemHandler;
import static dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil.getTargetItemHandlerList;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class BaseChuteBlockEntity
    extends BaseMachineBlockEntity
    implements IFilterBlockEntity, IDiskCloneable, IItemHandlerHolder {

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(9) {
        @Override
        public void onContentsChanged(int slot) {
            assert level != null;
            if (level.isClientSide) return;
            setChanged();
        }
    };
    @Setter
    private int cooldown = 0;
    private long tickedGameTime;

    protected BaseChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (validateBlockState(state)) return state.getValue(getFacingProperty());
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {
        if (shouldSkipDirection(direction)) return;
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!validateBlockState(state)) return;
        level.setBlockAndUpdate(pos, state.setValue(getFacingProperty(), direction));
    }

    protected abstract boolean shouldSkipDirection(Direction direction);

    protected abstract boolean validateBlockState(BlockState state);

    protected abstract DirectionProperty getFacingProperty();

    protected abstract Direction getOutputDirection();

    protected abstract Direction getInputDirection();

    protected abstract boolean isEnabled();

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return itemHandler;
    }

    @Override
    public abstract Component getDisplayName();

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int i, Inventory inventory, Player player);

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Cooldown", cooldown);
        tag.put("Inventory", itemHandler.serializeNBT(provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cooldown = tag.getInt("Cooldown");
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }

    /**
     * 溜槽 tick
     */
    public void tick() {
        if (level == null) return;
        if (cooldown > 0) cooldown--;
        tickedGameTime = level.getGameTime();
        boolean resetCD = false;
        if (cooldown <= 0) {
            if (isEnabled()) {
                BlockPos targetPos = getBlockPos().relative(getOutputDirection());
                // 尝试向朝向容器输出
                List<IItemHandler> targetList = getTargetItemHandlerList(
                    targetPos,
                    getOutputDirection().getOpposite(),
                    level
                );
                if (targetList != null && !targetList.isEmpty()) {
                    for (IItemHandler target : targetList) {
                        BlockEntity targetBE = level.getBlockEntity(targetPos);
                        boolean setChuteCD = targetBE != null && isTargetEmpty(targetBE);
                        boolean success = ItemHandlerUtil.exportToTarget(getItemHandler(), 64, stack -> true, target);
                        if (success) {
                            //特判溜槽cd7gt
                            if (setChuteCD) setChuteCD(targetBE);
                            resetCD = true;
                            break;
                        }
                    }
                } else {
                    Vec3 center = getBlockPos().relative(getOutputDirection()).getCenter();
                    AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
                    if (Objects.requireNonNull(getLevel()).noCollision(aabb)) {
                        List<ItemEntity> itemEntities = getLevel()
                            .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(getBlockPos().relative(getOutputDirection())),
                                itemEntity -> !itemEntity.getItem().isEmpty());
                        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
                            ItemStack stack = this.itemHandler.getStackInSlot(i);
                            if (!stack.isEmpty()) {
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
                                        getLevel(),
                                        center.x,
                                        center.y,
                                        center.z,
                                        droppedItemStack,
                                        0,
                                        0,
                                        0
                                    );
                                    applySpeed(itemEntity, getOutputDirection());
                                    itemEntity.setDefaultPickUpDelay();
                                    getLevel().addFreshEntity(itemEntity);
                                    this.itemHandler.setStackInSlot(i, stack);
                                    resetCD = true;
                                    break;
                                }
                            }
                        }
                    }

                }
                // 尝试从上方容器输入
                if (!this.inventoryFull()) {
                    IItemHandler source = getSourceItemHandler(
                        getBlockPos().relative(getInputDirection()),
                        getInputDirection().getOpposite(),
                        level
                    );
                    if (source != null) {
                        resetCD |= ItemHandlerUtil.importFromTarget(getItemHandler(), 64, stack -> true, source);
                    } else {
                        List<ItemEntity> itemEntities = Objects.requireNonNull(getLevel())
                            .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(getBlockPos().relative(getInputDirection())),
                                itemEntity -> !itemEntity.getItem().isEmpty());
                        for (ItemEntity itemEntity : itemEntities) {
                            ItemStack itemStack = itemEntity.getItem();
                            ItemStack remaining =
                                ItemHandlerHelper.insertItem(this.itemHandler, itemStack, true);
                            if (remaining.getCount() == itemStack.getCount()) continue;
                            ItemHandlerHelper.insertItem(this.itemHandler, itemEntity.getItem(), false);
                            itemEntity.setItem(remaining);
                            resetCD = true;
                        }
                    }
                }

            }

        }
        level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        if (resetCD) cooldown = AnvilCraft.config.chuteMaxCooldown;
    }

    private boolean isTargetEmpty(BlockEntity blockEntity) {
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

    /**
     * 获取红石信号强度
     *
     * @return 红石信号强度
     */
    public int getRedstoneSignal() {
        int strength = 0;
        for (int index = 0; index < itemHandler.getSlots(); index++) {
            ItemStack itemStack = itemHandler.getStackInSlot(index);
            // 槽位为未设置过滤的已禁用槽位
            if (itemHandler.isSlotDisabled(index) && !itemHandler.isFilterEnabled()) {
                strength++;
                continue;
            }
            // 槽位上没有物品
            if (itemStack.isEmpty()) {
                continue;
            }
            strength++;
        }
        return strength;
    }

    protected void applySpeed(ItemEntity itemEntity, Direction direction) {

    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.put("Filtering", itemHandler.serializeFiltering());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        itemHandler.deserializeFiltering(data.getCompound("Filtering"));
    }

    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    private boolean inventoryFull() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack itemstack = itemHandler.getStackInSlot(i);
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize())
                return false;
        }
        return true;
    }
}
