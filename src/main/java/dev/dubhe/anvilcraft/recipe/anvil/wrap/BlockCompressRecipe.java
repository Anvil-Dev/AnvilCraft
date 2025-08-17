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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块压缩配方类
 * <p>
 * 该配方用于在铁砧下落时压缩方块，是方块级别的压缩处理配方
 * </p>
 */
@Getter
public class BlockCompressRecipe extends AbstractProcessRecipe<BlockCompressRecipe> {
    /**
     * 输入方块列表
     */
    private final List<BlockStatePredicate> inputs;

    /**
     * 结果方块列表
     */
    private final List<ChanceBlockState> results;

    /**
     * 构造一个方块压缩配方
     *
     * @param inputs  输入方块列表
     * @param results 结果方块列表
     */
    public BlockCompressRecipe(
        List<BlockStatePredicate> inputs,
        List<ChanceBlockState> results
    ) {
        super(
            new AbstractProcessRecipe.Property()
                .setBlockInputOffset(new Vec3(0.0, -1.0, 0.0))
                .setInputBlocks(inputs)
                .setBlockOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultBlocks(results)
        );
        this.inputs = inputs;
        this.results = results;
    }

    @Override
    public @NotNull RecipeSerializer<BlockCompressRecipe> getSerializer() {
        return ModRecipeTypes.BLOCK_COMPRESS_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<BlockCompressRecipe> getType() {
        return ModRecipeTypes.BLOCK_COMPRESS_TYPE.get();
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
     * 方块压缩配方序列化器
     */
    public static class Serializer implements RecipeSerializer<BlockCompressRecipe> {
        /**
         * 编解码器
         */
        private static final MapCodec<BlockCompressRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStatePredicate.CODEC
                .listOf()
                .fieldOf("inputs")
                .forGetter(BlockCompressRecipe::getInputs),
            ChanceBlockState.CODEC.codec()
                .listOf()
                .fieldOf("results")
                .forGetter(BlockCompressRecipe::getResults)
        ).apply(instance, BlockCompressRecipe::new));

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, BlockCompressRecipe> STREAM_CODEC = StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BlockCompressRecipe::getInputs,
            ChanceBlockState.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BlockCompressRecipe::getResults,
            BlockCompressRecipe::new
        );

        @Override
        public @NotNull MapCodec<BlockCompressRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, BlockCompressRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    /**
     * 方块压缩配方构建器
     */
    public static class Builder extends AbstractRecipeBuilder<BlockCompressRecipe> {
        /**
         * 输入方块列表
         */
        private final List<BlockStatePredicate> inputs = new ArrayList<>();

        /**
         * 结果方块列表
         */
        private final List<ChanceBlockState> results = new ArrayList<>();

        /**
         * 添加输入方块
         *
         * @param input 输入方块谓词
         * @return 构建器实例
         */
        public Builder input(BlockStatePredicate input) {
            this.inputs.add(input);
            return this;
        }

        /**
         * 添加输入方块（标签形式）
         *
         * @param input 输入方块标签
         * @return 构建器实例
         */
        public Builder input(TagKey<Block> input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            return this;
        }

        /**
         * 添加输入方块
         *
         * @param input 输入方块
         * @return 构建器实例
         */
        public Builder input(Block input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            return this;
        }

        /**
         * 添加结果方块
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder result(ChanceBlockState result) {
            this.results.add(result);
            return this;
        }

        /**
         * 添加结果方块（默认概率为1.0f）
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder result(@NotNull Block result) {
            this.results.add(new ChanceBlockState(result.defaultBlockState(), 1.0f));
            return this;
        }

        @Override
        public @NotNull BlockCompressRecipe buildRecipe() {
            return new BlockCompressRecipe(inputs, results);
        }

        @Override
        public void validate(@NotNull ResourceLocation pId) {
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("Recipe inputs must not be empty, RecipeId: " + pId);
            }
            if (results.isEmpty()) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + pId);
            }
        }

        @Override
        public @NotNull String getType() {
            return "block_compress";
        }

        @Override
        public @NotNull Item getResult() {
            return WrapUtils.getItem(results);
        }
    }
}