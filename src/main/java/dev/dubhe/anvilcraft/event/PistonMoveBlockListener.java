package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PistonMoveBlockListener {

    @Unique
    private static final Map<Block, Double> CHARGE_NUMS = new HashMap<>();

    static {
        CHARGE_NUMS.put(Blocks.COPPER_BLOCK, 1d / 4);
        CHARGE_NUMS.put(Blocks.EXPOSED_COPPER, 1d / 8);
        CHARGE_NUMS.put(Blocks.WEATHERED_COPPER, 1d / 16);
    }

    /**
     * 活塞移动方块
     */
    public static void onPistonMoveBlocks(Level level, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            BlockState blockState = level.getBlockState(pos);
            if (!(blockState.getBlock() instanceof MagnetBlock)) continue;
            if (blockState.getValue(MagnetBlock.LIT)) continue;
            double n = getChargeNum(level, pos);
            if (n <= 0) {
                continue;
            }
            ChargeCollectorManager.charge(n, level, pos);
        }
    }

    private static Double getChargeNum(Level level, BlockPos pos) {
        double max = 0d;
        for (Direction face : Direction.values()) {
            Block block = level.getBlockState(pos.relative(face)).getBlock();
            if (!CHARGE_NUMS.containsKey(block)) continue;
            max = max < CHARGE_NUMS.get(block) ? CHARGE_NUMS.get(block) : max;
        }
        return max;
    }
}
