package dev.dubhe.anvilcraft.recipe.anvil.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.item.HasItemIngredient;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 物品原料谓词
 * <p>
 * 用于定义配方中物品原料的匹配规则，包括物品类型、数量、组件和子谓词
 * </p>
 */
public record ItemIngredientPredicate(
    Optional<HolderSet<Item>> items, // 物品集合
    int count, // 数量
    DataComponentPredicate components, // 数据组件谓词
    Map<ItemSubPredicate.Type<?>, ItemSubPredicate> subPredicates // 子谓词映射
) implements IItemStackPredicate {
    /**
     * ItemIngredientPredicate编解码器
     */
    public static final Codec<ItemIngredientPredicate> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            RegistryCodecs
                .homogeneousList(Registries.ITEM)
                .optionalFieldOf("items")
                .forGetter(ItemIngredientPredicate::items),
            Codec.INT
                .optionalFieldOf("count", 1)
                .forGetter(ItemIngredientPredicate::count),
            DataComponentPredicate.CODEC
                .optionalFieldOf("components", DataComponentPredicate.EMPTY)
                .forGetter(ItemIngredientPredicate::components),
            ItemSubPredicate.CODEC
                .optionalFieldOf("predicates", Map.of())
                .forGetter(ItemIngredientPredicate::subPredicates)
        ).apply(instance, ItemIngredientPredicate::new)
    );

    /**
     * ItemIngredientPredicate流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemIngredientPredicate> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            RegistryOps<Tag> ops = HolderLookup.Provider
                .create(Stream.of(BuiltInRegistries.ITEM.asLookup()))
                .createSerializationContext(NbtOps.INSTANCE);
            DataResult<Tag> encode = ItemIngredientPredicate.CODEC.encode(value, ops, ops.empty());
            Tag tag = encode.getOrThrow();
            buffer.writeNbt(tag);
        },
        buffer -> {
            RegistryOps<Tag> ops = HolderLookup.Provider
                .create(Stream.of(BuiltInRegistries.ITEM.asLookup()))
                .createSerializationContext(NbtOps.INSTANCE);
            return ItemIngredientPredicate.CODEC.decode(ops, buffer.readNbt()).getOrThrow().getFirst();
        }
    );

    /**
     * 创建一个物品构建器
     *
     * @param items 物品数组
     * @return 构建器实例
     */
    public static Builder of(ItemLike... items) {
        return new Builder().of(items);
    }

    /**
     * 创建一个标签构建器
     *
     * @param tag 物品标签
     * @return 构建器实例
     */
    public static Builder of(TagKey<Item> tag) {
        return new Builder().of(tag);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return this.testIgnoreCount(itemStack) && this.testCount(itemStack.getCount());
    }

    @Override
    public boolean testCount(int count) {
        return this.count <= count;
    }

    /**
     * 转换为HasItemIngredient谓词
     *
     * @param offset 偏移量
     * @param range  范围
     * @return HasItemIngredient谓词
     */
    public @NotNull HasItemIngredient toHasItemIngredient(Vec3 offset, Vec3 range) {
        return new HasItemIngredient(offset, range, this);
    }

    private static final Int2ObjectMap<ItemStack[]> INGREDIENT_CACHE = new Int2ObjectArrayMap<>();

    /**
     * 获取物品数组
     *
     * @return 物品数组
     */
    public ItemStack[] getItems() {
        int hash = this.hashCode();
        if (!INGREDIENT_CACHE.containsKey(hash)) {
            //noinspection deprecation
            INGREDIENT_CACHE.put(
                hash, this.items()
                    .map(itemSet -> itemSet.stream()
                        .map(itemHolder -> new ItemStack(itemHolder, this.count(), this.components().asPatch()))
                        .toArray(ItemStack[]::new))
                    .orElse(
                        new ItemStack[]{new ItemStack(Items.BARRIER.builtInRegistryHolder(), this.count(), this.components().asPatch())})
            );
        }
        return INGREDIENT_CACHE.get(hash);
    }

    /**
     * 构建器类，用于构建ItemIngredientPredicate实例
     */
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<HolderSet<Item>> items = Optional.empty();
        private int count;
        private DataComponentPredicate components;
        private final ImmutableMap.Builder<ItemSubPredicate.Type<?>, ItemSubPredicate> subPredicates;

        /**
         * 构造一个构建器
         */
        private Builder() {
            this.count = 1;
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
         * 设置物品堆栈
         *
         * @param stack 物品堆栈
         * @return 构建器实例
         */
        public <D> Builder of(@NotNull ItemStack stack) {
            Item item = stack.getItem();
            ItemStack defaultInstance = item.getDefaultInstance();
            this.of(item);
            for (TypedDataComponent<?> component : item.components()) {
                Object o = defaultInstance.get(component.type());
                if (o != null && o.equals(component.value())) continue;
                //noinspection unchecked
                this.hasComponents(
                    DataComponentPredicate.builder()
                        .expect((DataComponentType<D>) component.type(), (D) component.value())
                        .build()
                );
            }
            return this;
        }

        /**
         * 设置数量
         *
         * @param count 数量
         * @return 构建器实例
         */
        public Builder withCount(int count) {
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
         * 构建ItemIngredientPredicate实例
         *
         * @return ItemIngredientPredicate实例
         */
        public ItemIngredientPredicate build() {
            return new ItemIngredientPredicate(this.items, this.count, this.components, this.subPredicates.build());
        }
    }
}