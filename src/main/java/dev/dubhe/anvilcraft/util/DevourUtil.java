package dev.dubhe.anvilcraft.util;

import com.google.common.collect.Streams;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * @param chainCount 连锁数量,0以禁用连锁
     *
     */
    public static List<BlockPos> getDevourPosList(
        ServerLevel level,
        BlockPos centerPos,
        Direction devourDirection,
        int range,
        int chainCount) {
        // a, b: bottom corners
        BlockPos a;
        BlockPos b;
        switch (devourDirection) {
            case DOWN, UP -> {
                a = centerPos.relative(Direction.NORTH, range).relative(Direction.WEST, range);
                b = centerPos.relative(Direction.SOUTH, range).relative(Direction.EAST, range);
            }
            case NORTH, SOUTH -> {
                a = centerPos.relative(Direction.DOWN, range).relative(Direction.WEST, range);
                b = centerPos.relative(Direction.DOWN, range).relative(Direction.EAST, range);
            }
            case WEST, EAST -> {
                a = centerPos.relative(Direction.DOWN, range).relative(Direction.NORTH, range);
                b = centerPos.relative(Direction.DOWN, range).relative(Direction.SOUTH, range);
            }
            default -> {
                a = centerPos;
                b = centerPos;
            }
        }

        Set<BlockPos> normalizedOriginalPoses = new HashSet<>();
        // BlockPos.betweenClosed: down -> up
        Set<BlockPos> devourTargets = Streams
            .stream(BlockPos.betweenClosed(a, b))
            .flatMap(bottomPos -> {
                // deltaHeight = 0 when DOWN, UP; above topY is chain range
                int deltaHeight = devourDirection.getStepY() != 0 ? 0 : 2 * range;
                int topY = bottomPos.getY() + deltaHeight;
                return Streams
                    .stream(BlockPos.betweenClosed(bottomPos, bottomPos.atY(topY + chainCount)))
                    .takeWhile(pos -> pos.getY() <= topY || DevourUtil.shouldChainDevour(level.getBlockState(pos)))
                    .map(BlockPos::immutable);
                // in common devour OR chain until unchainable
            })
            .map(originalPos -> {
                BlockPos normalizedBlockPos = MultiPartBlockUtil.getChainableMainPartPos(level, originalPos);
                if (!originalPos.equals(normalizedBlockPos)) {
                    normalizedOriginalPoses.add(originalPos);
                    return normalizedBlockPos;
                }
                return originalPos;
            })
            .collect(Collectors.toSet());

        LinkedList<BlockPos> l = new LinkedList<>();
        // include original pos, not process twice
        DevouringLevelReader devouringLevelReader = new DevouringLevelReader(level, devourTargets, normalizedOriginalPoses);

        for (BlockPos devourBlockPos : devourTargets) {
            BlockState devourState = level.getBlockState(devourBlockPos);
            if (devourState.isAir()) continue;

            if (!devourState.canSurvive(devouringLevelReader, devourBlockPos)) {
                l.addFirst(devourBlockPos);
            } else {
                l.addLast(devourBlockPos);
            }
        }
        return l;
    }

    public static boolean shouldDevour(BlockState devourState) {
        return !devourState.isAir() && DevourUtil.canDevour(devourState);
    }

    private static boolean shouldChainDevour(BlockState devourState) {
        return devourState.is(ModBlockTags.BLOCK_DEVOURER_CHAIN_DEVOURING) && DevourUtil.shouldDevour(devourState);
    }
}
