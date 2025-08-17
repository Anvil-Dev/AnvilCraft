package dev.dubhe.anvilcraft.recipe.anvil.util;

import com.google.common.collect.AbstractIterator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * 距离定义类
 * <p>
 * 用于定义不同类型的距離计算方式和范围检查
 * </p>
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
        Vec3 dV = original.subtract(other);
        return switch (this.type) {
            case EUCLIDEAN -> dV.x * dV.x + dV.z * dV.z + (this.isHorizontal ? 0 : dV.y * dV.y) < this.distance * this.distance;
            case MANHATTAN -> Math.abs(dV.x) + Math.abs(dV.z) + (this.isHorizontal ? 0 : Math.abs(dV.y)) < this.distance;
            case CHEBYSHEV -> (this.isHorizontal ? Math.max(dV.x, dV.z) : Math.max(Math.max(dV.x, dV.z), dV.y)) < this.distance;
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
                private final int radiusSq = distance * distance;

                private int x = -distance;
                private int y = -distance;
                private int z = -distance - 1;

                @Override
                protected BlockPos computeNext() {
                    while (x <= distance) {
                        while (y <= distance) {
                            while (++z <= distance) {
                                if (x * x + y * y + z * z <= radiusSq) {
                                    return center.offset(x, y, z);
                                }
                            }
                            z = -distance - 1;
                            y++;
                        }
                        y = -distance;
                        x++;
                    }
                    return endOfData();
                }
            };
            case MANHATTAN -> BlockPos.withinManhattan(center, this.distance, this.distance, this.distance);
            case CHEBYSHEV -> BlockPos.betweenClosed(
                center.offset(-this.distance, -this.distance, -this.distance),
                center.offset(this.distance, this.distance, this.distance));
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
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = CodecUtil.enumStreamCodec(Type.class);
    }
}