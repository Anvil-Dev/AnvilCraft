package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 方块粉碎配方类
 * <p>
 * 该配方用于在铁砧下落时粉碎方块，是方块级别的粉碎处理配方
 * </p>
 */
@Getter
public class BlockCrushRecipe extends AbstractProcessRecipe<BlockCrushRecipe> {
    /**
     * 输入方块谓词
     */
    private final BlockStatePredicate input;

    /**
     * 结果方块
     */
    private final ChanceBlockState result;

    /**
     * 构造一个方块粉碎配方
     *
     * @param input  输入方块谓词
     * @param result 结果方块
     */
    public BlockCrushRecipe(
        BlockStatePredicate input,
        ChanceBlockState result
    ) {
        super(
            new AbstractProcessRecipe.Property()
                .setBlockInputOffset(new Vec3(0.0, -1.0, 0.0))
                .setInputBlocks(input)
                .setBlockOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultBlocks(result)
        );
        this.input = input;
        this.result = result;
    }

    @Override
    public @NotNull RecipeType<BlockCrushRecipe> getType() {
        return ModRecipeTypes.BLOCK_CRUSH_TYPE.get();
    }

    @Override
    public @NotNull RecipeSerializer<BlockCrushRecipe> getSerializer() {
        return ModRecipeTypes.BLOCK_CRUSH_SERIALIZER.get();
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
     * 方块粉碎配方序列化器
     */
    public static class Serializer implements RecipeSerializer<BlockCrushRecipe> {
        /**
         * 编解码器
         */
        private static final MapCodec<BlockCrushRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStatePredicate.CODEC
                .fieldOf("input")
                .forGetter(BlockCrushRecipe::getInput),
            ChanceBlockState.CODEC.codec()
                .fieldOf("result")
                .forGetter(BlockCrushRecipe::getResult)
        ).apply(instance, BlockCrushRecipe::new));

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, BlockCrushRecipe> STREAM_CODEC = StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC,
            BlockCrushRecipe::getInput,
            ChanceBlockState.STREAM_CODEC,
            BlockCrushRecipe::getResult,
            BlockCrushRecipe::new
        );

        @Override
        public @NotNull MapCodec<BlockCrushRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, BlockCrushRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    /**
     * 方块粉碎配方构建器
     */
    public static class Builder extends AbstractRecipeBuilder<BlockCrushRecipe> {
        /**
         * 输入方块谓词
         */
        private BlockStatePredicate input = null;

        /**
         * 结果方块
         */
        private ChanceBlockState result = null;

        /**
         * 设置输入方块
         *
         * @param input 输入方块谓词
         * @return 构建器实例
         */
        public Builder input(BlockStatePredicate input) {
            this.input = (input);
            return this;
        }

        /**
         * 设置输入方块（标签形式）
         *
         * @param input 输入方块标签
         * @return 构建器实例
         */
        public Builder input(TagKey<Block> input) {
            this.input = BlockStatePredicate.builder().of(input).build();
            return this;
        }

        /**
         * 设置输入方块
         *
         * @param input 输入方块
         * @return 构建器实例
         */
        public Builder input(Block input) {
            this.input = (BlockStatePredicate.builder().of(input).build());
            return this;
        }

        /**
         * 设置结果方块
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder result(ChanceBlockState result) {
            this.result = (result);
            return this;
        }

        /**
         * 设置结果方块（默认概率为1.0f）
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder result(@NotNull Block result) {
            this.result = (new ChanceBlockState(result.defaultBlockState(), 1.0f));
            return this;
        }

        @Override
        public @NotNull BlockCrushRecipe buildRecipe() {
            return new BlockCrushRecipe(this.input, this.result);
        }

        @Override
        public void validate(@NotNull ResourceLocation pId) {
            if (input == null) {
                throw new IllegalArgumentException("Recipe input must not be null, RecipeId: " + pId);
            }
            if (result == null) {
                throw new IllegalArgumentException("Recipe result must not be null, RecipeId: " + pId);
            }
        }

        @Override
        public @NotNull String getType() {
            return "block_crush";
        }

        @Override
        public @NotNull Item getResult() {
            return WrapUtils.getItem(result);
        }
    }
}