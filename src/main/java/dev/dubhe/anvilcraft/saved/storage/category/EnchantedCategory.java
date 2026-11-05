package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** 匹配实际包含附魔的物品，包括储存附魔与额外附魔。 */
public record EnchantedCategory() implements ICategory {
    public static final EnchantedCategory INSTANCE = new EnchantedCategory();

    @Override
    public ItemStackTemplate icon() {
        return new ItemStackTemplate(Items.ENCHANTED_BOOK);
    }

    @Override
    public Component name() {
        return Component.translatable("category.anvilcraft.enchanted");
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
            || !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
            || !stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
            || !stack.getOrDefault(ModComponents.DISABLED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.ENCHANTED.get();
    }

    public static class Type implements ICategory.Type<EnchantedCategory> {
        public static final MapCodec<EnchantedCategory> CODEC = MapCodec.unit(EnchantedCategory.INSTANCE);
        public static final StreamCodec<ByteBuf, EnchantedCategory> STREAM_CODEC = StreamCodec.unit(EnchantedCategory.INSTANCE);

        @Override
        public MapCodec<EnchantedCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EnchantedCategory> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
