package dev.dubhe.anvilcraft.recipe.anvil.util;

import com.google.common.collect.AbstractIterator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 距离定义类
 *
 * <p>用于定义不同类型的距離计算方式和范围检查</p>
 */
public record Distance(Type type, int distance, boolean isHorizontal) {
    /**
     * 默认距离（曼哈顿距离，距离1，水平方向）
     */
    public static final Distance DEFAULT = new Distance(Type.MANHATTAN, 1, true);

    /**
     * Distance编解码器
     */
    public static final Codec<Distance> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Type.LOWER_NAME_CODEC.fieldOf("type").forGetter(Distance::type),
        Codec.INT.fieldOf("distance").forGetter(Distance::distance),
        Codec.BOOL.fieldOf("isHorizontal").forGetter(Distance::isHorizontal)
    ).apply(ins, Distance::new));

    /**
     * Distance流编解码器
     */
    public static final StreamCodec<ByteBuf, Distance> STREAM_CODEC = StreamCodec.composite(
        Type.STREAM_CODEC, Distance::type,
        ByteBufCodecs.VAR_INT, Distance::distance,
        ByteBufCodecs.BOOL, Distance::isHorizontal,
        Distance::new
    );

    /**
     * 检查点是否在范围内
     *
     * @param original 原点
     * @param other    其他点
     * @return 是否在范围内
     */
    public boolean isInRange(Vec3 original, Vec3 other) {
        Vec3 deltaV = original.subtract(other);
        return switch (this.type) {
            case EUCLIDEAN -> deltaV.x * deltaV.x
                              + deltaV.z * deltaV.z
                              + (this.isHorizontal ? 0 : deltaV.y * deltaV.y)
                              < this.distance * this.distance;
            case MANHATTAN -> Math.abs(deltaV.x) + Math.abs(deltaV.z) + (this.isHorizontal ? 0 : Math.abs(deltaV.y)) < this.distance;
            case CHEBYSHEV -> (this.isHorizontal
                               ? Math.max(deltaV.x, deltaV.z)
                               : Math.max(Math.max(deltaV.x, deltaV.z), deltaV.y)
                              ) < this.distance;
        };
    }

    /**
     * 获取范围内所有位置
     *
     * @param centerPos 中心位置
     * @return 位置迭代器
     */
    public Iterable<BlockPos> getAllPosesInRange(Vec3 centerPos) {
        final BlockPos center = BlockPos.containing(centerPos.x, centerPos.y, centerPos.z);
        return switch (this.type) {
            case EUCLIDEAN -> () -> new AbstractIterator<>() {
                private final int radiusSq = Distance.this.distance * Distance.this.distance;

                private int offsetX = -Distance.this.distance;
                private int offsetY = -Distance.this.distance;
                private int offsetZ = -Distance.this.distance - 1;

                @Override
                protected @Nullable BlockPos computeNext() {
                    while (this.offsetX <= Distance.this.distance) {
                        while (this.offsetY <= Distance.this.distance) {
                            while (++this.offsetZ <= Distance.this.distance) {
                                if (
                                    this.offsetX * this.offsetX
                                    + this.offsetY * this.offsetY
                                    + this.offsetZ * this.offsetZ
                                    <= this.radiusSq
                                ) {
                                    return center.offset(this.offsetX, this.offsetY, this.offsetZ);
                                }
                            }
                            this.offsetZ = -Distance.this.distance - 1;
                            this.offsetY++;
                        }
                        this.offsetY = -Distance.this.distance;
                        this.offsetX++;
                    }
                    return endOfData();
                }
            };
            case MANHATTAN -> BlockPos.withinManhattan(center, this.distance, this.distance, this.distance);
            case CHEBYSHEV -> BlockPos.betweenClosed(
                center.offset(-this.distance, -this.distance, -this.distance),
                center.offset(this.distance, this.distance, this.distance)
            );
        };
    }

    /**
     * 距离类型枚举
     */
    public enum Type {
        /**
         * 欧几里得距离
         */
        EUCLIDEAN,

        /**
         * 曼哈顿距离
         */
        MANHATTAN,

        /**
         * 切比雪夫距离
         */
        CHEBYSHEV;

        /**
         * 小写名称编解码器
         */
        public static final Codec<Type> LOWER_NAME_CODEC = CodecUtil.enumCodecInLowerName(Type.class);

        /**
         * Type流编解码器
         */
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(Type.class);
    }
}
