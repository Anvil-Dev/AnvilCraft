package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/// 方块粉碎配方类
///
/// <p>该配方用于在铁砧下落时粉碎方块，是方块级别的粉碎处理配方</p>
public class BlockCrushRecipe extends AbstractProcessRecipe<BlockCrushRecipe> {
    public static final RecipeSerializer<BlockCrushRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStatePredicate.CODEC
                .fieldOf("input")
                .forGetter(BlockCrushRecipe::getFirstInputBlock),
            ChanceBlockState.CODEC.codec()
                .fieldOf("result")
                .forGetter(BlockCrushRecipe::getFirstResultBlock)
        ).apply(instance, BlockCrushRecipe::new)),
        StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC,
            BlockCrushRecipe::getFirstInputBlock,
            ChanceBlockState.STREAM_CODEC,
            BlockCrushRecipe::getFirstResultBlock,
            BlockCrushRecipe::new
        )
    );

    /// 构造一个方块粉碎配方
    ///
    /// @param input  输入方块谓词
    /// @param result 结果方块
    public BlockCrushRecipe(
        BlockStatePredicate input,
        ChanceBlockState result
    ) {
        super(
            new AbstractProcessRecipe.Property()
                .setBlockInputOffset(new Vec3i(0, -1, 0))
                .setConsumeInputBlocks(true)
                .setInputBlocks(input)
                .setBlockOutputOffset(new Vec3i(0, -1, 0))
                .setResultBlocks(result)
        );
    }

    @Override
    public RecipeType<BlockCrushRecipe> getType() {
        return ModRecipeTypes.BLOCK_CRUSH.get();
    }

    @Override
    public RecipeSerializer<BlockCrushRecipe> getSerializer() {
        return BlockCrushRecipe.SERIALIZER;
    }

    /// 创建一个构建器实例
    ///
    /// @return 构建器实例
    public static Builder builder() {
        return new Builder();
    }

    /// 方块粉碎配方构建器
    public static class Builder extends AbstractRecipeBuilder<BlockCrushRecipe> {
        /// 输入方块谓词
        private @Nullable BlockStatePredicate input = null;

        /// 结果方块
        private @Nullable ChanceBlockState result = null;

        /// 设置输入方块
        ///
        /// @param input 输入方块谓词
        ///
        /// @return 构建器实例
        public Builder input(BlockStatePredicate input) {
            this.input = (input);
            return this;
        }

        /// 设置输入方块（标签形式）
        ///
        /// @param input 输入方块标签
        ///
        /// @return 构建器实例
        public Builder input(HolderGetter<Block> blocks, TagKey<Block> input) {
            this.input = BlockStatePredicate.builder().of(blocks, input).build();
            return this;
        }

        /// 设置输入方块
        ///
        /// @param input 输入方块
        ///
        /// @return 构建器实例
        public Builder input(Block input) {
            this.input = (BlockStatePredicate.builder().of(input).build());
            return this;
        }

        /// 设置结果方块
        ///
        /// @param result 结果方块
        ///
        /// @return 构建器实例
        public Builder result(ChanceBlockState result) {
            this.result = (result);
            return this;
        }

        /// 设置结果方块（默认概率为1.0F）
        ///
        /// @param result 结果方块
        ///
        /// @return 构建器实例
        public Builder result(Block result) {
            this.result = (new ChanceBlockState(result.defaultBlockState(), 1.0F));
            return this;
        }

        @Override
        public BlockCrushRecipe buildRecipe() {
            return new BlockCrushRecipe(this.input, this.result);
        }

        @Override
        public void validate(Identifier id) {
            if (this.input == null) {
                throw new IllegalArgumentException("Recipe input must not be null, RecipeId: " + id);
            }
            if (this.result == null) {
                throw new IllegalArgumentException("Recipe result must not be null, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "block_crush";
        }

        @Override
        public ItemStackTemplate getResult() {
            return WrapUtils.getItem(this.result);
        }
    }
}
