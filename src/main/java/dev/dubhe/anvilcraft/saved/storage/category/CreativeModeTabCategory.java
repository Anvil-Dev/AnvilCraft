package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;

public record CreativeModeTabCategory(ItemStackTemplate icon, Component name, ResourceKey<CreativeModeTab> key) implements ICategory {
    public CreativeModeTabCategory(ItemLike icon, Identifier suffix, ResourceKey<CreativeModeTab> key) {
        this(new ItemStackTemplate(icon.asItem()), ICategory.constructName(suffix), key);
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(this.key).value().contains(stack.getStack());
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.CREATIVE_MODE_TAB.get();
    }

    public static class Type implements ICategory.Type<CreativeModeTabCategory> {
        public static final MapCodec<CreativeModeTabCategory> CODEC = CodecUtil.mapCodec(
            ItemStackTemplate.CODEC
                .fieldOf("icon")
                .forGetter(CreativeModeTabCategory::icon),
            ComponentSerialization.flatRestrictedCodec(Integer.MAX_VALUE)
                .fieldOf("name")
                .forGetter(CreativeModeTabCategory::name),
            ResourceKey.codec(Registries.CREATIVE_MODE_TAB)
                .fieldOf("key")
                .forGetter(CreativeModeTabCategory::key),
            CreativeModeTabCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CreativeModeTabCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            CreativeModeTabCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            CreativeModeTabCategory::name,
            ResourceKey.streamCodec(Registries.CREATIVE_MODE_TAB),
            CreativeModeTabCategory::key,
            CreativeModeTabCategory::new
        );

        @Override
        public MapCodec<CreativeModeTabCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CreativeModeTabCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
