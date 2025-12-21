package dev.dubhe.anvilcraft.api.sc.category;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.sc.ModCategories;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Function;

public record HasDataComponentCategory(ItemStack icon, Component name, List<? extends DataComponentType<?>> types) implements ICategory {
    public HasDataComponentCategory(ItemLike icon, String suffix, DataComponentType<?>... types) {
        this(icon.asItem().getDefaultInstance(), ICategory.constructName(suffix), List.of(types));
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        for (DataComponentType<?> type : this.types) {
            if (!stack.getStack().has(type)) return false;
        }
        return true;
    }

    @Override
    public Type getType() {
        return ModCategories.HAS_DATA_COMPONENT.get();
    }

    public static class Type implements ICategory.Type<HasDataComponentCategory> {
        public static final MapCodec<HasDataComponentCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(HasDataComponentCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(HasDataComponentCategory::name),
            Codec.either(DataComponentType.CODEC, DataComponentType.CODEC.listOf())
                .xmap(
                    either -> either.map(List::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(List.copyOf(list))
                )
                .fieldOf("components")
                .forGetter(HasDataComponentCategory::types)
        ).apply(ins, HasDataComponentCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, HasDataComponentCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            HasDataComponentCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            HasDataComponentCategory::name,
            DataComponentType.STREAM_CODEC.apply(ByteBufCodecs.list()),
            o -> Lists.transform(o.types(), Util::cast),
            HasDataComponentCategory::new
        );

        @Override
        public MapCodec<HasDataComponentCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasDataComponentCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
