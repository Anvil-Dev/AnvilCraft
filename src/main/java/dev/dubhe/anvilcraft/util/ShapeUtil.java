package dev.dubhe.anvilcraft.util;

import com.mojang.datafixers.util.Pair;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Slf4j(topic = "ShapeUtil")
public class ShapeUtil {
    public static Future<VoxelShape> threadedJoin(
        List<VoxelShape> shapes,
        BooleanOp function,
        ExecutorService executor
    ) {
        return executor.submit(new ThreadedJoinTask(shapes, function, executor));
    }

    private record ThreadedJoinTask(
        List<VoxelShape> input,
        BooleanOp function,
        ExecutorService executorService
    ) implements Callable<VoxelShape> {

        @SneakyThrows
        @Override
        public VoxelShape call() {
            long timeStart = System.currentTimeMillis();
            List<VoxelShape> shapes = new ArrayList<>(input);
            log.debug("We have {} shapes to merge.", shapes.size());
            while (shapes.size() > 1) {
                if (shapes.size() % 2 != 0) {
                    shapes.add(Shapes.empty());
                }
                List<Future<VoxelShape>> futures = new ArrayList<>(shapes.size() / 2);
                List<Pair<VoxelShape, VoxelShape>> slices = slice2(shapes);
                log.debug("Grouped merging into {} groups.", slices.size());
                for (Pair<VoxelShape, VoxelShape> slice : slices) {
                    futures.add(executorService.submit(new ShapeJoinTask(slice, function)));
                }
                spinWait(futures);
                List<VoxelShape> list = new ArrayList<>();
                for (Future<VoxelShape> future : futures) {
                    VoxelShape shape = future.get();
                    list.add(shape);
                }
                log.debug("Merge {} groups completed.", slices.size());
                shapes = list;
            }
            log.debug("Merge {} shapes took {} milliseconds.", input.size(), System.currentTimeMillis() - timeStart);
            return shapes.getFirst();
        }
    }

    private record ShapeJoinTask(
        Pair<VoxelShape, VoxelShape> input,
        BooleanOp function
    ) implements Callable<VoxelShape> {

        @Override
        public VoxelShape call() {
            log.debug("Merging {} and {}", input.getFirst(), input.getSecond());
            if (input.getFirst() == Shapes.empty()) {
                return input.getSecond();
            }
            if (input.getSecond() == Shapes.empty()) {
                return input.getFirst();
            }
            return Shapes.join(input.getFirst(), input.getSecond(), function);
        }
    }

    private static <T> void spinWait(List<Future<T>> futures) {
        boolean completed = false;
        while (!completed) {
            completed = true;
            for (Future<T> future : futures) {
                if (!future.isDone()) completed = false;
            }
        }
    }

    private static List<Pair<VoxelShape, VoxelShape>> slice2(List<VoxelShape> input) {
        List<Pair<VoxelShape, VoxelShape>> result = new ArrayList<>();
        for (int i = 0; i < input.size(); i += 2) {
            result.add(Pair.of(input.get(i), input.get(i + 1)));
        }
        return result;
    }

    /**
     * 合并指定的若干形状
     *
     * @param shapes 子形状
     *
     * @return 总形状
     * @apiNote 仅应用于方块形状初始化！
     */
    public static VoxelShape merge(VoxelShape... shapes) {
        if (shapes.length == 0) return Shapes.empty();
        return Stream.of(shapes).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    }

    /**
     * 合并指定的若干碰撞箱
     *
     * @param shapes 子碰撞箱
     *
     * @return 总形状
     * @apiNote 仅应用于方块形状初始化！
     */
    public static VoxelShape merge(AABB... shapes) {
        if (shapes.length == 0) return Shapes.empty();
        return Stream.of(shapes).map(ShapeUtil::box).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    }

    /**
     * 等同于 {@link Block#box(double, double, double, double, double, double) Block.box()}
     *
     * @param shape 16x长度的碰撞箱
     *
     * @return 1x长度的碰撞箱
     */
    public static VoxelShape box(AABB shape) {
        return Block.box(shape.minX, shape.minY, shape.minZ, shape.maxX, shape.maxY, shape.maxZ);
    }

