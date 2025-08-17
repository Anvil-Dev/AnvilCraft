package dev.dubhe.anvilcraft.recipe.anvil.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 物品谓词
 * <p>
 * 用于定义物品匹配规则，包括物品类型、数量范围、组件和子谓词
 * </p>
 */
public record ItemPredicate(
    Optional<HolderSet<Item>> items, // 物品集合
    MinMaxBounds.Ints count, // 数量范围
    DataComponentPredicate components, // 数据组件谓词
    Map<ItemSubPredicate.Type<?>, ItemSubPredicate> subPredicates // 子谓词映射
) implements IItemStackPredicate {
    /**
     * ItemPredicate编解码器
     */
    public static final Codec<ItemPredicate> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            RegistryCodecs
                .homogeneousList(Registries.ITEM)
                .optionalFieldOf("items")
                .forGetter(ItemPredicate::items),
            MinMaxBounds.Ints.CODEC
                .optionalFieldOf("count", MinMaxBounds.Ints.ANY)
                .forGetter(ItemPredicate::count),
            DataComponentPredicate.CODEC
                .optionalFieldOf("components", DataComponentPredicate.EMPTY)
                .forGetter(ItemPredicate::components),
            ItemSubPredicate.CODEC
                .optionalFieldOf("predicates", Map.of())
                .forGetter(ItemPredicate::subPredicates)
        ).apply(instance, ItemPredicate::new));

    /**
     * ItemPredicate流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemPredicate> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            RegistryOps<Tag> ops = HolderLookup.Provider
                .create(Stream.of(BuiltInRegistries.ITEM.asLookup()))
                .createSerializationContext(NbtOps.INSTANCE);
            DataResult<Tag> encode = ItemPredicate.CODEC.encode(value, ops, ops.empty());
            Tag tag = encode.getOrThrow();
            buffer.writeNbt(tag);
        },
        buffer -> {
            RegistryOps<Tag> ops = HolderLookup.Provider
                .create(Stream.of(BuiltInRegistries.ITEM.asLookup()))
                .createSerializationContext(NbtOps.INSTANCE);
            return ItemPredicate.CODEC.decode(ops, buffer.readNbt()).getOrThrow().getFirst();
        }
    );

    @Override
    public boolean test(ItemStack itemStack) {
        return this.testIgnoreCount(itemStack) && this.testCount(itemStack.getCount());
    }

    @Override
    public boolean testCount(int count) {
        return this.count.matches(count);
    }

    /**
     * 构建器类，用于构建ItemPredicate实例
     */
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<HolderSet<Item>> items = Optional.empty();
        private MinMaxBounds.Ints count;
        private DataComponentPredicate components;
        private final ImmutableMap.Builder<ItemSubPredicate.Type<?>, ItemSubPredicate> subPredicates;

        /**
         * 构造一个构建器
         */
        private Builder() {
            this.count = MinMaxBounds.Ints.ANY;
            this.components = DataComponentPredicate.EMPTY;
            this.subPredicates = ImmutableMap.builder();
        }

        /**
         * 创建一个物品构建器
         *
         * @return 构建器实例
         */
        public static @NotNull Builder item() {
            return new Builder();
        }

        /**
         * 设置物品
         *
         * @param items 物品数组
         * @return 构建器实例
         */
        public Builder of(ItemLike... items) {
            //noinspection deprecation
            this.items = Optional.of(HolderSet.direct((item) -> item.asItem().builtInRegistryHolder(), items));
            return this;
        }

        /**
         * 设置物品标签
         *
         * @param tag 物品标签
         * @return 构建器实例
         */
        public Builder of(TagKey<Item> tag) {
            this.items = Optional.of(BuiltInRegistries.ITEM.getOrCreateTag(tag));
            return this;
        }

        /**
         * 设置数量范围
         *
         * @param count 数量范围
         * @return 构建器实例
         */
        public Builder withCount(MinMaxBounds.Ints count) {
            this.count = count;
            return this;
        }

        /**
         * 添加子谓词
         *
         * @param type      子谓词类型
         * @param predicate 子谓词
         * @param <T>       子谓词类型
         * @return 构建器实例
         */
        public <T extends ItemSubPredicate> Builder withSubPredicate(ItemSubPredicate.Type<T> type, T predicate) {
            this.subPredicates.put(type, predicate);
            return this;
        }

        /**
         * 设置数据组件谓词
         *
         * @param components 数据组件谓词
         * @return 构建器实例
         */
        public Builder hasComponents(DataComponentPredicate components) {
            this.components = components;
            return this;
        }

        /**
         * 构建ItemPredicate实例
         *
         * @return ItemPredicate实例
         */
        public ItemPredicate build() {
            return new ItemPredicate(this.items, this.count, this.components, this.subPredicates.build());
        }
    }
}