package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.util.Distance;
import dev.dubhe.anvilcraft.util.Util;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 产生热量配方结果类，用于定义在配方执行时产生热量的结果
 * 该类实现了 IRecipeOutcome 接口，可以对范围内的可加热方块产生热量效果
 */
@Getter(AccessLevel.PRIVATE)
public class ProduceHeat implements IRecipeOutcome<ProduceHeat> {
    /**
     * 热量数据列表
     */
    private final List<HeatData> heatData;

    /**
     * 距离范围
     */
    private final Distance distance;

    /**
     * 构造一个新的产生热量配方结果
     *
     * @param heatData 热量数据列表
     * @param distance 距离范围
     */
    private ProduceHeat(List<HeatData> heatData, Distance distance) {
        this.heatData = heatData;
        this.distance = distance;
    }

    /**
     * 创建一个新的产生热量配方结果构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取配方结果类型
     *
     * @return 配方结果类型
     */
    @Override
    public Type getType() {
        return ModRecipeOutcomeTypes.PRODUCE_HEAT.get();
    }

    /**
     * 接受配方上下文并处理产生热量的结果
     *
     * @param context 配方上下文
     */
    @Override
    public void accept(InWorldRecipeContext context) {
        ServerLevel level = context.getLevel();
        Vec3 center = context.getPos();
        for (BlockPos pos : this.distance.getAllPosesInRange(center)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlockTags.HEATABLE_BLOCKS)) continue;

