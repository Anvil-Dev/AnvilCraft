package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;

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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public abstract class BaseChuteBlockEntity extends BaseMachineBlockEntity
        implements IFilterBlockEntity, IDiskCloneable, ItemHandlerHolder {
    private int cooldown = 0;
    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };

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

    protected abstract boolean shouldSkipDirection(Direction direction);

    protected abstract boolean validateBlockState(BlockState state);

    protected abstract DirectionProperty getFacingProperty();

    protected abstract Direction getOutputDirection();

    protected abstract Direction getInputDirection();

    protected abstract boolean isEnabled();

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

    @Override
    public FilteredItemStackHandler getFilteredItemDepository() {
        return itemHandler;
    }

    @Override
    public abstract Component getDisplayName();

    @Nullable @Override
    public abstract AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player);

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
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
    @SuppressWarnings("UnreachableCode")
    public void tick() {
        if (cooldown <= 0) {
            if (isEnabled()) {
                // 尝试从上方容器输入
                IItemHandler source = getLevel()
                        .getCapability(
                                Capabilities.ItemHandler.BLOCK,
                                getBlockPos().relative(getInputDirection()),
                                getInputDirection().getOpposite());
                if (source != null) {
                    ItemHandlerUtil.importFromTarget(getItemHandler(), 64, stack -> true, source);
                }
            } else {
                List<ItemEntity> itemEntities = getLevel()
                        .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(getBlockPos().relative(getInputDirection())),
                                itemEntity -> !itemEntity.getItem().isEmpty());
                int prevSize = itemEntities.size();
                for (ItemEntity itemEntity : itemEntities) {
                    ItemStack remaining = ItemHandlerHelper.insertItem(this.itemHandler, itemEntity.getItem(), true);
                    if (!remaining.isEmpty()) continue;
                    ItemHandlerHelper.insertItem(this.itemHandler, itemEntity.getItem(), false);
                    itemEntity.kill();
                    break;
                }
                if (prevSize > itemEntities.size()) {
                    cooldown = AnvilCraft.config.chuteMaxCooldown;
                }
            }
            if (getLevel()
                            .getCapability(
                                    Capabilities.ItemHandler.BLOCK,
                                    getBlockPos().relative(this.getOutputDirection()),
                                    getOutputDirection().getOpposite())
                    != null) {
                // 尝试向朝向容器输出
                IItemHandler target = getLevel()
                        .getCapability(
                                Capabilities.ItemHandler.BLOCK,
                                getBlockPos().relative(getOutputDirection()),
                                getOutputDirection().getOpposite());
                if (target != null) {
                    ItemHandlerUtil.exportToTarget(getItemHandler(), 64, stack -> true, target);
                    cooldown = AnvilCraft.config.chuteMaxCooldown;
                }
            } else {
                Vec3 center = getBlockPos().relative(getOutputDirection()).getCenter();
                List<ItemEntity> itemEntities = getLevel()
                        .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(getBlockPos().relative(getOutputDirection())),
                                itemEntity -> !itemEntity.getItem().isEmpty());
                AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
                if (getLevel().noCollision(aabb)) {
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
                                        getLevel(), center.x, center.y, center.z, droppedItemStack, 0, 0, 0);
                                itemEntity.setDefaultPickUpDelay();
                                getLevel().addFreshEntity(itemEntity);
                                this.itemHandler.setStackInSlot(i, stack);
                                cooldown = AnvilCraft.config.chuteMaxCooldown;
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            cooldown--;
        }
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
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

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.put("Filtering", itemHandler.serializeFiltering());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        itemHandler.deserializeFiltering(data.getCompound("Filtering"));
    }
}
