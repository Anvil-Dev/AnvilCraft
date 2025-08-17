package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasBlock;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 抽象处理配方类
 * <p>
 * 该类是所有处理类型配方的基类，定义了配方的基本结构和通用方法
 * </p>
 */
@Getter
public abstract class AbstractProcessRecipe<T extends InWorldRecipe> extends InWorldRecipe {
    /**
     * 配方属性
     */
    protected final Property property;

    /**
     * 构造一个处理配方
     *
     * @param property 配方属性
     */
    public AbstractProcessRecipe(@NotNull Property property) {
        super(
            property.getIcon(),
            ModRecipeTriggers.ON_ANVIL_FALL_ON.get(),
            property.getConflictingPredicates(),
            property.getNonConflictingPredicates(),
            property.getOutcomes(),
            property.getPriority(),
            false
        );
        this.property = property;
    }

    @Override
    public abstract @NotNull RecipeSerializer<T> getSerializer();

    @Override
    public abstract @NotNull RecipeType<T> getType();

    /**
     * 获取输入物品列表
     *
     * @return 输入物品列表
     */
    public List<ItemIngredientPredicate> getInputItems() {
        return this.property.getInputItems();
    }

    /**
     * 获取结果物品列表
     *
     * @return 结果物品列表
     */
    public List<ChanceItemStack> getResultItems() {
        return this.property.getResultItems();
    }

    /**
     * 获取输入方块列表
     *
     * @return 输入方块列表
     */
    public List<BlockStatePredicate> getInputBlocks() {
        return this.property.getInputBlocks();
    }

    /**
     * 获取结果方块列表
     *
     * @return 结果方块列表
     */
    public List<ChanceBlockState> getResultBlocks() {
        return this.property.getResultBlocks();
    }

    /**
     * 获取炼药锅条件
     *
     * @return 炼药锅条件
     */
    public HasCauldronSimple getHasCauldron() {
        return this.property.getHasCauldron();
    }

    /**
     * 抽象序列化器类
     *
     * @param <T> 配方类型
     */
    public abstract static class AbstractSerializer<T extends AbstractProcessRecipe<T>> implements RecipeSerializer<T> {
        /**
         * 编解码器
         */
        protected final MapCodec<T> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(T::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .optionalFieldOf("results", List.of())
                .forGetter(T::getResultItems)
        ).apply(instance, this::of));

