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
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        if (this.distance < 0) return false;
        Vec3 deltaV = original.subtract(other);
        return switch (this.type) {
            case EUCLIDEAN -> deltaV.x * deltaV.x
                              + deltaV.z * deltaV.z
                              + (this.isHorizontal ? 0 : deltaV.y * deltaV.y)
                              <= (double) this.distance * this.distance;
            case MANHATTAN -> Math.abs(deltaV.x) + Math.abs(deltaV.z)
                              + (this.isHorizontal ? 0 : Math.abs(deltaV.y)) <= this.distance;
            case CHEBYSHEV -> (this.isHorizontal
                               ? Math.max(Math.abs(deltaV.x), Math.abs(deltaV.z))
                               : Math.max(Math.max(Math.abs(deltaV.x), Math.abs(deltaV.z)), Math.abs(deltaV.y))
                              ) <= this.distance;
        };
    }

    /**
     * 获取范围内所有位置
     *
     * @param centerPos 中心位置
     * @return 位置迭代器
     */
    public Iterable<BlockPos> getAllPosesInRange(Vec3 centerPos) {
        if (this.distance < 0) return List.of();
        final BlockPos center = BlockPos.containing(centerPos.x, centerPos.y, centerPos.z);
        final int distance = this.distance;
        final int verticalDistance = this.isHorizontal ? 0 : distance;
        return () -> new AbstractIterator<>() {
            private long offsetX = -((long) distance);
            private long offsetY = -((long) verticalDistance);
            private long offsetZ = -((long) distance);

            @Override
            protected @Nullable BlockPos computeNext() {
                while (this.offsetX <= distance) {
                    while (this.offsetY <= verticalDistance) {
                        while (this.offsetZ <= distance) {
                            int offsetX = (int) this.offsetX;
                            int offsetY = (int) this.offsetY;
                            int offsetZ = (int) this.offsetZ++;
                            if (Distance.this.isOffsetInRange(offsetX, offsetY, offsetZ)) {
                                return center.offset(offsetX, offsetY, offsetZ);
                            }
                        }
                        this.offsetZ = -((long) distance);
                        this.offsetY++;
                    }
                    this.offsetY = -((long) verticalDistance);
                    this.offsetX++;
                }
                return endOfData();
            }
        };
    }

    private boolean isOffsetInRange(int offsetX, int offsetY, int offsetZ) {
        long absoluteX = Math.abs((long) offsetX);
        long absoluteY = Math.abs((long) offsetY);
        long absoluteZ = Math.abs((long) offsetZ);
        return switch (this.type) {
            case EUCLIDEAN -> (double) offsetX * offsetX
                              + (double) offsetZ * offsetZ
                              + (this.isHorizontal ? 0 : (double) offsetY * offsetY)
                              <= (double) this.distance * this.distance;
            case MANHATTAN -> absoluteX + absoluteZ + (this.isHorizontal ? 0 : absoluteY) <= this.distance;
            case CHEBYSHEV -> (this.isHorizontal
                               ? Math.max(absoluteX, absoluteZ)
                               : Math.max(Math.max(absoluteX, absoluteZ), absoluteY)
                              ) <= this.distance;
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
