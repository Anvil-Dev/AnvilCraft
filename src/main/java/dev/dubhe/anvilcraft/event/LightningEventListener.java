package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager.Entry;
import dev.dubhe.anvilcraft.api.event.LightningBoltStrikeEvent;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Collection;

import static dev.dubhe.anvilcraft.block.MagnetBlock.LIT;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LightningEventListener {
    @SubscribeEvent
    public static void onLightingStrike(LightningBoltStrikeEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        lightningCharge(pos, level, state);
        if (state.is(Blocks.LIGHTNING_ROD)) pos = pos.below();
        int depth = AnvilCraft.CONFIG.lightningStrikeDepth;
        int radius = AnvilCraft.CONFIG.lightningStrikeRadius;
        for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(radius, 0, radius), pos.offset(-radius, -depth, -radius))) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.is(Blocks.IRON_BLOCK)) {
                BlockState blockState1 = ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState();
                if (blockState1.hasProperty(LIT)) {
                    blockState1 = blockState1.setValue(LIT, level.hasNeighborSignal(blockPos));
                }
                level.setBlockAndUpdate(blockPos, blockState1);
            }
        }
    }

    private static void lightningCharge(BlockPos pos, Level level, BlockState state) {
        if (state.is(Blocks.COPPER_BLOCK) || state.is(Blocks.LIGHTNING_ROD)) {
            double unCharged = 32;
            Collection<Entry> nearestChargeCollect =
                ChargeCollectorManager.getInstance(level).getNearestChargeCollect(pos);
            for (var floatChargeCollectorBlockEntityEntry : nearestChargeCollect) {
                ChargeCollectorBlockEntity blockEntity = floatChargeCollectorBlockEntityEntry.getBlockEntity();
                if (ChargeCollectorManager.getInstance(level).canCollect(blockEntity, pos)) {
                    unCharged = blockEntity.incomingCharge(unCharged, pos);
                    if (unCharged <= 0) break;
                }
            }
        }
    }
}
