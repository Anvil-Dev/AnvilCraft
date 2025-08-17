package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.injection.block.state.IBlockStateExtension;
import dev.dubhe.anvilcraft.init.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.cache.BlockCache;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 设置方块配方结果类，用于定义在配方执行时设置方块的结果
 * 该类实现了 IRecipeOutcome 接口，可以根据偏移量和概率设置指定位置的方块
 */
@Getter
public class SetBlock implements IRecipeOutcome<SetBlock> {
    /**
     * 方块状态
     */
    private final BlockState state;

    /**
     * 偏移量
     */
    private final Vec3 offset;

    /**
     * 概率
     */
    private final NumberProvider chance;

    /**
     * 构造一个新的设置方块配方结果
     *
     * @param state  方块状态
     * @param offset 偏移量
     * @param chance 概率
     */
    public SetBlock(BlockState state, Vec3 offset, NumberProvider chance) {
        this.state = state;
        this.offset = offset;
        this.chance = chance;
    }

    /**
     * 创建一个新的设置方块配方结果构建器
     *
     * @return 构建器实例
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * 获取配方结果类型
     *
     * @return 配方结果类型
     */
    @Override
    public Type getType() {
        return ModRecipeOutcomeTypes.SET_BLOCK.get();
    }

    /**
     * 接受配方上下文并处理设置方块的结果
     *
     * @param context 配方上下文
     */
    @Override
    public void accept(@NotNull InWorldRecipeContext context) {
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        cache.setBlock(BlockPos.containing(context.getPos().add(this.offset)), this.state);
        context.putAcceptor(BlockCache.BLOCK_CACHE.location(), BlockCache.DEFAULT_ACCEPTOR);
    }

    /**
     * 设置方块配方结果类型类
     */
    public static class Type implements IRecipeOutcome.Type<SetBlock> {
        /**
         * Map编解码器
         */
        private static final MapCodec<SetBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                IBlockStateExtension.MAP_CODEC
                    .forGetter(SetBlock::getState),
                Vec3.CODEC
                    .fieldOf("offset")
                    .forGetter(SetBlock::getOffset),
                CodecUtil.NUMBER_PROVIDER_CODEC
                    .optionalFieldOf("chance", ConstantValue.exactly(1.0f))
                    .forGetter(SetBlock::getChance)
            ).apply(instance, SetBlock::new));

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, SetBlock> STREAM_CODEC = StreamCodec.composite(
            BlockState.STREAM_CODEC,
            SetBlock::getState,
            RecipeUtil.VEC3_STREAM_CODEC,
            SetBlock::getOffset,
            RecipeUtil.NUMBER_PROVIDER_STREAM_CODEC,
            SetBlock::getChance,
            SetBlock::new
        );

        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public @NotNull MapCodec<SetBlock> codec() {
            return Type.CODEC;
        }

        /**
         * 获取StreamCodec编解码器
         *
         * @return StreamCodec编解码器
         */
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SetBlock> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }

    /**
     * 设置方块配方结果构建器类
     */
    public static class Builder {
        /**
         * 方块状态
         */
        private BlockState state = Blocks.AIR.defaultBlockState();

        /**
         * 偏移量
         */
        private Vec3 offset = Vec3.ZERO;

        /**
         * 概率
         */
        private NumberProvider chance = ConstantValue.exactly(1.0f);

        /**
         * 设置偏移量
         *
         * @param offset 偏移量
         * @return 构建器实例
         */
        public Builder offset(Vec3 offset) {
            this.offset = offset;
            return this;
        }

        /**
         * 设置偏移量
         *
         * @param x X轴偏移量
         * @param y Y轴偏移量
         * @param z Z轴偏移量
         * @return 构建器实例
         */
        public Builder offset(double x, double y, double z) {
            this.offset = new Vec3(x, y, z);
            return this;
        }

        /**
         * 设置向下偏移量
         *
         * @param below 向下偏移量
         * @return 构建器实例
         */
        public Builder below(double below) {
            return this.offset(Vec3.ZERO.subtract(0, below, 0));
        }

        /**
         * 设置向下偏移1格
         *
         * @return 构建器实例
         */
        public Builder below() {
            return this.below(1);
        }

        /**
         * 设置向上偏移量
         *
         * @param above 向上偏移量
         * @return 构建器实例
         */
        public Builder above(double above) {
            return this.offset(Vec3.ZERO.add(0, above, 0));
        }

        /**
         * 设置向上偏移1格
         *
         * @return 构建器实例
         */
        public Builder above() {
            return this.above(1);
        }

        /**
         * 设置概率
         *
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder chance(NumberProvider chance) {
            this.chance = chance;
            return this;
        }

        /**
         * 设置概率
         *
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder chance(float chance) {
            return this.chance(ConstantValue.exactly(chance));
        }

        /**
         * 设置方块状态
         *
         * @param state 方块状态
         * @return 构建器实例
         */
        public Builder block(BlockState state) {
            this.state = state;
            return this;
        }

        /**
         * 设置方块
         *
         * @param block 方块
         * @return 构建器实例
         */
        public Builder block(@NotNull Block block) {
            this.state = block.defaultBlockState();
            return this;
        }

        /**
         * 构建设置方块配方结果
         *
         * @return 设置方块配方结果
         */
        public SetBlock build() {
            return new SetBlock(state, offset, chance);
        }
    }
}