package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 有过滤的方块实体
 */
public interface IFilterBlockEntity {
    int DEFAULT_SLOT_LIMIT = 64;

    default void filterChanged() {
        if (this instanceof BlockEntity blockEntity) blockEntity.setChanged();
    }
    
    /**
     * 获取有过滤的物品存储
     *
     * @return 有过滤的物品存储
     */
    FilteredItemStackHandler getFilteredItemStackHandler();

    /**
     * 获取是否开启过滤
     *
     * @return 是否开启过滤
     */
    default boolean isFilterEnabled() {
        return this.getFilteredItemStackHandler().isFilterEnabled();
    }

    /**
     * 设置是否开启过滤
     *
     * @param enable 是否开启过滤
     */
    default void setFilterEnabled(boolean enable) {
        this.getFilteredItemStackHandler().setFilterEnabled(enable);
        this.filterChanged();
    }

    /**
     * 获取指定槽位是否禁用
     *
     * @param slot 槽位
     */
    default boolean isSlotDisabled(int slot) {
        return this.getFilteredItemStackHandler().isSlotDisabled(slot);
    }

    /**
     * 设置指定槽位是否禁用
     *
     * @param slot    槽位
     * @param disable 是否禁用
     */
    default void setSlotDisabled(int slot, boolean disable) {
        this.getFilteredItemStackHandler().setSlotDisabled(slot, disable);
        this.filterChanged();
    }

    /**
     * 获取过滤物品
     *
     * @return 过滤物品
     */
    default NonNullList<ItemStack> getFilteredItems() {
        return this.getFilteredItemStackHandler().getFilteredItems();
    }

    /**
     * 获取指定槽位的过滤
     *
     * @param slot 槽位
     */
    default ItemStack getFilter(int slot) {
        return this.getFilteredItemStackHandler().getFilter(slot);
    }

    /**
     * 设置指定槽位的过滤
     *
     * @param slot   槽位
     * @param filter 过滤
     */
    default boolean setFilter(int slot, ItemStack filter) {
        boolean changed = this.getFilteredItemStackHandler().setFilter(slot, filter);
        if (changed) this.filterChanged();
        return changed;
    }
    
    /**
     * 获取指定槽位的物品数量上限
     *
     * @param slot 槽位
     * @return 物品数量上限
     */
    default int getSlotLimit(int slot) {
        return this.getFilteredItemStackHandler().getSlotLimit(slot);
    }
    
    /**
     * 设置指定槽位的物品数量上限
     *
     * @param slot  槽位
     * @param limit 物品数量上限
     */
    default void setSlotLimit(int slot, int limit) {
        this.getFilteredItemStackHandler().setSlotLimit(slot, limit);
        this.filterChanged();
    }
}
