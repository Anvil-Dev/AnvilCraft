package dev.dubhe.anvilcraft.api.container.category;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCategories;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record DataComponentCategory(ItemStack icon, Component name, DataComponentPredicate predicate) implements ICategory {
    public DataComponentCategory(ItemLike icon, String prefix, DataComponentPredicate predicate) {
        this(icon.asItem().getDefaultInstance(), ICategory.constructName(prefix), predicate);
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.predicate.test(stack);
    }

    @Override
    public Type getType() {
        return ModCategories.DATA_COMPONENT.get();
    }

    public static class Type implements ICategory.Type<DataComponentCategory> {
        public static final MapCodec<DataComponentCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(DataComponentCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(DataComponentCategory::name),
            DataComponentPredicate.CODEC
                .fieldOf("predicate")
                .forGetter(DataComponentCategory::predicate)
        ).apply(ins, DataComponentCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            DataComponentCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            DataComponentCategory::name,
            DataComponentPredicate.STREAM_CODEC,
            DataComponentCategory::predicate,
            DataComponentCategory::new
        );

        @Override
        public MapCodec<DataComponentCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DataComponentCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
