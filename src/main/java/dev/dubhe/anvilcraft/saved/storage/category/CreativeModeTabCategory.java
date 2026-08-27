package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Objects;

public record CreativeModeTabCategory(ItemStack icon, Component name, ResourceKey<CreativeModeTab> key) implements ICategory {
    public CreativeModeTabCategory(ItemLike icon, ResourceLocation suffix, ResourceKey<CreativeModeTab> key) {
        this(new ItemStack(icon.asItem()), ICategory.constructName(suffix), key);
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(this.key).contains(stack.getStack());
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.CREATIVE_MODE_TAB.get();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CreativeModeTabCategory(ItemStack icon1, Component name1, ResourceKey<CreativeModeTab> key1))) return false;
        return ItemStack.isSameItemSameComponents(this.icon(), icon1)
               && Objects.equals(this.name(), name1)
               && Objects.equals(this.key(), key1);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.icon()) * 31 + Objects.hash(this.name(), this.key());
    }

    public static class Type implements ICategory.Type<CreativeModeTabCategory> {
        public static final MapCodec<CreativeModeTabCategory> CODEC = CodecUtil.mapCodec(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(CreativeModeTabCategory::icon),
            ICategory.NAME_CODEC
                .fieldOf("name")
                .forGetter(CreativeModeTabCategory::name),
            ResourceKey.codec(Registries.CREATIVE_MODE_TAB)
                .fieldOf("key")
                .forGetter(CreativeModeTabCategory::key),
            CreativeModeTabCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CreativeModeTabCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
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
