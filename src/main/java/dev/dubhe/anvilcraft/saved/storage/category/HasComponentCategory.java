package dev.dubhe.anvilcraft.saved.storage.category;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Locale;

public record HasComponentCategory(
    ItemStack icon,
    Component name,
    List<DataComponentPredicate> predicates,
    MatchType match
) implements ICategory {
    public static HasComponentCategory single(
        ItemLike icon,
        ResourceLocation suffix,
        DataComponentPredicate predicate
    ) {
        return new HasComponentCategory(
            new ItemStack(icon.asItem()),
            ICategory.constructName(suffix),
            ImmutableList.of(predicate),
            MatchType.AND
        );
    }

    public static HasComponentCategory and(
        ItemLike icon,
        ResourceLocation suffix,
        DataComponentPredicate... predicates
    ) {
        return new HasComponentCategory(
            new ItemStack(icon.asItem()),
            ICategory.constructName(suffix),
            ImmutableList.copyOf(predicates),
            MatchType.AND
        );
    }

    public static HasComponentCategory or(
        ItemLike icon,
        ResourceLocation suffix,
        DataComponentPredicate... predicates
    ) {
        return new HasComponentCategory(
            new ItemStack(icon.asItem()),
            ICategory.constructName(suffix),
            ImmutableList.copyOf(predicates),
            MatchType.OR
        );
    }

    public static HasComponentCategory nand(
        ItemLike icon,
        ResourceLocation suffix,
        DataComponentPredicate... predicates
    ) {
        return new HasComponentCategory(
            new ItemStack(icon.asItem()),
            ICategory.constructName(suffix),
            ImmutableList.copyOf(predicates),
            MatchType.NAND
        );
    }

    public static HasComponentCategory nor(
        ItemLike icon,
        ResourceLocation suffix,
        DataComponentPredicate... predicates
    ) {
        return new HasComponentCategory(
            new ItemStack(icon.asItem()),
            ICategory.constructName(suffix),
            ImmutableList.copyOf(predicates),
            MatchType.NOR
        );
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        for (DataComponentPredicate predicate : this.predicates) {
            if (predicate.test(stack) == !this.match.isAnd()) {
                return (!this.match.isAnd()) ^ this.match.isInverted();
            }
        }
        return this.match.isAnd() ^ this.match.isInverted();
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.HAS_COMPONENT.get();
    }

    public static class Type implements ICategory.Type<HasComponentCategory> {
        public static final MapCodec<HasComponentCategory> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC.fieldOf("icon").forGetter(HasComponentCategory::icon),
            ComponentSerialization.flatCodec(Integer.MAX_VALUE).fieldOf("name").forGetter(HasComponentCategory::name),
            DataComponentPredicate.CODEC.listOf().optionalFieldOf("predicates", List.of()).forGetter(HasComponentCategory::predicates),
            MatchType.CODEC.optionalFieldOf("match_type", MatchType.OR).forGetter(HasComponentCategory::match)
        ).apply(instance, HasComponentCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, HasComponentCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            HasComponentCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            HasComponentCategory::name,
            DataComponentPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            HasComponentCategory::predicates,
            MatchType.STREAM_CODEC,
            HasComponentCategory::match,
            HasComponentCategory::new
        );

        @Override
        public MapCodec<HasComponentCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasComponentCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }

    @Getter
    public enum MatchType implements StringRepresentable {
        AND(true, false),
        OR(false, false),
        NAND(true, true),
        NOR(false, true),
        ;

        public static final Codec<MatchType> CODEC = StringRepresentable.fromEnum(MatchType::values);
        public static final StreamCodec<ByteBuf, MatchType> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(MatchType.class);
        private final boolean and;
        private final boolean inverted;

        MatchType(boolean and, boolean inverted) {
            this.and = and;
            this.inverted = inverted;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}