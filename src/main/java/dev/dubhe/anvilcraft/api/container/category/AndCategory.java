package dev.dubhe.anvilcraft.api.container.category;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.shulkercontainer.ModCategories;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public record AndCategory(ItemStack icon, Component name, List<ICategory> categories) implements ICategory {
    public AndCategory(ItemLike icon, String prefix, ICategory... categories) {
        this(icon.asItem().getDefaultInstance(), ICategory.constructName(prefix), List.of(categories));
    }

    @Override
    public boolean test(ItemStack stack) {
        for (var category : this.categories) {
            if (!category.test(stack)) return false;
        }
        return true;
    }

    @Override
    public Type getType() {
        return ModCategories.AND.get();
    }

    public static class Type implements ICategory.Type<AndCategory> {
        public static final MapCodec<AndCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(AndCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(AndCategory::name),
            ICategory.CODEC.listOf(2, Integer.MAX_VALUE)
                .optionalFieldOf("categories", List.of())
                .forGetter(AndCategory::categories)
        ).apply(ins, AndCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AndCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            AndCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            AndCategory::name,
            ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
            AndCategory::categories,
            AndCategory::new
        );

        @Override
        public MapCodec<AndCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AndCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
