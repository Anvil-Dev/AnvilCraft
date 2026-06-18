package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.block.state.IrradiatorType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
 *  序列装配配方中的“闪炼、时移、中子辐照、砧子辐照”实际上都是反向涂抹，
 *  因为并不是物品-炼药锅-方块操作，而是方块-方块操作
 */
public class ReversedSmearAlikeRecipe extends AbstractProcessRecipe<ReversedSmearAlikeRecipe> {

    public ReversedSmearAlikeRecipe(
        List<BlockStatePredicate> inputs,
        ChanceBlockState result
    ) {
        super(
            new Property()
                .setBlockInputOffset(new Vec3i(0, -1, 0))
                .setInputBlocks(inputs)
                .setBlockOutputOffset(new Vec3i(0, -1, 0))
                .setResultBlocks(result)
        );
    }

    @Override
    public RecipeSerializer<ReversedSmearAlikeRecipe> getSerializer() {
        return ModRecipeTypes.REVERSED_SMEAR_ALIKE_SERIALIZER.get();
    }

    @Override
    public RecipeType<ReversedSmearAlikeRecipe> getType() {
        return ModRecipeTypes.REVERSED_SMEAR_ALIKE_TYPE.get();
    }

    /**
     * 创建一个构建器实例
     *
     * @return 构建器实例
     */
    public static ReversedSmearAlikeRecipe.Builder builder() {
        return new ReversedSmearAlikeRecipe.Builder();
    }

    public static class Serializer implements RecipeSerializer<ReversedSmearAlikeRecipe> {
        /**
         * 编解码器
         */
        private static final MapCodec<ReversedSmearAlikeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStatePredicate.CODEC
                .listOf()
                .fieldOf("inputs")
                .forGetter(ReversedSmearAlikeRecipe::getInputBlocks),
            ChanceBlockState.CODEC.codec()
                .fieldOf("result")
                .forGetter(ReversedSmearAlikeRecipe::getFirstResultBlock)
        ).apply(instance, ReversedSmearAlikeRecipe::new));
        // 实际上它的输入输出和涂抹都是一模一样的，只是位置不一样……
        // 关于CODEC.codec()，ChanceBlockState.CODEC是MapCodec类型，
        // 虽然实际上MapCodec确实也有fieldOf()，
        // 不过这里还是保持和BlockSmearRecipe统一，转化成Codec类型了

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, ReversedSmearAlikeRecipe> STREAM_CODEC = StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ReversedSmearAlikeRecipe::getInputBlocks,
            ChanceBlockState.STREAM_CODEC,
            ReversedSmearAlikeRecipe::getFirstResultBlock,
            ReversedSmearAlikeRecipe::new
        );

        @Override
        public MapCodec<ReversedSmearAlikeRecipe> codec() {
            return ReversedSmearAlikeRecipe.Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReversedSmearAlikeRecipe> streamCodec() {
            return ReversedSmearAlikeRecipe.Serializer.STREAM_CODEC;
        }
    }

    public static class Builder extends AbstractRecipeBuilder<ReversedSmearAlikeRecipe> {
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
        public ReversedSmearAlikeRecipe.Builder input(BlockStatePredicate input) {
            this.inputs.add(input);
            return this;
        }

        /**
         * 添加输入方块（标签形式）
         *
         * @param input 输入方块标签
         * @return 构建器实例
         */
        public ReversedSmearAlikeRecipe.Builder input(TagKey<Block> input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            return this;
        }

        /**
         * 添加输入方块
         *
         * @param input 输入方块
         * @return 构建器实例
         */
        public ReversedSmearAlikeRecipe.Builder input(Block input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            return this;
        }

        // 小巧思来了：序列装配中所使用到的闪炼、时移、中子辐照、砧子辐照由于不涉及物品和炼药锅操作，实际上都是“反向涂抹”

        /**
         * 直接构建一个（用于序列装配配方的）伪闪炼配方
         *
         * @param input 输入的上方方块
         * @return 构建器实例
         */
        public ReversedSmearAlikeRecipe.Builder fakeSuperHeating(Block input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            this.inputs.add(BlockStatePredicate.builder()
                .of(ModBlocks.HEATER.get())
                .with(HeaterBlock.OVERLOAD, false)
                .build());
            return this;
        }

        /**
         * 构建一个（用于序列装配配方的）伪时移配方
         *
         * @param input 输入的上方方块
         * @return 构建器实例
         */
        public ReversedSmearAlikeRecipe.Builder fakeTimeWarp(Block input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            this.inputs.add(BlockStatePredicate.builder()
                .of(ModBlocks.CORRUPTED_BEACON.get())
                .with(CorruptedBeaconBlock.LIT, true)
                .build());
            return this;
        }

        // TODO: 在有人实现砧子辐照之后，这里还会需要再做相应修改；但是由于没有人实现该配方类型，所以暂时先不处理

        /**
         * 构建一个（用于序列装配配方的）伪中子辐照配方
         *
         * @param input 输入的上方方块
         * @return 构建器实例
         */
        public ReversedSmearAlikeRecipe.Builder fakeNeutronIrradiation(Block input, IrradiatorType type) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            this.inputs.add(BlockStatePredicate.builder()
                .of(ModBlocks.NEUTRON_IRRADIATOR.get())
                .with(NeutronIrradiatorBlock.TYPE, type)
                .build());
            return this;
        }

        /**
         * 设置结果方块
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public ReversedSmearAlikeRecipe.Builder result(ChanceBlockState result) {
            this.result = result;
            return this;
        }

        /**
         * 设置结果方块（默认概率为1.0f）
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public ReversedSmearAlikeRecipe.Builder result(Block result) {
            this.result = new ChanceBlockState(result.defaultBlockState(), 1.0f);
            return this;
        }

        @Override
        public ReversedSmearAlikeRecipe buildRecipe() {
            return new ReversedSmearAlikeRecipe(this.inputs, this.result);
        }

        @Override
        public void validate(ResourceLocation id) {
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("Recipe inputs must not be empty, RecipeId: " + id);
            }
            if (result == null) {
                throw new IllegalArgumentException("Recipe result must not be null, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "reversed_smear_alike";
        }

        @Override
        public Item getResult() {
            return WrapUtils.getItem(result);
        }
    }
}
