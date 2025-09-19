package dev.dubhe.anvilcraft.api.crate.category;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCategories;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record CreativeModeTabCategory(ItemStack icon, Component name, ResourceKey<CreativeModeTab> tabKey) implements ICategory {
    public CreativeModeTabCategory(ItemLike icon, String prefix, ResourceKey<CreativeModeTab> tabKey) {
        this(icon.asItem().getDefaultInstance(), ICategory.name(prefix), tabKey);
    }

    @Override
    public boolean test(ItemStack stack) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(this.tabKey)
            .map(tab -> tab.contains(stack))
            .orElse(false);
    }

    @Override
    public Type getType() {
        return ModCategories.CREATIVE_MODE_TAB.get();
    }

    public static class Type implements ICategory.Type<CreativeModeTabCategory> {
        public static final MapCodec<CreativeModeTabCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(CreativeModeTabCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(CreativeModeTabCategory::name),
            ResourceKey.codec(Registries.CREATIVE_MODE_TAB)
                .fieldOf("tab")
                .forGetter(CreativeModeTabCategory::tabKey)
        ).apply(ins, CreativeModeTabCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, CreativeModeTabCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            CreativeModeTabCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            CreativeModeTabCategory::name,
            ResourceKey.streamCodec(Registries.CREATIVE_MODE_TAB),
            CreativeModeTabCategory::tabKey,
            CreativeModeTabCategory::new
        );

        @Override
        public MapCodec<CreativeModeTabCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CreativeModeTabCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