        /**
         * 流编解码器
         */
        protected final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            T::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            T::getResultItems,
            this::of
        );

        /**
         * 创建配方实例
         *
         * @param itemIngredients 物品原料列表
         * @param results         结果列表
         * @return 配方实例
         */
        protected abstract T of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results);

        @Override
        public @NotNull MapCodec<T> codec() {
            return this.codec;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
            return this.streamCodec;
        }
    }

    /**
     * 抽象构建器类
     *
     * @param <T> 配方类型
     * @param <B> 构建器类型
     */
    public abstract static class AbstractBuilder<
        T extends AbstractProcessRecipe<T>,
        B extends AbstractBuilder<T, B>
        > extends AbstractRecipeBuilder<T> {

        /**
         * 物品原料列表
         */
        protected final List<ItemIngredientPredicate> itemIngredients = new ArrayList<>();

        /**
         * 结果列表
         */
        protected final List<ChanceItemStack> results = new ArrayList<>();

        /**
         * 获取构建器实例
         *
         * @return 构建器实例
         */
        protected abstract B getThis();

        /**
         * 添加原料
         *
         * @param ingredient 原料
         * @return 构建器实例
         */
        public B requires(@NotNull ItemIngredientPredicate ingredient) {
            this.itemIngredients.add(ingredient);
            return this.getThis();
        }

        /**
         * 添加原料（标签形式）
         *
         * @param ingredient 原料标签
         * @param count      数量
         * @return 构建器实例
         */
        public B requires(@NotNull TagKey<Item> ingredient, int count) {
            this.itemIngredients.add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            return this.getThis();
        }

        /**
         * 添加原料（标签形式，默认数量为1）
         *
         * @param ingredient 原料标签
         * @return 构建器实例
         */
        public B requires(@NotNull TagKey<Item> ingredient) {
            return this.requires(ingredient, 1);
        }

        /**
         * 添加原料（物品堆栈形式）
         *
         * @param ingredient 原料物品堆栈
         * @return 构建器实例
         */
        public B requires(@NotNull ItemStack ingredient) {
            this.itemIngredients.add(ItemIngredientPredicate.Builder.item().of(ingredient).build());
            return this.getThis();
        }

        /**
         * 添加原料（物品形式）
         *
         * @param ingredient 原料物品
         * @param count      数量
         * @return 构建器实例
         */
        public B requires(@NotNull ItemLike ingredient, int count) {
            return this.requires(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
        }

        /**
         * 添加原料（物品形式，默认数量为1）
         *
         * @param ingredient 原料物品
         * @return 构建器实例
         */
        public B requires(@NotNull ItemLike ingredient) {
            return this.requires(ingredient, 1);
        }

        /**
         * 添加结果（物品堆栈形式，指定数量提供器）
         *
         * @param result 结果物品堆栈
         * @param count  数量提供器
         * @return 构建器实例
         */
        public B result(@NotNull ItemStack result, NumberProvider count) {
            this.results.add(ChanceItemStack.of(result, count));
            return this.getThis();
        }

        /**
         * 添加结果（物品堆栈形式，指定概率）
         *
         * @param result 结果物品堆栈
         * @param chance 概率
         * @return 构建器实例
         */
        public B result(@NotNull ItemStack result, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(result.getCount(), chance));
        }

        /**
         * 添加结果（物品堆栈形式，默认数量）
         *
         * @param result 结果物品堆栈
         * @return 构建器实例
         */
        public B result(@NotNull ItemStack result) {
            return this.result(result, ConstantValue.exactly(result.getCount()));
        }

        /**
         * 添加结果（物品形式，指定数量提供器）
         *
         * @param result 结果物品
         * @param count  数量提供器
         * @return 构建器实例
         */
        public B result(@NotNull ItemLike result, NumberProvider count) {
            this.results.add(ChanceItemStack.of(result, count));
            return this.getThis();
        }

        /**
         * 添加结果（物品形式，指定数量和概率）
         *
         * @param result 结果物品
         * @param count  数量
         * @param chance 概率
         * @return 构建器实例
         */
        public B result(@NotNull ItemLike result, int count, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(count, chance));
        }

        /**
         * 添加结果（物品形式，指定数量）
         *
         * @param result 结果物品
         * @param count  数量
         * @return 构建器实例
         */
        public B result(@NotNull ItemLike result, int count) {
            return this.result(result, ConstantValue.exactly(count));
        }

        /**
         * 添加结果（物品形式，指定概率，默认数量为1）
         *
         * @param result 结果物品
         * @param chance 概率
         * @return 构建器实例
         */
        public B result(@NotNull ItemLike result, float chance) {
            return this.result(result, 1, chance);
        }

        /**
         * 添加结果（物品形式，默认数量为1）
         *
         * @param result 结果物品
         * @return 构建器实例
         */
        public B result(@NotNull ItemLike result) {
            return this.result(result, ConstantValue.exactly(1.0f));
        }

        @Override
        public @NotNull Item getResult() {
            return results.isEmpty() ? Items.ANVIL : results.getFirst().getItem();
        }
    }

    /**
     * 简单抽象构建器类
     *
     * @param <T> 配方类型
     * @param <B> 构建器类型
     */
    public abstract static class SimpleAbstractBuilder<
        T extends AbstractProcessRecipe<T>,
        B extends SimpleAbstractBuilder<T, B>
        > extends AbstractBuilder<T, B> {
        /**
         * 创建配方实例
         *
         * @param itemIngredients 物品原料列表
         * @param results         结果列表
         * @return 配方实例
         */
        protected abstract T of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results);

        @Override
        public @NotNull T buildRecipe() {
            return this.of(this.itemIngredients, this.results);
        }

        @Override
        public void validate(@NotNull ResourceLocation pId) {
            if (itemIngredients.isEmpty()) {
                throw new IllegalArgumentException("Recipe ingredients must not be empty, RecipeId: " + pId);
            }
            if (results.isEmpty()) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + pId);
            }
        }
    }

    /**
     * 配方属性类
     * <p>
     * 定义配方的各种属性，包括输入输出偏移量、输入输出内容等
     * </p>
     */
    @Getter
    public static class Property {
        /**
         * 物品输入偏移量
         */
        private Vec3 itemInputOffset = Vec3.ZERO;

        /**
         * 物品输入范围
         */
        private Vec3 itemInputRange = new Vec3(1, 1, 1);

        /**
         * 输入物品列表
         */
        private List<ItemIngredientPredicate> inputItems = null;

        /**
         * 物品输出偏移量
         */
        private Vec3 itemOutputOffset = Vec3.ZERO;

        /**
         * 结果物品列表
         */
        private List<ChanceItemStack> resultItems = null;

        /**
         * 方块输入偏移量
         */
        private Vec3 blockInputOffset = Vec3.ZERO;

        /**
         * 输入方块列表
         */
        private List<BlockStatePredicate> inputBlocks = null;

        /**
         * 方块输出偏移量
         */
        private Vec3 blockOutputOffset = Vec3.ZERO;

        /**
         * 结果方块列表
         */
        private List<ChanceBlockState> resultBlocks = null;

        /**
         * 炼药锅偏移量
         */
        private Vec3 cauldronOffset = Vec3.ZERO;

        /**
         * 炼药锅条件
         */
        private HasCauldronSimple hasCauldron = null;

        /**
         * 优先级
         */
        private Integer priority = null;

        /**
         * 设置物品输入偏移量
         *
         * @param itemInputOffset 物品输入偏移量
         * @return 属性实例
         */
        public Property setItemInputOffset(Vec3 itemInputOffset) {
            this.itemInputOffset = itemInputOffset;
            return this;
        }

        /**
         * 设置物品输入范围
         *
         * @param itemInputRange 物品输入范围
         * @return 属性实例
         */
        public Property setItemInputRange(Vec3 itemInputRange) {
            this.itemInputRange = itemInputRange;
            return this;
        }

        /**
         * 设置输入物品列表
         *
         * @param inputItems 输入物品列表
         * @return 属性实例
         */
        public Property setInputItems(List<ItemIngredientPredicate> inputItems) {
            this.inputItems = inputItems;
            return this;
        }

        /**
         * 设置输入物品列表（可变参数形式）
         *
         * @param inputItems 输入物品数组
         * @return 属性实例
         */
        public Property setInputItems(ItemIngredientPredicate... inputItems) {
            return this.setInputItems(Arrays.asList(inputItems));
        }

        /**
         * 设置物品输出偏移量
         *
         * @param itemOutputOffset 物品输出偏移量
         * @return 属性实例
         */
        public Property setItemOutputOffset(Vec3 itemOutputOffset) {
            this.itemOutputOffset = itemOutputOffset;
            return this;
        }

        /**
         * 设置结果物品列表
         *
         * @param resultItems 结果物品列表
         * @return 属性实例
         */
        public Property setResultItems(List<ChanceItemStack> resultItems) {
            this.resultItems = resultItems;
            return this;
        }

        /**
         * 设置结果物品列表（可变参数形式）
         *
         * @param resultItems 结果物品数组
         * @return 属性实例
         */
        public Property setResultItems(ChanceItemStack... resultItems) {
            return this.setResultItems(Arrays.asList(resultItems));
        }

        /**
         * 设置方块输入偏移量
         *
         * @param blockInputOffset 方块输入偏移量
         * @return 属性实例
         */
        public Property setBlockInputOffset(Vec3 blockInputOffset) {
            this.blockInputOffset = blockInputOffset;
            return this;
        }

        /**
         * 设置输入方块列表
         *
         * @param inputBlocks 输入方块列表
         * @return 属性实例
         */
        public Property setInputBlocks(List<BlockStatePredicate> inputBlocks) {
            this.inputBlocks = inputBlocks;
            return this;
        }

        /**
         * 设置输入方块列表（可变参数形式）
         *
         * @param inputBlocks 输入方块数组
         * @return 属性实例
         */
        public Property setInputBlocks(BlockStatePredicate... inputBlocks) {
            return this.setInputBlocks(Arrays.asList(inputBlocks));
        }

        /**
         * 设置方块输出偏移量
         *
         * @param blockOutputOffset 方块输出偏移量
         * @return 属性实例
         */
        public Property setBlockOutputOffset(Vec3 blockOutputOffset) {
            this.blockOutputOffset = blockOutputOffset;
            return this;
        }

        /**
         * 设置结果方块列表
         *
         * @param resultBlocks 结果方块列表
         * @return 属性实例
         */
        public Property setResultBlocks(List<ChanceBlockState> resultBlocks) {
            this.resultBlocks = resultBlocks;
            return this;
        }

        /**
         * 设置结果方块列表（可变参数形式）
         *
         * @param resultBlocks 结果方块数组
         * @return 属性实例
         */
        public Property setResultBlocks(ChanceBlockState... resultBlocks) {
            return this.setResultBlocks(Arrays.asList(resultBlocks));
        }

        /**
         * 设置炼药锅偏移量
         *
         * @param cauldronOffset 炼药锅偏移量
         * @return 属性实例
         */
        public Property setCauldronOffset(Vec3 cauldronOffset) {
            this.cauldronOffset = cauldronOffset;
            return this;
        }

        /**
         * 设置炼药锅条件
         *
         * @param hasCauldron 炼药锅条件
         * @return 属性实例
         */
        public Property setHasCauldron(HasCauldronSimple hasCauldron) {
            this.hasCauldron = hasCauldron;
            return this;
        }

        /**
         * 设置优先级
         *
         * @param priority 优先级
         * @return 属性实例
         */
        public Property setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 获取图标
         *
         * @return 图标物品堆栈
         */
        private @NotNull ItemStack getIcon() {
            ItemStack icon = null;
            if (this.resultItems != null && !this.resultItems.isEmpty()) {
                icon = this.resultItems.getFirst().getStack();
            }
            if (icon == null && this.resultBlocks != null && !this.resultBlocks.isEmpty()) {
                Item item = this.resultBlocks.getFirst().getState().getBlock().asItem();
                if (item != Items.AIR) icon = item.getDefaultInstance();
            }
            if (icon == null) icon = Items.ANVIL.getDefaultInstance();
            return icon;
        }

        /**
         * 获取优先级
         *
         * @return 优先级
         */
        private int getPriority() {
            if (this.priority != null) return this.priority;
            return (this.inputItems == null ? 0 : this.inputItems.size())
                + (this.resultItems == null ? 0 : this.resultItems.size())
                + (this.inputBlocks == null ? 0 : this.inputBlocks.size() * 100)
                + (this.resultBlocks == null ? 0 : this.resultBlocks.size())
                + (this.hasCauldron != null ? 1 : 0);
        }

        /**
         * 获取非冲突谓词列表
         *
         * @return 非冲突谓词列表
         */
        private @NotNull List<IRecipePredicate<?>> getNonConflictingPredicates() {
            List<IRecipePredicate<?>> predicates = new ArrayList<>();
            if (this.hasCauldron != null) {
                predicates.add(this.hasCauldron.toHasCauldron(this.cauldronOffset));
            }
            if (this.inputBlocks != null) {
                for (int i = 0; i < this.inputBlocks.size(); i++) {
                    BlockStatePredicate block = this.inputBlocks.get(i);
                    predicates.add(new HasBlock(this.blockInputOffset.subtract(0, i, 0), block));
                }
            }
            return predicates;
        }

        /**
         * 获取冲突谓词列表
         *
         * @return 冲突谓词列表
         */
        private @NotNull List<IRecipePredicate<?>> getConflictingPredicates() {
            List<IRecipePredicate<?>> predicates = new ArrayList<>();
            if (this.inputItems != null) {
                for (ItemIngredientPredicate ingredient : this.inputItems) {
                    predicates.add(ingredient.toHasItemIngredient(this.itemInputOffset, this.itemInputRange));
                }
            }
            return predicates;
        }

        /**
         * 获取结果列表
         *
         * @return 结果列表
         */
        private @NotNull List<IRecipeOutcome<?>> getOutcomes() {
            List<IRecipeOutcome<?>> outcomes = new ArrayList<>();
            if (this.resultItems != null) {
                for (ChanceItemStack chanceItemStack : this.resultItems) {
                    outcomes.add(chanceItemStack.toSpawnItem(this.itemOutputOffset));
                }
            }
            if (this.resultBlocks != null) {
                for (int i = 0; i < this.resultBlocks.size(); i++) {
                    ChanceBlockState chanceBlockState = this.resultBlocks.get(i);
                    outcomes.add(chanceBlockState.toSetBlock(this.blockOutputOffset.subtract(0, i, 0)));
                }
            }
            return outcomes;
        }
    }
}