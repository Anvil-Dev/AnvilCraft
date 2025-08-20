package dev.dubhe.anvilcraft.recipe.anvil.util;

import lombok.Data;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;

/**
 * 范围类
 * <p>
 * 定义一个三维空间范围，用于表示配方中的检测区域
 * </p>
 */
@Data
public class Range implements Iterable<BlockPos> {
    public static final Range EMPTY = new Range(Vec3.ZERO, Vec3.ZERO) {
        @Override
        public boolean isEmpty() {
            return true;
        }
    };

    /**
     * 起始点
     */
    @NotNull
    private Vec3 start;

    /**
     * 结束点
     */
    @NotNull
    private Vec3 end;

    /**
     * 构造一个范围
     *
     * @param start 起始点
     * @param end   结束点
     */
    public Range(@NotNull Vec3 start, @NotNull Vec3 end) {
        this.start = new Vec3(
            Math.min(start.x, end.x),
            Math.min(start.y, end.y),
            Math.min(start.z, end.z)
        );
        this.end = new Vec3(
            Math.max(start.x, end.x),
            Math.max(start.y, end.y),
            Math.max(start.z, end.z)
        );
    }

    /**
     * 根据中心点和范围创建范围
     *
     * @param pos   中心点
     * @param range 范围
     * @return 范围实例
     */
    public static @NotNull Range of(@NotNull Vec3 pos, @NotNull Vec3 range) {
        range = new Vec3(Math.abs(range.x()) / 2, Math.abs(range.y()) / 2, Math.abs(range.z()) / 2);
        return new Range(pos.subtract(range), pos.add(range));
    }

    /**
     * 检查是否包含另一个范围
     *
     * @param range 范围
     * @return 是否包含
     */
    public boolean contains(@NotNull Range range) {
        return range.start.x() >= this.start.x()
            && range.start.y() >= this.start.y()
            && range.start.z() >= this.start.z()
            && range.end.x() <= this.end.x()
            && range.end.y() <= this.end.y()
            && range.end.z() <= this.end.z();
    }

    /**
     * 检查是否包含指定位置和范围
     *
     * @param pos   位置
     * @param range 范围
     * @return 是否包含
     */
    public boolean contains(@NotNull Vec3 pos, @NotNull Vec3 range) {
        if (this.isEmpty()) return false;
        return this.contains(Range.of(pos, range));
    }

    /**
     * 检查是否与另一个范围交叉
     *
     * @param range 范围
     * @return 是否交叉
     */
    public boolean cross(@NotNull Range range) {
        if (this.isEmpty()) return false;
        return Math.max(range.start.x, this.start.x) < Math.min(range.end.x, this.end.x)
            && Math.max(range.start.y, this.start.y) < Math.min(range.end.y, this.end.y)
            && Math.max(range.start.z, this.start.z) < Math.min(range.end.z, this.end.z);
    }

    /**
     * 检查是否与指定位置和范围交叉
     *
     * @param pos   位置
     * @param range 范围
     * @return 是否交叉
     */
    public boolean cross(@NotNull Vec3 pos, @NotNull Vec3 range) {
        return this.cross(Range.of(pos, range));
    }

    /**
     * 扩展范围以包含另一个范围
     *
     * @param range 范围
     */
    public void grow(@NotNull Range range) {
        this.start = new Vec3(
            Math.min(range.start.x, this.start.x),
            Math.min(range.start.y, this.start.y),
            Math.min(range.start.z, this.start.z)
        );
        this.end = new Vec3(
            Math.max(range.end.x, this.end.x),
            Math.max(range.end.y, this.end.y),
            Math.max(range.end.z, this.end.z)
        );
    }

    public boolean isEmpty() {
        return false;
    }

    /**
     * 转换为AABB（无偏移）
     *
     * @return AABB
     */
    public AABB toAABB() {
        return this.toAABB(Vec3.ZERO);
    }

    /**
     * 转换为AABB（带偏移）
     *
     * @param offset 偏移量
     * @return AABB
     */
    public AABB toAABB(@NotNull Vec3 offset) {
        return new AABB(
            offset.x + this.start.x,
            offset.y + this.start.y,
            offset.z + this.start.z,
            offset.x + this.end.x,
            offset.y + this.end.y,
            offset.z + this.end.z
        );
    }

    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        return new BlockPosIterator(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Range range = (Range) o;
        return start.equals(range.start) && end.equals(range.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    /**
     * 方块位置迭代器
     */
    private static class BlockPosIterator implements Iterator<BlockPos> {
        private final Range range;
        private BlockPos lastPos = null;
        private boolean genNext = false;
        private BlockPos nextPos = null;
        private boolean end = false;

        /**
         * 构造一个方块位置迭代器
         *
         * @param range 范围
         */
        public BlockPosIterator(Range range) {
            this.range = range;
        }

        /**
         * 生成下一个位置
         */
        private void genNext() {
            if (this.end) return;
            if (this.genNext) return;
            this.genNext = true;
            int startX = (int) Math.floor(range.start.x);
            int startY = (int) Math.floor(range.start.y);
            int startZ = (int) Math.floor(range.start.z);
            if (this.lastPos == null) {
                this.nextPos = new BlockPos(startX, startY, startZ);
                return;
            }
            BlockPos nextPos = this.lastPos.offset(1, 0, 0);
            final Vec3 vec3_1 = new Vec3(1.0, 1.0, 1.0);
            if (this.range.cross(nextPos.getCenter(), vec3_1)) {
                this.nextPos = nextPos;
                return;
            }
            nextPos = new BlockPos(startX, this.lastPos.getY(), this.lastPos.getZ() + 1);
            if (this.range.cross(nextPos.getCenter(), vec3_1)) {
                this.nextPos = nextPos;
                return;
            }
            nextPos = new BlockPos(startX, this.lastPos.getY() + 1, startZ);
            if (this.range.cross(nextPos.getCenter(), vec3_1)) {
                this.nextPos = nextPos;
            }
        }

        @Override
        public boolean hasNext() {
            this.genNext();
            if (this.nextPos == null) {
                this.end = true;
                return false;
            }
            return true;
        }

        @Override
        public @Nullable BlockPos next() {
            BlockPos next = null;
            if (this.hasNext()) {
                next = this.nextPos;
            }
            this.nextPos = null;
            this.lastPos = next;
            this.genNext = false;
            return next;
        }
    }
}