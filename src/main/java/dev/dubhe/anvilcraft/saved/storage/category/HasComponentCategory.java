package dev.dubhe.anvilcraft.saved.storage.category;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record HasComponentCategory(
    ItemStackTemplate icon,
    Component name,
    ImmutableMap<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates,
    MatchType match
) implements ICategory {
    public static <T extends DataComponentPredicate> HasComponentCategory single(
        ItemLike icon,
        Identifier suffix,
        DataComponentPredicate.Type<T> type,
        T predicate
    ) {
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            ImmutableMap.of(type, predicate),
            MatchType.AND
        );
    }

    public static HasComponentCategory and(
        ItemLike icon,
        Identifier suffix,
        Map<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates
    ) {
        ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> processed = ImmutableMap.builder();
        for (Map.Entry<DataComponentPredicate.Type<?>, DataComponentPredicate> entry : predicates.entrySet()) {
            if (entry.getKey() instanceof DataComponentPredicate.AnyValueType type) {
                processed.put(type, type.predicate());
            } else {
                processed.put(entry.getKey(), entry.getValue());
            }
        }
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            processed.build(),
            MatchType.AND
        );
    }

    public static HasComponentCategory and(
        ItemLike icon,
        Identifier suffix,
        DataComponentPredicate.AnyValueType... types
    ) {
        ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates = ImmutableMap.builder();
        for (DataComponentPredicate.AnyValueType type : types) {
            predicates.put(type, type.predicate());
        }
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            predicates.build(),
            MatchType.AND
        );
    }

    public static HasComponentCategory or(
        ItemLike icon,
        Identifier suffix,
        Map<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates
    ) {
        ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> processed = ImmutableMap.builder();
        for (Map.Entry<DataComponentPredicate.Type<?>, DataComponentPredicate> entry : predicates.entrySet()) {
            if (entry.getKey() instanceof DataComponentPredicate.AnyValueType type) {
                processed.put(type, type.predicate());
            } else {
                processed.put(entry.getKey(), entry.getValue());
            }
        }
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            processed.build(),
            MatchType.OR
        );
    }

    public static HasComponentCategory or(
        ItemLike icon,
        Identifier suffix,
        DataComponentPredicate.AnyValueType... types
    ) {
        ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates = ImmutableMap.builder();
        for (DataComponentPredicate.AnyValueType type : types) {
            predicates.put(type, type.predicate());
        }
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            predicates.build(),
            MatchType.OR
        );
    }

    public static HasComponentCategory nand(
        ItemLike icon,
        Identifier suffix,
        Map<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates
    ) {
        ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> processed = ImmutableMap.builder();
        for (Map.Entry<DataComponentPredicate.Type<?>, DataComponentPredicate> entry : predicates.entrySet()) {
            if (entry.getKey() instanceof DataComponentPredicate.AnyValueType type) {
                processed.put(type, type.predicate());
            } else {
                processed.put(entry.getKey(), entry.getValue());
            }
        }
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            processed.build(),
            MatchType.NAND
        );
    }

    public static HasComponentCategory nand(
        ItemLike icon,
        Identifier suffix,
        DataComponentPredicate.AnyValueType... types
    ) {
        ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates = ImmutableMap.builder();
        for (DataComponentPredicate.AnyValueType type : types) {
            predicates.put(type, type.predicate());
        }
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            predicates.build(),
            MatchType.NAND
        );
    }

    public static HasComponentCategory nor(
        ItemLike icon,
        Identifier suffix,
        Map<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates
    ) {
        ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> processed = ImmutableMap.builder();
        for (Map.Entry<DataComponentPredicate.Type<?>, DataComponentPredicate> entry : predicates.entrySet()) {
            if (entry.getKey() instanceof DataComponentPredicate.AnyValueType type) {
                processed.put(type, type.predicate());
            } else {
                processed.put(entry.getKey(), entry.getValue());
            }
        }
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            processed.build(),
            MatchType.NOR
        );
    }

    public static HasComponentCategory nor(
        ItemLike icon,
        Identifier suffix,
        DataComponentPredicate.AnyValueType... types
    ) {
        ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates = ImmutableMap.builder();
        for (DataComponentPredicate.AnyValueType type : types) {
            predicates.put(type, type.predicate());
        }
        return new HasComponentCategory(
            new ItemStackTemplate(icon.asItem()),
            ICategory.constructName(suffix),
            predicates.build(),
            MatchType.NOR
        );
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        for (DataComponentPredicate predicate : this.predicates.values()) {
            if (predicate.matches(stack) == !this.match.isAnd()) {
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
        public static final MapCodec<HasComponentCategory> CODEC = CodecUtil.mapCodec(
            ItemStackTemplate.CODEC
                .fieldOf("icon")
                .forGetter(HasComponentCategory::icon),
            ComponentSerialization.flatRestrictedCodec(Integer.MAX_VALUE)
                .fieldOf("name")
                .forGetter(HasComponentCategory::name),
            DataComponentPredicate.CODEC
                .xmap(HashMap::new, HashMap::new)
                .xmap(ImmutableMap::copyOf, HashMap::new)
                .optionalFieldOf("predicates", ImmutableMap.of())
                .forGetter(HasComponentCategory::predicates),
            MatchType.CODEC
                .optionalFieldOf("match_type", MatchType.OR)
                .forGetter(HasComponentCategory::match),
            HasComponentCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, HasComponentCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            HasComponentCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            HasComponentCategory::name,
            DataComponentPredicate.STREAM_CODEC
                .map(HashMap::new, HashMap::new)
                .map(ImmutableMap::copyOf, HashMap::new),
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
