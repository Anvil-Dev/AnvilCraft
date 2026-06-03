package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

/// 由于 {@link DataComponentType} 要求存储的数值必须继承 {@code hashCode}与 {@code equals}方法，
/// 我们需要一个记录类存储用于展示的物品。
///
/// @param stored 被储存的用于展示的物品
/// @apiNote {@code stored}中的物品仅用于展示，不要尝试修改它。
public record StoredItem(ItemStack stored) implements TooltipProvider {
    public static Codec<StoredItem> CODEC = ItemStack.CODEC.xmap(
        StoredItem::new,
        StoredItem::stored
    );

    public static StreamCodec<RegistryFriendlyByteBuf, StoredItem> STREAM_CODEC = ItemStack.STREAM_CODEC.map(
        StoredItem::new,
        StoredItem::stored
    );

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StoredItem(ItemStack stack))) return false;
        return ItemStack.isSameItemSameComponents(this.stored, stack);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.stored);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        StoredItem storedItem = components.getOrDefault(ModComponents.DISPLAY_ITEM, new StoredItem(ItemStack.EMPTY));
        ItemStack stored = storedItem.stored();
        if (!stored.isEmpty()) {
            if (stored.getCount() == 1) {
                consumer.accept(stored.getHoverName());
            } else {
                consumer.accept(stored.getHoverName().copy().append(" x").append(String.valueOf(stored.getCount())));
            }
        }
    }
}
