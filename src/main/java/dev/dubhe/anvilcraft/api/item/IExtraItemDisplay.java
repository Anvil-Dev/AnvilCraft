package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 继承该接口的物品可用于在GUI内部，于自身上方额外渲染另一个物品。<br/>
 * 注意：额外渲染嵌套层数过深时不会继续渲染。
 */

public interface IExtraItemDisplay {
    /**
     * 判断一个物品的额外渲染物品。若无需额外渲染，返回{@link ItemStack#EMPTY}。
     *
     * @param stack 需判断额外渲染物的物品
     * @return 需要额外渲染的物品。
     */
    ItemStack getDisplayedItem(ItemStack stack);

    /**
     * 渲染的额外物品相对于<b>左侧</b>的水平偏移量。
     * 返回0相当于对其自齐最左侧，返回16相当于对齐自身最右侧。
     *
     * @param stack 需判断偏移量的物品
     * @return 水平偏移量
     */
    int offsetX(ItemStack stack);

    /**
     * 渲染的额外物品相对于<b>上侧</b>的垂直偏移量。
     * 返回0相当于对齐自身最上侧，返回16相当于对齐自身最下侧。
     *
     * @param stack 需判断偏移量的物品
     * @return 垂直偏移量
     */
    int offsetY(ItemStack stack);

    /**
     * 渲染的额外物品相对于自身的缩放大小。
     * 返回1.0相当于与自身相同，返回0.5相当于渲染为自身大小的一半。
     *
     * @param stack 需判断缩放大小的物品
     * @return 缩放大小
     */
    float scale(ItemStack stack);
}
