package dev.dubhe.anvilcraft.util;

import com.google.common.collect.Streams;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DevourUtil {
    /**
     * 检查目标位置是否可以破坏
     *
     * @param devourState 目标方块
     *
     */
    public static boolean canDevour(BlockState devourState) {
        return !devourState.is(ModBlockTags.DEVOUR_BLACKLIST) && devourState.getBlock().defaultDestroyTime() >= 0;
    }

    /**
     * 获取吞噬列表,已经过修正
     *
     * @param level 世界
     * @param centerPos 中心坐标
     * @param devourDirection 吞噬方向
     * @param range 吞噬范围,从中心扩展格数
     * @param chainCount 连锁数量,非正值以禁用连锁
     *
     */
    public static List<BlockPos> getDevourPosList(
        ServerLevel level,
        BlockPos centerPos,
        Direction devourDirection,
        int range,
        int chainCount) {
        Iterable<BlockPos> devourPoses;
        Iterable<BlockPos> topPoses;
        switch (devourDirection) {
            case DOWN, UP -> {
                devourPoses = BlockPos.betweenClosed(
                    centerPos.relative(Direction.NORTH, range).relative(Direction.WEST, range),
                    centerPos.relative(Direction.SOUTH, range).relative(Direction.EAST, range)
                );
                topPoses = devourPoses;
            }
            case NORTH, SOUTH -> {
                devourPoses = BlockPos.betweenClosed(
                    centerPos.relative(Direction.UP, range).relative(Direction.WEST, range),
                    centerPos.relative(Direction.DOWN, range).relative(Direction.EAST, range)
                );
                topPoses = BlockPos.betweenClosed(
                    centerPos.relative(Direction.UP, range).relative(Direction.WEST, range),
                    centerPos.relative(Direction.UP, range).relative(Direction.EAST, range)
                );
            }
            case WEST, EAST -> {
                devourPoses = BlockPos.betweenClosed(
                    centerPos.relative(Direction.UP, range).relative(Direction.NORTH, range),
                    centerPos.relative(Direction.DOWN, range).relative(Direction.SOUTH, range)
                );
                topPoses = BlockPos.betweenClosed(
                    centerPos.relative(Direction.UP, range).relative(Direction.NORTH, range),
                    centerPos.relative(Direction.UP, range).relative(Direction.SOUTH, range)
                );
            }
            default -> {
                devourPoses = List.of(centerPos);
                topPoses = devourPoses;
            }
        }
        Stream<BlockPos> devuorStream;
        if (chainCount > 0) {
            Stream<BlockPos> chainStream = Streams
                .stream(topPoses)
                .map(pos -> BlockPos.betweenClosed(pos.above(), pos.above(chainCount)))
                .flatMap(Streams::stream)
                .filter(pos -> level.getBlockState(pos).is(ModBlockTags.BLOCK_DEVOURER_CHAIN_DEVOURING));
            devuorStream = Streams.concat(Streams.stream(devourPoses), chainStream);
        } else {
            devuorStream = Streams.stream(devourPoses);
        }
        Set<BlockPos> devourTargets = devuorStream
            .map(BlockPos::immutable)
            .collect(Collectors.toSet());
        List<BlockPos> first = new ArrayList<>(devourTargets.size());
        List<BlockPos> second = new ArrayList<>(devourTargets.size());
        DevouringLevelReader devouringLevelReader = new DevouringLevelReader(level, devourTargets);

        for (BlockPos devourBlockPos : devourTargets) {
            BlockState devourState = level.getBlockState(devourBlockPos);
            if (!DevourUtil.shouldDevour(devourState)) continue;
            BlockPos normalizedBlockPos = MultiPartBlockUtil.getChainableMainPartPos(level, devourBlockPos);
            if (normalizedBlockPos != devourBlockPos) {
                devourBlockPos = normalizedBlockPos;
                devourState = level.getBlockState(normalizedBlockPos);
            }
            if (!devourState.canSurvive(devouringLevelReader, devourBlockPos)) {
                first.add(devourBlockPos);
            } else {
                second.add(devourBlockPos);
            }
        }
        first.addAll(second);
        return first;
    }

    private static boolean shouldDevour(BlockState devourState) {
        return !devourState.isAir() && DevourUtil.canDevour(devourState);
    }
}