            HeatableBlockEntity heatable = Util.castSafely(level.getBlockEntity(pos), HeatableBlockEntity.class).orElse(null);
            Optional<ResourceLocation> idOp = HeatRecorder.getId(level, pos, state);
            if (idOp.isEmpty()) continue;
            HeatTier currentTier = HeatRecorder.getTier(level, pos, state)
                .orElseThrow(() -> new IllegalStateException("Unexpected non tier heatable block!"));
            for (var info : this.heatData) {
                HeatTier tier = info.tier();
                int durationDelta = info.duration;
                if (tier.compareTo(currentTier) > 0) {
                    Block deltaBlock = HeatRecorder.getHeatableBlock(idOp.get(), tier).orElse(null);
                    if (deltaBlock == null) continue;
                    level.setBlockAndUpdate(pos, deltaBlock.defaultBlockState());
                    if (!(deltaBlock instanceof EntityBlock)) continue;
                    BlockEntity deltaBlockEntity = level.getBlockEntity(pos);
                    if (!(deltaBlockEntity instanceof HeatableBlockEntity heatableEntity)) continue;
                    heatable = heatableEntity;
                } else if (tier.compareTo(currentTier) < 0) {
                    durationDelta = 0;
                }
                if (heatable == null) continue;

                if (durationDelta > 0) {
                    heatable.addDurationInTick(durationDelta);
                }
                heatable = null;
            }
        }
    }

    /**
     * 产生热量配方结果类型类
     */
    public static class Type implements IRecipeOutcome.Type<ProduceHeat> {
        /**
         * Map编解码器
         */
        public static final MapCodec<ProduceHeat> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            HeatData.CODEC.listOf().fieldOf("heatData").forGetter(ProduceHeat::getHeatData),
            Distance.CODEC.fieldOf("distance").forGetter(ProduceHeat::getDistance)
        ).apply(ins, ProduceHeat::new));

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, ProduceHeat> STREAM_CODEC = StreamCodec.composite(
            HeatData.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ProduceHeat::getHeatData,
            Distance.STREAM_CODEC,
            ProduceHeat::getDistance,
            ProduceHeat::new
        );

        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public @NotNull MapCodec<ProduceHeat> codec() {
            return Type.MAP_CODEC;
        }

        /**
         * 获取StreamCodec编解码器
         *
         * @return StreamCodec编解码器
         */
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ProduceHeat> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }

    /**
     * 热量数据记录类
     */
    public record HeatData(
        HeatTier tier, // 热量等级
        int duration // 持续时间
    ) {
        /**
         * Map编解码器
         */
        public static final Codec<HeatData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            HeatTier.LOWER_NAME_CODEC.fieldOf("tier").forGetter(HeatData::tier),
            Codec.INT.fieldOf("duration").forGetter(HeatData::duration)
        ).apply(ins, HeatData::new));

        /**
         * 流编解码器
         */
        public static final StreamCodec<ByteBuf, HeatData> STREAM_CODEC = StreamCodec.composite(
            HeatTier.STREAM_CODEC, HeatData::tier,
            ByteBufCodecs.VAR_INT, HeatData::duration,
            HeatData::new
        );
    }

    /**
     * 产生热量配方结果构建器类
     */
    public static class Builder {
        /**
         * 可达到的热量数据列表
         */
        private final List<HeatData> canReach = new ArrayList<>();

        /**
         * 距离范围
         */
        private Distance distance = Distance.DEFAULT;

        /**
         * 添加热量数据
         *
         * @param tier     热量等级
         * @param duration 持续时间
         * @return 构建器实例
         */
        public Builder heat(HeatTier tier, int duration) {
            this.canReach.add(new HeatData(tier, duration));
            return this;
        }

        /**
         * 设置距离范围
         *
         * @param distance 距离范围
         * @return 构建器实例
         */
        public Builder distance(Distance distance) {
            this.distance = distance;
            return this;
        }

        /**
         * 设置距离范围
         *
         * @param type         距离类型
         * @param distance     距离
         * @param isHorizontal 是否水平
         * @return 构建器实例
         */
        public Builder distance(Distance.Type type, int distance, boolean isHorizontal) {
            return this.distance(new Distance(type, distance, isHorizontal));
        }

        /**
         * 设置欧几里得距离范围
         *
         * @param distance     距离
         * @param isHorizontal 是否水平
         * @return 构建器实例
         */
        public Builder distanceEuclidean(int distance, boolean isHorizontal) {
            return this.distance(Distance.Type.EUCLIDEAN, distance, isHorizontal);
        }

        /**
         * 设置欧几里得距离范围
         *
         * @param isHorizontal 是否水平
         * @return 构建器实例
         */
        public Builder distanceEuclidean(boolean isHorizontal) {
            return this.distance(Distance.Type.EUCLIDEAN, 1, isHorizontal);
        }

        /**
         * 设置欧几里得距离范围
         *
         * @param distance 距离
         * @return 构建器实例
         */
        public Builder distanceEuclidean(int distance) {
            return this.distance(Distance.Type.EUCLIDEAN, distance, true);
        }

        /**
         * 设置欧几里得距离范围
         *
         * @return 构建器实例
         */
        public Builder distanceEuclidean() {
            return this.distance(Distance.Type.EUCLIDEAN, 1, true);
        }

        /**
         * 设置曼哈顿距离范围
         *
         * @param distance 距离
         * @param isHorizontal 是否水平
         * @return 构建器实例
         */
        public Builder distanceManhattan(int distance, boolean isHorizontal) {
            return this.distance(Distance.Type.MANHATTAN, distance, isHorizontal);
        }

        /**
         * 设置曼哈顿距离范围
         *
         * @param isHorizontal 是否水平
         * @return 构建器实例
         */
        public Builder distanceManhattan(boolean isHorizontal) {
            return this.distance(Distance.Type.MANHATTAN, 1, isHorizontal);
        }

        /**
         * 设置曼哈顿距离范围
         *
         * @param distance 距离
         * @return 构建器实例
         */
        public Builder distanceManhattan(int distance) {
            return this.distance(Distance.Type.MANHATTAN, distance, true);
        }

        /**
         * 设置曼哈顿距离范围
         *
         * @return 构建器实例
         */
        public Builder distanceManhattan() {
            return this.distance(Distance.Type.MANHATTAN, 1, true);
        }

        /**
         * 设置切比雪夫距离范围
         *
         * @param distance 距离
         * @param isHorizontal 是否水平
         * @return 构建器实例
         */
        public Builder distanceChebyshev(int distance, boolean isHorizontal) {
            return this.distance(Distance.Type.CHEBYSHEV, distance, isHorizontal);
        }

        /**
         * 设置切比雪夫距离范围
         *
         * @param isHorizontal 是否水平
         * @return 构建器实例
         */
        public Builder distanceChebyshev(boolean isHorizontal) {
            return this.distance(Distance.Type.CHEBYSHEV, 1, isHorizontal);
        }

        /**
         * 设置切比雪夫距离范围
         *
         * @param distance 距离
         * @return 构建器实例
         */
        public Builder distanceChebyshev(int distance) {
            return this.distance(Distance.Type.CHEBYSHEV, distance, true);
        }

        /**
         * 设置切比雪夫距离范围
         *
         * @return 构建器实例
         */
        public Builder distanceChebyshev() {
            return this.distance(Distance.Type.CHEBYSHEV, 1, true);
        }

        /**
         * 构建产生热量配方结果
         *
         * @return 产生热量配方结果
         */
        public ProduceHeat build() {
            return new ProduceHeat(this.canReach, this.distance);
        }
    }
}