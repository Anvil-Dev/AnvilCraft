package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PistonMoveBlockListener {
    public static void onPistonMoveBlocks(Level level, List<BlockPos> blocks) {
        RandomSource random = level.random;
        for (BlockPos pos : blocks) {
            if (!level.getBlockState(pos).is(ModBlocks.MAGNET_BLOCK.get())) continue;
            boolean b = isNearbyCopperBlock(level, pos);
            double r = random.nextDouble();
            if (b && r < 0.25) {
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

    private static boolean isNearbyCopperBlock(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            if (level.getBlockState(pos.relative(face)).is(Blocks.COPPER_BLOCK)) {
                return true;
            }
        }
        return false;
    }
}
