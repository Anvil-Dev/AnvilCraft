package dev.dubhe.anvilcraft.api.container.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCategories;
import lombok.SneakyThrows;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemClassCategory(ItemStack icon, Component name, String clazz) implements ICategory {
    private static ClassLoader loader;

    private static ClassLoader getLoader() {
        if (ItemClassCategory.loader != null) return ItemClassCategory.loader;
        return ItemClassCategory.loader = ClassLoader.getSystemClassLoader();
    }

    public ItemClassCategory(ItemLike icon, String prefix, Class<? extends Item> clazz) {
        this(icon.asItem().getDefaultInstance(), ICategory.constructName(prefix), clazz.getName());
    }

    @Override
    @SneakyThrows
    public boolean test(ItemStack stack) {
        return ItemClassCategory.getLoader().loadClass(this.clazz).isInstance(stack.getItem());
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
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(ItemClassCategory::name),
            Codec.STRING
                .fieldOf("class")
                .forGetter(ItemClassCategory::clazz)
        ).apply(ins, ItemClassCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemClassCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            ItemClassCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            ItemClassCategory::name,
            ByteBufCodecs.STRING_UTF8,
            ItemClassCategory::clazz,
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
