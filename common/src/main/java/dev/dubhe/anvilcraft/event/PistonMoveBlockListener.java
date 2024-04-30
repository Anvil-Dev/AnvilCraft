package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PistonMoveBlockListener {
    private static final Map<Block, Double> CHANCES = new HashMap<>();

    static {
        CHANCES.put(Blocks.COPPER_BLOCK, 1d / 4);
        CHANCES.put(Blocks.EXPOSED_COPPER, 1d / 8);
        CHANCES.put(Blocks.WEATHERED_COPPER, 1d / 16);
    }

    /**
     * 活塞移动方块
     */
    public static void onPistonMoveBlocks(@NotNull Level level, @NotNull List<BlockPos> blocks) {
        RandomSource random = level.random;
        for (BlockPos pos : blocks) {
            BlockState blockState = level.getBlockState(pos);
            if (!blockState.is(ModBlocks.MAGNET_BLOCK.get())) continue;
            if (blockState.getValue(MagnetBlock.LIT)) continue;
            double c = getChance(level, pos);
            double r = random.nextDouble();
            if (c > 0 && r < c) {
                Collection<Map.Entry<Float, ChargeCollectorBlockEntity>> nearestChargeCollect =
                    ChargeCollectorManager.getNearestChargeCollect(pos);
                for (var floatChargeCollectorBlockEntityEntry : nearestChargeCollect) {
                    ChargeCollectorBlockEntity blockEntity = floatChargeCollectorBlockEntityEntry.getValue();
                    if (ChargeCollectorManager.canCollect(blockEntity, pos)) {
                        int unCharged = blockEntity.incomingCharge(1);
                        if (unCharged == 0) {
                            break;
                        }

                    }
                }
            }
        }
    }

    private static Double getChance(Level level, BlockPos pos) {
        double max = 0d;
        for (Direction face : Direction.values()) {
            Block block = level.getBlockState(pos.relative(face)).getBlock();
            if (!CHANCES.containsKey(block)) continue;
            max = max < CHANCES.get(block) ? CHANCES.get(block) : max;
        }
        return max;
    }
}
