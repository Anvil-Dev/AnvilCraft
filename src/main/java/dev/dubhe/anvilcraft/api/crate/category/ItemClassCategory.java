package dev.dubhe.anvilcraft.api.crate.category;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCategories;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemClassCategory(ItemStack icon, Component name, Class<? extends Item> clazz) implements ICategory {
    public ItemClassCategory(ItemLike icon, String prefix, Class<? extends Item> clazz) {
        this(icon.asItem().getDefaultInstance(), ICategory.constructName(prefix), clazz);
    }

    private ItemClassCategory(ItemStack icon, Class<?> itemClass, Component name) {
        this(icon, name, Util.cast(itemClass));
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.clazz.isInstance(stack.getItem());
    }

    @Override
    public Type getType() {
        return ModCategories.ITEM_CLASS.get();
    }

    public static class Type implements ICategory.Type<ItemClassCategory> {
        public static final MapCodec<ItemClassCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(ItemClassCategory::icon),
            CodecUtil.CLASS_CODEC
                .forGetter(ItemClassCategory::clazz),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(ItemClassCategory::name)
        ).apply(ins, ItemClassCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemClassCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            ItemClassCategory::icon,
            CodecUtil.CLASS_STREAM_CODEC,
            ItemClassCategory::clazz,
            ComponentSerialization.STREAM_CODEC,
            ItemClassCategory::name,
            ItemClassCategory::new
        );

        @Override
        public MapCodec<ItemClassCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemClassCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
