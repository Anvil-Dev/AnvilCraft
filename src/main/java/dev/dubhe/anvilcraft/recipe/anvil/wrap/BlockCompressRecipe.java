package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块压缩配方类
 *
 * <p>该配方用于在铁砧下落时压缩方块，是方块级别的压缩处理配方</p>
 */
public class BlockCompressRecipe extends AbstractProcessRecipe<BlockCompressRecipe> {
    /**
     * 构造一个方块压缩配方
     *
     * @param inputs 输入方块列表
     * @param result 结果方块
     */
    public BlockCompressRecipe(
        List<BlockStatePredicate> inputs,
        ChanceBlockState result
    ) {
        super(
            new AbstractProcessRecipe.Property()
                .setBlockInputOffset(new Vec3i(0, -1, 0))
                .setConsumeInputBlocks(true)
                .setInputBlocks(inputs)
                .setBlockOutputOffset(new Vec3i(0, -2, 0))
                .setResultBlocks(result)
        );
        if (inputs.size() != 2) throw new IllegalArgumentException("AnvilItemProcessRecipe only support 2 inputs");
    }

    @Override
    public RecipeSerializer<BlockCompressRecipe> getSerializer() {
        return ModRecipeTypes.BLOCK_COMPRESS_SERIALIZER.get();
    }

    @Override
    public RecipeType<BlockCompressRecipe> getType() {
        return ModRecipeTypes.BLOCK_COMPRESS_TYPE.get();
    }

    /**
     * 创建一个构建器实例
     *
     * @return 构建器实例
     */
    public static Builder builder() {
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
                .forGetter(BlockCompressRecipe::getInputBlocks),
            ChanceBlockState.CODEC.codec()
                .fieldOf("result")
                .forGetter(BlockCompressRecipe::getFirstResultBlock)
        ).apply(instance, BlockCompressRecipe::new));

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, BlockCompressRecipe> STREAM_CODEC = StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BlockCompressRecipe::getInputBlocks,
            ChanceBlockState.STREAM_CODEC,
            BlockCompressRecipe::getFirstResultBlock,
            BlockCompressRecipe::new
        );

        @Override
        public MapCodec<BlockCompressRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockCompressRecipe> streamCodec() {
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
         * 结果方块
         */
        private ChanceBlockState result = null;

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
            this.result = result;
            return this;
        }

        /**
         * 添加结果方块（默认概率为1.0f）
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder result(Block result) {
            this.result = new ChanceBlockState(result.defaultBlockState(), 1.0f);
            return this;
        }

        @Override
        public BlockCompressRecipe buildRecipe() {
            return new BlockCompressRecipe(inputs, result);
        }

        @Override
        public void validate(ResourceLocation id) {
            if (inputs.size() != 2) {
                throw new IllegalArgumentException("Recipe input list size must in 2, RecipeId: " + id);
            }
            if (result == null) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "block_compress";
        }

        @Override
        public Item getResult() {
            return WrapUtils.getItem(result);
        }
    }
}