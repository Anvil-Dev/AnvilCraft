package dev.dubhe.anvilcraft.api.item;

import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 继承该接口的物品可用于在GUI内部，于自身上方额外渲染另一个物品。<br/>
 * 注意：额外渲染嵌套层数过深时不会继续渲染。
 */

@MethodsReturnNonnullByDefault
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
    int xOffset(ItemStack stack);

    /**
     * 渲染的额外物品相对于<b>上侧</b>的垂直偏移量。
     * 返回0相当于对齐自身最上侧，返回16相当于对齐自身最下侧。
     *
     * @param stack 需判断偏移量的物品
     * @return 垂直偏移量
     */
    int yOffset(ItemStack stack);


    /**
     * 渲染的额外物品相对于自身的缩放大小。
     * 返回1.0相当于与自身相同，返回0.5相当于渲染为自身大小的一半。
     *
     * @param stack 需判断缩放大小的物品
     * @return 缩放大小
     */
    float scale(ItemStack stack);

    /**
     * 由于 {@link DataComponentType} 要求存储的数值必须继承 {@code hashCode}与 {@code equals}方法，
     * 我们需要一个记录类存储用于展示的物品。
     *
     * @param stored 被储存的用于展示的物品
     * @apiNote {@code stored}中的物品仅用于展示，不要尝试修改它。
     */
    record StoredItem(ItemStack stored) {

        public static Codec<StoredItem> CODEC = ItemStack.CODEC.xmap(StoredItem::new, StoredItem::stored);

        public static StreamCodec<RegistryFriendlyByteBuf, StoredItem> STREAM_CODEC = ItemStack.STREAM_CODEC
            .map(StoredItem::new, StoredItem::stored);

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StoredItem(ItemStack stack))) return false;
            return ItemStack.isSameItemSameComponents(this.stored, stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stored);
        }
    }
}
