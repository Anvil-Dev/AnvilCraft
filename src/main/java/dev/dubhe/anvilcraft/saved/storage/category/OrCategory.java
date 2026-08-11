package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public record OrCategory(ItemStack icon, Component name, List<ICategory> categories) implements ICategory {
    public OrCategory(ItemLike icon, ResourceLocation suffix, ICategory... categories) {
        this(new ItemStack(icon.asItem()), ICategory.constructName(suffix), List.of(categories));
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        for (ICategory category : this.categories) {
            if (category.test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.OR.get();
    }

    public static class Type implements ICategory.Type<OrCategory> {
        public static final MapCodec<OrCategory> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(OrCategory::icon),
            ComponentSerialization.flatCodec(Integer.MAX_VALUE)
                .fieldOf("name")
                .forGetter(OrCategory::name),
            ICategory.CODEC
                .listOf()
                .fieldOf("categories")
                .forGetter(OrCategory::categories)
        ).apply(instance, OrCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, OrCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            OrCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            OrCategory::name,
            ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
            OrCategory::categories,
            OrCategory::new
        );

        @Override
        public MapCodec<OrCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OrCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}