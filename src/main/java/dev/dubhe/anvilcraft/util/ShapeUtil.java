package dev.dubhe.anvilcraft.util;

import com.mojang.datafixers.util.Pair;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
     * 仅应用于方块碰撞箱初始化！
     *
     * @param shapes 子碰撞箱
     * @return 总碰撞箱
     */
    public static VoxelShape join(VoxelShape... shapes) {
        if (shapes.length == 0) return Shapes.empty();
        return Stream.of(shapes).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    }
}
