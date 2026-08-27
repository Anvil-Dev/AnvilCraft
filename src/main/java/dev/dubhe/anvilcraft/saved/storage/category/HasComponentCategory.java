package dev.dubhe.anvilcraft.saved.storage.category;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
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
import java.util.Objects;

public record HasComponentCategory(
    ItemStack icon,
    Component name,
    List<DataComponentPredicate> predicates,
    List<DataComponentType<?>> presence,
    MatchType match
) implements ICategory {
    public HasComponentCategory {
        predicates = ImmutableList.copyOf(predicates);
        presence = ImmutableList.copyOf(presence);
    }

    public static HasComponentCategory single(
        ItemLike icon,
        ResourceLocation suffix,
        DataComponentPredicate predicate
    ) {
        return new HasComponentCategory(
            new ItemStack(icon.asItem()),
            ICategory.constructName(suffix),
            ImmutableList.of(predicate),
            List.of(),
            MatchType.AND
        );
    }

    public static HasComponentCategory single(
        ItemLike icon,
        ResourceLocation suffix,
        DataComponentType<?> type
    ) {
        return new HasComponentCategory(
            new ItemStack(icon.asItem()),
            ICategory.constructName(suffix),
            List.of(),
            ImmutableList.of(type),
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
            List.of(),
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
            List.of(),
            MatchType.OR
        );
    }

    public static HasComponentCategory or(
        ItemLike icon,
        ResourceLocation suffix,
        DataComponentType<?>... types
    ) {
        return new HasComponentCategory(
            new ItemStack(icon.asItem()),
            ICategory.constructName(suffix),
            List.of(),
            ImmutableList.copyOf(types),
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
            List.of(),
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
            List.of(),
            MatchType.NOR
        );
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        boolean matches;
        if (this.match.isAnd()) {
            matches = this.allMatch(stack);
        } else {
            matches = this.anyMatch(stack);
        }
        return matches ^ this.match.isInverted();
    }

    private boolean allMatch(UnlimitedItemStack stack) {
        for (DataComponentPredicate predicate : this.predicates) {
            if (!predicate.test(stack)) {
                return false;
            }
        }
        for (DataComponentType<?> type : this.presence) {
            if (stack.get(type) == null) {
                return false;
            }
        }
        return true;
    }

    private boolean anyMatch(UnlimitedItemStack stack) {
        for (DataComponentPredicate predicate : this.predicates) {
            if (predicate.test(stack)) {
                return true;
            }
        }
        for (DataComponentType<?> type : this.presence) {
            if (stack.get(type) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.HAS_COMPONENT.get();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HasComponentCategory(
            ItemStack icon1,
            Component name1,
            List<DataComponentPredicate> predicates1,
            List<DataComponentType<?>> presence1,
            MatchType match1
            ))) { // fuck checkstyle
            return false;
        }
        return ItemStack.isSameItemSameComponents(this.icon(), icon1)
               && Objects.equals(this.name(), name1)
               && this.match() == match1
               && Objects.equals(this.presence(), presence1)
               && Objects.equals(this.predicates(), predicates1);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.icon()) * 31
               + Objects.hash(this.name(), this.predicates(), this.presence(), this.match());
    }

    public static class Type implements ICategory.Type<HasComponentCategory> {
        public static final MapCodec<HasComponentCategory> CODEC = CodecUtil.mapCodec(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(HasComponentCategory::icon),
            ICategory.NAME_CODEC
                .fieldOf("name")
                .forGetter(HasComponentCategory::name),
            DataComponentPredicate.CODEC
                .listOf()
                .optionalFieldOf("predicates", List.of())
                .forGetter(HasComponentCategory::predicates),
            DataComponentType.CODEC
                .listOf()
                .optionalFieldOf("presence", List.of())
                .forGetter(HasComponentCategory::presence),
            MatchType.CODEC
                .optionalFieldOf("match_type", MatchType.OR)
                .forGetter(HasComponentCategory::match),
            HasComponentCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, HasComponentCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            HasComponentCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            HasComponentCategory::name,
            DataComponentPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            HasComponentCategory::predicates,
            DataComponentType.STREAM_CODEC.apply(ByteBufCodecs.list()),
            HasComponentCategory::presence,
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
