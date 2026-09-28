package dev.dubhe.anvilcraft.item.property.component;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;
import java.util.function.Supplier;

/// 展示物品的数据组件；模板允许在默认组件绑定完成后再创建物品栈。
public final class StoredItem implements TooltipProvider {
    private final Supplier<ItemStack> stored;

    public StoredItem(ItemStack stored) {
        this.stored = () -> stored;
    }

    public StoredItem(ItemStackTemplate template) {
        this.stored = Suppliers.memoize(template::create);
    }

    /// 返回仅用于展示的物品栈，调用方不应修改它。
    public ItemStack stored() {
        return this.stored.get();
    }

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
        if (!(obj instanceof StoredItem other)) return false;
        return ItemStack.isSameItemSameComponents(this.stored(), other.stored());
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.stored());
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        StoredItem storedItem = components.getOrDefault(ModComponents.DISPLAY_ITEM, new StoredItem(ItemStack.EMPTY));
        ItemStack stored = storedItem.stored();
        if (!stored.isEmpty()) {
            if (stored.getCount() == 1) {
                consumer.accept(stored.getHoverName());
            } else {
                consumer.accept(Component.translatable("tooltip.anvilcraft.item_count", stored.getHoverName(), stored.getCount()));
            }
        }
    }
}
