package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.v2.recipe.component.ChanceBlockState;
import dev.anvilcraft.lib.v2.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.v2.recipe.component.ItemIngredientPredicate;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlock;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockIngredient;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ProduceHeat;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 抽象处理配方类
 *
 * <p>该类是所有处理类型配方的基类，定义了配方的基本结构和通用方法</p>
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
    public AbstractProcessRecipe(Property property) {
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
    public abstract RecipeSerializer<T> getSerializer();

    @Override
    public abstract RecipeType<T> getType();

    /**
     * 获取输入物品列表
     *
     * @return 输入物品列表
     */
    public List<ItemIngredientPredicate> getInputItems() {
        return Objects.requireNonNullElseGet(this.property.getInputItems(), List::of);
    }

    /**
     * 获取结果物品列表
     *
     * @return 结果物品列表
     */
    public List<ChanceItemStack> getResultItems() {
        return Objects.requireNonNullElseGet(this.property.getResultItems(), List::of);
    }

    /**
     * 获取输入方块列表
     *
     * @return 输入方块列表
     */
    public List<BlockStatePredicate> getInputBlocks() {
        return Objects.requireNonNullElseGet(this.property.getInputBlocks(), List::of);
    }

    /**
     * 获取首个输入方块
     *
     * @return 首个输入方块
     */
    public BlockStatePredicate getFirstInputBlock() {
        return Objects.requireNonNullElseGet(
            this.getInputBlocks().getFirst(),
            () -> BlockStatePredicate.builder().of(Blocks.AIR).build()
        );
    }

    /**
     * 获取结果方块列表
     *
     * @return 结果方块列表
     */
    public List<ChanceBlockState> getResultBlocks() {
        return Objects.requireNonNullElseGet(this.property.getResultBlocks(), List::of);
    }

    /**
     * 获取首个结果方块
     *
     * @return 首个结果方块
     */
    public ChanceBlockState getFirstResultBlock() {
        return Objects.requireNonNullElseGet(
            this.getResultBlocks().getFirst(),
            () -> new ChanceBlockState(Blocks.AIR.defaultBlockState(), 1.0f)
        );
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
     * 获取铁砧条件
     *
     * @return 炼药锅条件
     */
    public HasAnvil getHasAnvil() {
        return this.property.getHasAnvil();
    }

    /**
     * 获取产热信息
     *
     * @return 产热信息
     */
    public ProduceHeat getProduceHeat() {
        return this.property.getProduceHeat();
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
        public MapCodec<T> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
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
        public B requires(ItemIngredientPredicate ingredient) {
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
        public B requires(TagKey<Item> ingredient, int count) {
            this.itemIngredients.add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            return this.getThis();
        }

        /**
         * 添加原料（标签形式，默认数量为1）
         *
         * @param ingredient 原料标签
         * @return 构建器实例
         */
        public B requires(TagKey<Item> ingredient) {
            return this.requires(ingredient, 1);
        }

        /**
         * 添加原料（物品堆栈形式）
         *
         * @param ingredient 原料物品堆栈
         * @return 构建器实例
         */
        public B requires(ItemStack ingredient) {
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
        public B requires(ItemLike ingredient, int count) {
            return this.requires(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
        }

        /**
         * 添加原料（物品形式，默认数量为1）
         *
         * @param ingredient 原料物品
         * @return 构建器实例
         */
        public B requires(ItemLike ingredient) {
            return this.requires(ingredient, 1);
        }

        /**
         * 添加结果（物品堆栈形式，指定数量提供器）
         *
         * @param result 结果物品堆栈
         * @param count  数量提供器
         * @return 构建器实例
         */
        public B result(ItemStack result, NumberProvider count) {
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
        public B result(ItemStack result, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(result.getCount(), chance));
        }

        /**
         * 添加结果（物品堆栈形式，默认数量）
         *
         * @param result 结果物品堆栈
         * @return 构建器实例
         */
        public B result(ItemStack result) {
            return this.result(result, ConstantValue.exactly(result.getCount()));
        }

        /**
         * 添加结果（物品形式，指定数量提供器）
         *
         * @param result 结果物品
         * @param count  数量提供器
         * @return 构建器实例
         */
        public B result(ItemLike result, NumberProvider count) {
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
        public B result(ItemLike result, int count, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(count, chance));
        }

        /**
         * 添加结果（物品形式，指定数量）
         *
         * @param result 结果物品
         * @param count  数量
         * @return 构建器实例
         */
        public B result(ItemLike result, int count) {
            return this.result(result, ConstantValue.exactly(count));
        }

        /**
         * 添加结果（物品形式，指定概率，默认数量为1）
         *
         * @param result 结果物品
         * @param chance 概率
         * @return 构建器实例
         */
        public B result(ItemLike result, float chance) {
            return this.result(result, 1, chance);
        }

        /**
         * 添加结果（物品形式，默认数量为1）
         *
         * @param result 结果物品
         * @return 构建器实例
         */
        public B result(ItemLike result) {
            return this.result(result, ConstantValue.exactly(1.0f));
        }

        @Override
        public Item getResult() {
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
        public T buildRecipe() {
            return this.of(this.itemIngredients, this.results);
        }

        @Override
        public void validate(ResourceLocation id) {
            if (itemIngredients.isEmpty()) {
                throw new IllegalArgumentException("Recipe ingredients must not be empty, RecipeId: " + id);
            }
            if (results.isEmpty()) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + id);
            }
        }
    }

    /**
     * 配方属性类
     *
     * <p>定义配方的各种属性，包括输入输出偏移量、输入输出内容等</p>
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
        private Vec3i blockInputOffset = Vec3i.ZERO;

        private boolean consumeInputBlocks = false;

        /**
         * 输入方块列表
         */
        private List<BlockStatePredicate> inputBlocks = null;

        /**
         * 方块输出偏移量
         */
        private Vec3i blockOutputOffset = Vec3i.ZERO;

        /**
         * 结果方块列表
         */
        private List<ChanceBlockState> resultBlocks = null;

        /**
         * 炼药锅偏移量
         */
        private Vec3i cauldronOffset = Vec3i.ZERO;

        /**
         * 炼药锅条件
         */
        private HasCauldronSimple hasCauldron = null;

        /**
         * 铁砧条件
         */
        private HasAnvil hasAnvil = HasAnvil.DEFAULT;

        /**
         * 产热信息
         */
        private ProduceHeat produceHeat = null;

        /**
         * 优先级
         */
        private Integer priority = null;

        /**
         * 额外结果列表
         */
        private final List<IRecipeOutcome<?>> extraOutcomes = new ArrayList<>();

        /**
         * 添加额外结果
         *
         * @param outcome 额外结果
         * @return 属性实例
         */
        public Property addOutcome(IRecipeOutcome<?> outcome) {
            this.extraOutcomes.add(outcome);
            return this;
        }

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
        public Property setBlockInputOffset(Vec3i blockInputOffset) {
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
         * 设置是否消耗输入方块
         *
         * @param consumeInputBlocks 是否消耗输入方块
         * @return 属性实例
         */
        public Property setConsumeInputBlocks(boolean consumeInputBlocks) {
            this.consumeInputBlocks = consumeInputBlocks;
            return this;
        }

        /**
         * 设置方块输出偏移量
         *
         * @param blockOutputOffset 方块输出偏移量
         * @return 属性实例
         */
        public Property setBlockOutputOffset(Vec3i blockOutputOffset) {
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
        public Property setCauldronOffset(Vec3i cauldronOffset) {
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
         * 设置铁砧条件
         *
         * @param hasAnvil 铁砧条件
         * @return 属性实例
         */
        public Property setHasAnvil(HasAnvil hasAnvil) {
            this.hasAnvil = hasAnvil;
            return this;
        }

        /**
         * 设置产热信息
         *
         * @param produceHeat 产热信息
         * @return 属性实例
         */
        public Property setProduceHeat(ProduceHeat produceHeat) {
            this.produceHeat = produceHeat;
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
        private ItemStack getIcon() {
            ItemStack icon = null;
            if (this.resultItems != null && !this.resultItems.isEmpty()) {
                icon = this.resultItems.getFirst().stack();
            }
            if (icon == null && this.resultBlocks != null && !this.resultBlocks.isEmpty()) {
                Item item = this.resultBlocks.getFirst().state().getBlock().asItem();
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
                   + this.getHasCauldronPriority()
                   + (this.hasAnvil != null ? 1 : 0);
        }

        private int getHasCauldronPriority() {
            if (this.hasCauldron == null) return 0;
            int priority = 1;
            if (HasCauldron.isNotEmpty(this.hasCauldron.fluid())) priority++;
            if (HasCauldron.isNotEmpty(this.hasCauldron.transform())) priority++;
            return priority;
        }

        /**
         * 获取非冲突谓词列表
         *
         * @return 非冲突谓词列表
         */
        private List<IRecipePredicate<?>> getNonConflictingPredicates() {
            List<IRecipePredicate<?>> predicates = new ArrayList<>();
            if (this.hasCauldron != null) {
                predicates.add(this.hasCauldron.toHasCauldron(this.getCauldronOffset()));
            }
            if (!Objects.equals(this.hasAnvil, HasAnvil.DEFAULT)) {
                predicates.add(this.hasAnvil);
            }
            if (this.inputBlocks != null) {
                for (int i = 0; i < this.inputBlocks.size(); i++) {
                    BlockStatePredicate block = this.inputBlocks.get(i);
                    IRecipePredicate<?> hasBlock;
                    if (consumeInputBlocks) {
                        hasBlock = new HasBlockIngredient(this.getBlockInputOffset().subtract(0, i, 0), block);
                    } else {
                        hasBlock = new HasBlock(this.getBlockInputOffset().subtract(0, i, 0), block);
                    }
                    predicates.add(hasBlock);
                }
            }
            return predicates;
        }

        /**
         * 获取冲突谓词列表
         *
         * @return 冲突谓词列表
         */
        private List<IRecipePredicate<?>> getConflictingPredicates() {
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
        private List<IRecipeOutcome<?>> getOutcomes() {
            List<IRecipeOutcome<?>> outcomes = new ArrayList<>();
            if (this.resultItems != null) {
                for (ChanceItemStack chanceItemStack : this.resultItems) {
                    outcomes.add(chanceItemStack.toSpawnItem(this.itemOutputOffset));
                }
            }
            if (this.resultBlocks != null) {
                for (int i = 0; i < this.resultBlocks.size(); i++) {
                    ChanceBlockState chanceBlockState = this.resultBlocks.get(i);
                    outcomes.add(chanceBlockState.toSetBlock(this.getBlockOutputOffset().subtract(0, i, 0)));
                }
            }
            if (this.produceHeat != null) {
                outcomes.add(this.produceHeat);
            }
            outcomes.addAll(this.extraOutcomes);
            return outcomes;
        }

        public Vec3 getBlockOutputOffset() {
            return new Vec3(this.blockOutputOffset.getX(), this.blockOutputOffset.getY(), this.blockOutputOffset.getZ());
        }

        public Vec3 getBlockInputOffset() {
            return new Vec3(this.blockInputOffset.getX(), this.blockInputOffset.getY(), this.blockInputOffset.getZ());
        }

        public Vec3 getCauldronOffset() {
            return new Vec3(this.cauldronOffset.getX(), this.cauldronOffset.getY(), this.cauldronOffset.getZ());
        }
    }
}