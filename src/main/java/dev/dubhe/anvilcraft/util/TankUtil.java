package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

public class TankUtil {
    /**
     * 递归检测当前位置的结构是否符合门格海绵结构
     * 门格海绵结构规则：将结构分为3x3x3的块后，
     * 角块和边块应该是门格海绵方块，面块和中心块应该是空气
     *
     * @param level 世界对象
     * @param centerPos   检测起始位置
     * @param size  结构大小（必须是3的倍数）
     * @return 是否符合门格海绵结构
     */
    public static boolean isMengerStructure(Level level, BlockPos centerPos, int size) {
        if (size <= 0) return false;

        // 基础情况：如果size为1，则直接检查是否为门格海绵
        if (size == 1) {
            return level.getBlockState(centerPos).is(ModBlocks.MENGER_SPONGE.get());
        }

        if (size % 3 != 0) return false;
        // 递归情况：将大结构分解为27个小结构进行检查
        int subSize = size / 3;

        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    BlockPos subPos = centerPos.offset(x * subSize, y * subSize, z * subSize);

                    if (isMengerPos(x, y, z)) {
                        // 角块和边块应该符合门格海绵结构
                        if (!isMengerStructure(level, subPos, subSize)) {
                            return false;
                        }
                    } else {
                        // 面块和中心块应该是空气
                        if (!isNoMengerSponge(level, subPos, subSize)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public static boolean isNoMengerSponge(Level level, BlockPos centerPos, int size) {
        if (size <= 0) return false;
        if (size % 2 == 0) return false;
        int delta = size / 2;
        List<BlockPos> blockPosList = BlockPos.betweenClosedStream(
                centerPos.getX() - delta,
                centerPos.getY() - delta,
                centerPos.getZ() - delta,
                centerPos.getX() + delta,
                centerPos.getY() + delta,
                centerPos.getZ() + delta
            )
            .map(BlockPos::immutable)
            .toList();
        for (BlockPos subPos : blockPosList) {
            if (level.getBlockState(subPos).is(ModBlocks.MENGER_SPONGE.get())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断在3x3x3结构中的位置是否为角块或边块
     */
    public static boolean isMengerPos(int x, int y, int z) {
        int sideCount = 0;
        if (x != 0) sideCount++;
        if (y != 0) sideCount++;
        if (z != 0) sideCount++;

        // 角块：至少两个坐标是-1或1
        // 边块：恰好一个坐标是-1或1
        return sideCount >= 2;
    }
}