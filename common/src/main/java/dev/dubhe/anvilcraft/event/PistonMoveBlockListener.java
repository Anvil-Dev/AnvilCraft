package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PistonMoveBlockListener {
    /**
     * 活塞移动方块
     */
    public static void onPistonMoveBlocks(@NotNull Level level, @NotNull List<BlockPos> blocks) {
        RandomSource random = level.random;
        for (BlockPos pos : blocks) {
            if (!level.getBlockState(pos).is(ModBlocks.MAGNET_BLOCK.get())) continue;
            if (level.getBlockState(pos).is(ModBlocks.MAGNET_BLOCK.get())
                && level.getBlockState(pos).getValue(BlockStateProperties.LIT)) continue;
            int b = isNearbyCopperBlock(level, pos);
            double r = random.nextDouble();
            double p;
            if (b == 0) {
                p = 0;
            } else {
                p = Math.pow(0.5, b + 1);
            }
            System.out.print(p + "|b=" + b);
            if (r < p) {
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

    private static int isNearbyCopperBlock(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            if (level.getBlockState(pos.relative(face)).is(Blocks.COPPER_BLOCK)) {
                return 1;
            } else if (level.getBlockState(pos.relative(face)).is(Blocks.EXPOSED_COPPER)) {
                return 2;
            } else if (level.getBlockState(pos.relative(face)).is(Blocks.WEATHERED_COPPER)) {
                return 3;
            } else if (level.getBlockState(pos.relative(face)).is(Blocks.OXIDIZED_COPPER)) {
                return 0;
            }
        }
        return 0;
    }
}