    /**
     * 按逆时针旋转指定的形状
     *
     * @param axis  旋转轴
     * @param angle 旋转角度
     * @param shape 形状
     *
     * @return 旋转后的形状
     * @apiNote 仅应用于方块形状初始化！
     */
    public static VoxelShape rotate(Direction.Axis axis, float angle, VoxelShape shape) {
        List<AABB> shapes = shape.toAabbs();
        AABB[] result = new AABB[shapes.size()];
        for (int i = 0; i < shapes.size(); i++) {
            AABB unrotated = shapes.get(i);
            unrotated = new AABB(unrotated.getMinPosition().scale(16), unrotated.getMaxPosition().scale(16));
            result[i] = ShapeUtil.rotate(axis, angle, unrotated);
        }
        return ShapeUtil.merge(result);
    }

    /**
     * 按逆时针旋转指定的若干碰撞箱
     *
     * @param axis   旋转轴
     * @param angle  旋转角度
     * @param shapes 16x长度的碰撞箱
     *
     * @return 旋转后的16x长度碰撞箱
     * @apiNote 仅应用于方块形状初始化！
     */
    public static AABB[] rotate(Direction.Axis axis, float angle, AABB... shapes) {
        AABB[] result = new AABB[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            result[i] = ShapeUtil.rotate(axis, angle, shapes[i]);
        }
        return result;
    }

    /**
     * 按逆时针旋转指定的碰撞箱
     *
     * @param axis  旋转轴
     * @param angle 旋转角度
     * @param shape 16x长度的碰撞箱
     *
     * @return 旋转后的16x长度碰撞箱
     * @apiNote 仅应用于方块形状初始化！
     */
    public static AABB rotate(Direction.Axis axis, float angle, AABB shape) {
        if (angle == 0) return shape;

        Vec3 min = shape.getMinPosition().subtract(8, 8, 8);
        Vec3 max = shape.getMaxPosition().subtract(8, 8, 8);

        switch (axis) {
            case X -> {
                min = min.xRot((float) Math.toRadians(angle));
                max = max.xRot((float) Math.toRadians(angle));
            }
            case Y -> {
                min = min.yRot((float) Math.toRadians(angle));
                max = max.yRot((float) Math.toRadians(angle));
            }
            case Z -> {
                min = min.zRot((float) Math.toRadians(angle));
                max = max.zRot((float) Math.toRadians(angle));
            }
            default -> {
            }
        }
        return new AABB(min.add(8, 8, 8), max.add(8, 8, 8));
    }

    /**
     * 镜像指定的形状
     *
     * @param axis  轴
     * @param shape 形状
     *
     * @return 镜像后的形状
     * @apiNote 仅应用于方块形状初始化！
     */
    public static VoxelShape mirror(Direction.Axis axis, VoxelShape shape) {
        List<AABB> shapes = shape.toAabbs();
        AABB[] result = new AABB[shapes.size()];
        for (int i = 0; i < shapes.size(); i++) {
            AABB unmirrored = shapes.get(i);
            unmirrored = new AABB(unmirrored.getMinPosition().scale(16), unmirrored.getMaxPosition().scale(16));
            result[i] = ShapeUtil.mirror(axis, unmirrored);
        }
        return ShapeUtil.merge(result);
    }

    /**
     * 镜像指定的若干碰撞箱
     *
     * @param axis   轴
     * @param shapes 16x长度的碰撞箱
     *
     * @return 镜像后的16x长度碰撞箱
     * @apiNote 仅应用于方块形状初始化！
     */
    public static AABB[] mirror(Direction.Axis axis, AABB... shapes) {
        AABB[] result = new AABB[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            result[i] = ShapeUtil.mirror(axis, shapes[i]);
        }
        return result;
    }

    /**
     * 镜像指定的碰撞箱
     *
     * @param axis  轴
     * @param shape 16x长度的碰撞箱
     *
     * @return 镜像后的16x长度碰撞箱
     * @apiNote 仅应用于方块形状初始化！
     */
    public static AABB mirror(Direction.Axis axis, AABB shape) {
        double min = 16 - shape.max(axis);
        double max = 16 - shape.min(axis);
        return switch (axis) {
            case X -> new AABB(min, shape.minY, shape.minZ, max, shape.maxY, shape.maxZ);
            case Y -> new AABB(shape.minX, min, shape.minZ, shape.maxX, max, shape.maxZ);
            case Z -> new AABB(shape.minX, shape.minY, min, shape.maxX, shape.maxY, max);
        };
    }
}
