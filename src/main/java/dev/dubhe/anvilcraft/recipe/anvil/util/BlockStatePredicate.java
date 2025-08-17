package dev.dubhe.anvilcraft.recipe.anvil.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.cache.BlockCache;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 方块状态谓词
 * <p>
 * 用于定义和匹配特定方块状态的谓词，支持方块类型、属性和NBT数据的匹配
 * </p>
 */
@Getter
public class BlockStatePredicate {
    /**
     * 属性编解码器
     */
    public static final Codec<List<PropertyMatcher>> PROPERTIES_CODEC = Codec.unboundedMap(
            Codec.STRING, ValueMatcher.CODEC
        )
        .xmap(
            map -> map.entrySet()
                .stream()
                .map(entry -> new PropertyMatcher(entry.getKey(), entry.getValue()))
                .toList(),
            list -> list.stream()
                .collect(Collectors.toMap(PropertyMatcher::name, PropertyMatcher::valueMatcher))
        );

    /**
     * BlockStatePredicate编解码器
     */
    public static final Codec<BlockStatePredicate> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.BLOCK)
                    .optionalFieldOf("blocks", HolderSet.empty())
                    .forGetter(BlockStatePredicate::getBlocks),
                PROPERTIES_CODEC
                    .listOf()
                    .optionalFieldOf("properties", List.of())
                    .forGetter(BlockStatePredicate::getProperties),
                NbtPredicate.CODEC.listOf()
                    .optionalFieldOf("nbts", Collections.emptyList())
                    .forGetter(BlockStatePredicate::getNbts)
            )
            .apply(instance, BlockStatePredicate::new)
    );

    /**
     * BlockStatePredicate流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockStatePredicate> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.holderSet(Registries.BLOCK),
        BlockStatePredicate::getBlocks,
        PropertyMatcher.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()),
        BlockStatePredicate::getProperties,
        NbtPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
        BlockStatePredicate::getNbts,
        BlockStatePredicate::new
    );

    /**
     * 方块集合
     */
    private final HolderSet<Block> blocks;

    /**
     * 属性匹配器列表
     */
    private final List<List<PropertyMatcher>> properties;

    /**
     * NBT谓词列表
     */
    private final List<NbtPredicate> nbts;

    /**
     * 构造一个方块状态谓词
     *
     * @param blocks     方块集合
     * @param properties 属性匹配器列表
     * @param nbts       NBT谓词列表
     */
    private BlockStatePredicate(HolderSet<Block> blocks, List<List<PropertyMatcher>> properties, List<NbtPredicate> nbts) {
        this.blocks = blocks;
        this.properties = properties;
        this.nbts = nbts;
    }

    /**
     * 测试方块状态是否匹配此谓词
     *
     * @param level 世界等级
     * @param cache 方块缓存
     * @param pos   方块位置
     * @return 是否匹配
     */
    public boolean test(@NotNull LevelAccessor level, @NotNull BlockCache cache, BlockPos pos) {
        BlockState state = cache.getBlockState(pos);
        if (this.blocks.size() > 0 && !state.is(this.blocks)) return false;
        if (this.properties.isEmpty()) return true;
        boolean flag = false;
        for (List<PropertyMatcher> matchers : this.properties) {
            boolean flag1 = true;
            for (PropertyMatcher matcher : matchers) {
                if (!matcher.match(state.getBlock().getStateDefinition(), state)) {
                    flag1 = false;
                    break;
                }
            }
            if (flag1) {
                flag = true;
            }
        }
        if (!flag) return false;
        if (this.nbts.isEmpty()) return true;
        if (!state.hasBlockEntity() && !this.nbts.isEmpty()) return false;
        BlockEntity entity = cache.getBlockEntity(pos);
        if (entity == null) return false;
        for (NbtPredicate nbt : this.nbts) {
            if (nbt.test(entity.saveWithFullMetadata(level.registryAccess()))) return true;
        }
        return false;
    }

    private List<BlockState> statesCache;

    /**
     * 此方法不应用于除渲染外的任何用法！
     *
     * @return 方块状态列表
     */
    public List<BlockState> constructStatesForRender() {
        if (this.statesCache != null) return this.statesCache;
        Set<BlockState> states = new HashSet<>();
        if (this.blocks.size() == 0) {
            this.statesCache = List.of();
            return this.statesCache;
        }
        if (this.properties.isEmpty()) {
            for (Holder<Block> blockHolder : this.blocks) {
                states.add(blockHolder.value().defaultBlockState());
            }
            this.statesCache = List.copyOf(states);
            return this.statesCache;
        }
        for (Holder<Block> blockHolder : this.blocks) {
            for (List<PropertyMatcher> matchers : this.properties) {
                for (PropertyMatcher matcher : matchers) {
                    states.addAll(matcher.applyToState(blockHolder.value().getStateDefinition(), blockHolder.value().defaultBlockState()));
                }
            }
        }
        this.statesCache = List.copyOf(states);
        return this.statesCache;
    }

    /**
     * 创建一个构建器
     *
     * @return 构建器实例
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * 构建器类，用于构建BlockStatePredicate实例
     */
    @SuppressWarnings({"deprecation", "UnusedReturnValue"})
    public static class Builder {
        private final List<List<PropertyMatcher>> properties = new ArrayList<>();
        private final List<NbtPredicate> nbts = new ArrayList<>();
        private HolderSet<Block> blocks = HolderSet.empty();
        private List<PropertyMatcher> and = new ArrayList<>();

        /**
         * 构造一个构建器
         */
        private Builder() {
        }

        /**
         * 设置方块
         *
         * @param blocks 方块数组
         * @return 构建器实例
         */
        public Builder of(Block... blocks) {
            this.blocks = HolderSet.direct(Block::builtInRegistryHolder, blocks);
            return this;
        }

        /**
         * 设置方块集合
         *
         * @param blocks 方块集合
         * @return 构建器实例
         */
        public Builder of(Collection<Block> blocks) {
            this.blocks = HolderSet.direct(Block::builtInRegistryHolder, blocks);
            return this;
        }

        /**
         * 设置方块标签
         *
         * @param tag 方块标签
         * @return 构建器实例
         */
        public Builder of(TagKey<Block> tag) {
            this.blocks = BuiltInRegistries.BLOCK.getOrCreateTag(tag);
            return this;
        }

        /**
         * 设置方块属性
         *
         * @param property 属性
         * @param value    属性值
         * @return 构建器实例
         */
        public Builder with(@NotNull Property<?> property, String value) {
            this.and.add(new PropertyMatcher(property.getName(), new ExactMatcher(value)));
            return this;
        }

        /**
         * 设置整数型方块属性
         *
         * @param property 属性
         * @param value    属性值
         * @return 构建器实例
         */
        public Builder with(Property<Integer> property, int value) {
            return this.with(property, Integer.toString(value));
        }

        /**
         * 设置布尔型方块属性
         *
         * @param property 属性
         * @param value    属性值
         * @return 构建器实例
         */
        public Builder with(Property<Boolean> property, boolean value) {
            return this.with(property, Boolean.toString(value));
        }

        /**
         * 设置方块属性
         *
         * @param property 属性
         * @param value    属性值
         * @param <T>      属性值类型
         * @return 构建器实例
         */
        public <T extends Comparable<T>> Builder with(Property<T> property, @NotNull T value) {
            if (value instanceof StringRepresentable stringRepresentable) {
                return this.with(property, stringRepresentable.getSerializedName());
            }
            return this.with(property, String.valueOf(value));
        }

        /**
         * 设置方块属性范围
         *
         * @param property 属性
         * @param minValue 最小值
         * @param maxValue 最大值
         * @param <T>      属性值类型
         * @return 构建器实例
         */
        public <T extends Comparable<T>> Builder with(
            @NotNull Property<T> property,
            @Nullable T minValue,
            @Nullable T maxValue
        ) {
            this.and.add(
                new PropertyMatcher(
                    property.getName(),
                    new RangedMatcher(
                        minValue == null ? Optional.empty() : Optional.of(minValue.toString()),
                        maxValue == null ? Optional.empty() : Optional.of(maxValue.toString())
                    )
                )
            );
            return this;
        }

        /**
         * 设置方块属性最小值
         *
         * @param property 属性
         * @param minValue 最小值
         * @param <T>      属性值类型
         * @return 构建器实例
         */
        public <T extends Comparable<T>> Builder withMin(
            @NotNull Property<T> property,
            T minValue
        ) {
            return this.with(property, minValue, null);
        }

        /**
         * 设置方块属性最大值
         *
         * @param property 属性
         * @param maxValue 最大值
         * @param <T>      属性值类型
         * @return 构建器实例
         */
        public <T extends Comparable<T>> Builder withMax(
            @NotNull Property<T> property,
            T maxValue
        ) {
            return this.with(property, null, maxValue);
        }

        /**
         * 添加OR条件
         *
         * @return 构建器实例
         */
        public Builder or() {
            this.properties.add(this.and);
            this.and = new ArrayList<>();
            return this;
        }

        /**
         * 设置NBT谓词
         *
         * @param tag NBT标签
         * @return 构建器实例
         */
        public Builder nbt(@NotNull CompoundTag tag) {
            this.nbts.add(new NbtPredicate(tag));
            return this;
        }

        /**
         * 构建BlockStatePredicate实例
         *
         * @return BlockStatePredicate实例
         */
        public BlockStatePredicate build() {
            if (!this.and.isEmpty()) this.or();
            return new BlockStatePredicate(this.blocks, Collections.unmodifiableList(this.properties), this.nbts);
        }
    }

    /**
     * 属性匹配器
     */
    public record PropertyMatcher(String name, ValueMatcher valueMatcher) {
        /**
         * PropertyMatcher流编解码器
         */
        public static final StreamCodec<ByteBuf, PropertyMatcher> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            PropertyMatcher::name,
            ValueMatcher.STREAM_CODEC,
            PropertyMatcher::valueMatcher,
            PropertyMatcher::new
        );

        /**
         * 测试属性是否匹配
         *
         * @param properties      状态定义
         * @param propertyToMatch 属性持有者
         * @param <S>             状态持有者类型
         * @return 是否匹配
         */
        public <S extends StateHolder<?, S>> boolean match(@NotNull StateDefinition<?, S> properties, S propertyToMatch) {
            Property<?> property = properties.getProperty(this.name);
            return property != null && this.valueMatcher.match(propertyToMatch, property);
        }

        /**
         * 应用于状态
         *
         * @param properties 状态定义
         * @param state      状态
         * @param <S>        状态持有者类型
         * @return 状态列表
         */
        public <S extends StateHolder<?, S>> List<S> applyToState(@NotNull StateDefinition<?, S> properties, S state) {
            Property<?> property = properties.getProperty(this.name);
            if (property == null) return List.of();
            return this.valueMatcher.applyToState(state, property);
        }
    }

    /**
     * 精确匹配器
     */
    public record ExactMatcher(String value) implements ValueMatcher {
        /**
         * ExactMatcher编解码器
         */
        public static final Codec<ExactMatcher> CODEC = Codec.STRING
            .xmap(ExactMatcher::new, ExactMatcher::value);

        /**
         * ExactMatcher流编解码器
         */
        public static final StreamCodec<ByteBuf, ExactMatcher> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(ExactMatcher::new, ExactMatcher::value);

        @Override
        public <T extends Comparable<T>> boolean match(@NotNull StateHolder<?, ?> value, Property<T> property) {
            T t = value.getValue(property);
            Optional<T> optional = property.getValue(this.value);
            return optional.isPresent() && t.compareTo(optional.get()) == 0;
        }

        @Override
        public <T extends Comparable<T>, S extends StateHolder<?, S>> List<S> applyToState(S state, Property<T> property) {
            if (!state.hasProperty(property)) return List.of(state);
            return property.getValue(this.value)
                .map(value -> List.of(state.setValue(property, value)))
                .orElseGet(() -> List.of(state));
        }
    }

    /**
     * 范围匹配器
     */
    public record RangedMatcher(Optional<String> minValue, Optional<String> maxValue) implements ValueMatcher {
        /**
         * RangedMatcher编解码器
         */
        public static final Codec<RangedMatcher> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.optionalFieldOf("min").forGetter(RangedMatcher::minValue),
                    Codec.STRING.optionalFieldOf("max").forGetter(RangedMatcher::maxValue)
                )
                .apply(instance, RangedMatcher::new)
        );

        /**
         * RangedMatcher流编解码器
         */
        public static final StreamCodec<ByteBuf, RangedMatcher> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
            RangedMatcher::minValue,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
            RangedMatcher::maxValue,
            RangedMatcher::new
        );

        @Override
        public <T extends Comparable<T>> boolean match(@NotNull StateHolder<?, ?> stateHolder, Property<T> property) {
            T t = stateHolder.getValue(property);
            if (this.minValue.isPresent()) {
                Optional<T> optional = property.getValue(this.minValue.get());
                if (optional.isEmpty() || t.compareTo(optional.get()) < 0) {
                    return false;
                }
            }

            if (this.maxValue.isPresent()) {
                Optional<T> optional1 = property.getValue(this.maxValue.get());
                return optional1.isPresent() && t.compareTo(optional1.get()) <= 0;
            }

            return true;
        }

        @Override
        public <T extends Comparable<T>, S extends StateHolder<?, S>> List<S> applyToState(S state, Property<T> property) {
            if (!state.hasProperty(property)) return List.of(state);
            List<S> states = new ArrayList<>();
            property.getAllValues()
                .filter(value -> this.minValue.isEmpty() || this.minValue.flatMap(property::getValue).map(minValue -> value.value().compareTo(minValue) < 0).orElse(false))
                .filter(value -> this.maxValue.isEmpty() || this.maxValue.flatMap(property::getValue).map(maxValue -> value.value().compareTo(maxValue) > 0).orElse(false))
                .forEachOrdered(value -> states.add(state.setValue(property, value.value())));
            return List.copyOf(states);
        }
    }

    /**
     * 值匹配器接口
     */
    public interface ValueMatcher {
        /**
         * ValueMatcher编解码器
         */
        Codec<ValueMatcher> CODEC = Codec.either(
                ExactMatcher.CODEC, RangedMatcher.CODEC
            )
            .xmap(Either::unwrap, matcher -> {
                if (matcher instanceof ExactMatcher exactMatcher) {
                    return Either.left(exactMatcher);
                } else if (matcher instanceof RangedMatcher rangedMatcher) {
                    return Either.right(rangedMatcher);
                } else {
                    throw new UnsupportedOperationException();
                }
            });

        /**
         * ValueMatcher流编解码器
         */
        StreamCodec<ByteBuf, ValueMatcher> STREAM_CODEC = ByteBufCodecs.either(
                ExactMatcher.STREAM_CODEC, RangedMatcher.STREAM_CODEC
            )
            .map(Either::unwrap, matcher -> {
                if (matcher instanceof ExactMatcher exactMatcher) {
                    return Either.left(exactMatcher);
                } else if (matcher instanceof RangedMatcher rangedMatcher) {
                    return Either.right(rangedMatcher);
                } else {
                    throw new UnsupportedOperationException();
                }
            });

        /**
         * 测试值是否匹配
         *
         * @param stateHolder 状态持有者
         * @param property    属性
         * @param <T>         值类型
         * @return 是否匹配
         */
        <T extends Comparable<T>> boolean match(StateHolder<?, ?> stateHolder, Property<T> property);

        /**
         * 应用于状态
         *
         * @param state    状态
         * @param property 属性
         * @param <T>      值类型
         * @param <S>      状态持有者类型
         * @return 状态列表
         */
        <T extends Comparable<T>, S extends StateHolder<?, S>> List<S> applyToState(S state, Property<T> property);
    }
}