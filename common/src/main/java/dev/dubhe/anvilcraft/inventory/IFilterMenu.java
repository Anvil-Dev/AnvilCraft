package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * 有过滤的菜单
 */
public interface IFilterMenu {
    /**
     * 获取有过滤的方块实体
     *
     * @return 有过滤的方块实体
     */
    IFilterBlockEntity getFilterBlockEntity();

    /**
     * 获取是否开启过滤
     *
     * @return 是否开启过滤
     */
    default boolean isFilterEnabled() {
        return this.getFilterBlockEntity().isFilterEnabled();
    }

    /**
     * 设置是否开启过滤
     *
     * @param enable 是否开启过滤
     */
    default void setFilterEnabled(boolean enable) {
        this.getFilterBlockEntity().setFilterEnabled(enable);
    }

    /**
     * 获取指定槽位是否禁用
     *
     * @param slot 槽位
     */
    default boolean isSlotDisabled(int slot) {
        return this.getFilterBlockEntity().isSlotDisabled(slot);
    }

    /**
     * 获取指定槽位的过滤
     *
     * @param slot 槽位
     */
    default ItemStack getFilter(int slot) {
        return this.getFilterBlockEntity().getFilter(slot);
    }

    /**
     * 设置指定槽位是否禁用
     *
     * @param slot    槽位
     * @param disable 是否禁用
     */
    default void setSlotDisabled(int slot, boolean disable) {
        this.getFilterBlockEntity().setSlotDisabled(slot, disable);
    }

    /**
     * 获取过滤物品
     *
     * @return 过滤物品
     */
    default NonNullList<ItemStack> getFilteredItems() {
        return this.getFilterBlockEntity().getFilteredItems();
    }

    /**
     * 设置指定槽位的过滤
     *
     * @param slot   槽位
     * @param filter 过滤
     */
    default void setFilter(int slot, ItemStack filter) {
        this.getFilterBlockEntity().setFilter(slot, filter);
    }

    /**
     * 刷新
     */
    default void flush() {
    }
}
