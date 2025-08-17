package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * 物品注入配方类
 * <p>
 * 该配方用于在铁砧下落时将物品注入到方块中，需要在铁砧下方放置特定方块作为注入目标
 * </p>
 */
@Getter
public class ItemInjectRecipe extends AbstractProcessRecipe<ItemInjectRecipe> {
    /**
     * 方块原料谓词
     */
    private final BlockStatePredicate blockIngredient;

    /**
     * 方块结果
     */
    private final ChanceBlockState blockResult;

    /**
     * 构造一个物品注入配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     * @param blockIngredient 方块原料谓词
     * @param blockResult     方块结果
     */
    public ItemInjectRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        BlockStatePredicate blockIngredient,
        ChanceBlockState blockResult
    ) {
        super(
            new Property()
                .setItemInputOffset(Vec3.ZERO)
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultItems(results)
                .setBlockInputOffset(new Vec3(0.0, -1.0, 0.0))
                .setInputBlocks(blockIngredient)
                .setBlockOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultBlocks(blockResult)
        );
        this.blockIngredient = blockIngredient;
        this.blockResult = blockResult;
    }

    @Override
    public @NotNull RecipeSerializer<ItemInjectRecipe> getSerializer() {
        return ModRecipeTypes.ITEM_INJECT_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<ItemInjectRecipe> getType() {
        return ModRecipeTypes.ITEM_INJECT_TYPE.get();
    }

    /**
     * 创建一个构建器实例
     *
     * @return 构建器实例
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * 物品注入配方序列化器
     */
    public static class Serializer implements RecipeSerializer<ItemInjectRecipe> {
        /**
         * 编解码器
         */
        private static final MapCodec<ItemInjectRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC
                .listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(ItemInjectRecipe::getInputItems),
            ChanceItemStack.CODEC
                .listOf()
                .optionalFieldOf("results", List.of())
                .forGetter(ItemInjectRecipe::getResultItems),
            BlockStatePredicate.CODEC
                .fieldOf("block_ingredient")
                .forGetter(ItemInjectRecipe::getBlockIngredient),
            ChanceBlockState.CODEC.codec()
                .fieldOf("block_result")
                .forGetter(ItemInjectRecipe::getBlockResult)
        ).apply(instance, ItemInjectRecipe::new));

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, ItemInjectRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ItemInjectRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ItemInjectRecipe::getResultItems,
            BlockStatePredicate.STREAM_CODEC,
            ItemInjectRecipe::getBlockIngredient,
            ChanceBlockState.STREAM_CODEC,
            ItemInjectRecipe::getBlockResult,
            ItemInjectRecipe::new
        );

        @Override
        public @NotNull MapCodec<ItemInjectRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ItemInjectRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    /**
     * 物品注入配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<ItemInjectRecipe, Builder> {
        /**
         * 方块原料谓词构建器
         */
        BlockStatePredicate.Builder blockIngredient = BlockStatePredicate.builder();

        /**
         * 方块结果
         */
        ChanceBlockState blockResult = null;

        /**
         * 设置输入方块
         *
         * @param block 输入方块
         * @return 构建器实例
         */
        public Builder inputBlock(Block block) {
            this.blockIngredient.of(block);
            return this;
        }

        /**
         * 设置输入方块（供应器形式）
         *
         * @param block 输入方块供应器
         * @return 构建器实例
         */
        public Builder inputBlock(@NotNull Supplier<? extends Block> block) {
            return this.inputBlock(block.get());
        }

        /**
         * 设置结果方块
         *
         * @param block 结果方块
         * @return 构建器实例
         */
        public Builder resultBlock(@NotNull Block block) {
            this.blockResult = new ChanceBlockState(block.defaultBlockState(), 1.0F);
            return this;
        }

        /**
         * 设置结果方块（供应器形式）
         *
         * @param block 结果方块供应器
         * @return 构建器实例
         */
        public Builder resultBlock(@NotNull Supplier<? extends Block> block) {
            return this.resultBlock(block.get());
        }

        @Override
        public @NotNull String getType() {
            return "item_inject";
        }

        @Override
        public void validate(@NotNull ResourceLocation pId) {
            if (this.itemIngredients.isEmpty()) {
                throw new IllegalArgumentException("Recipe ingredients must not be empty, RecipeId: " + pId);
            }
        }

        @Override
        protected ItemInjectRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new ItemInjectRecipe(itemIngredients, results, this.blockIngredient.build(), this.blockResult);
        }

        @Override
        public @NotNull Item getResult() {
            return WrapUtils.getItem(blockResult);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}