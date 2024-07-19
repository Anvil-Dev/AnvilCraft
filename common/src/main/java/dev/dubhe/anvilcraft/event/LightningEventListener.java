package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager.Entry;
import dev.dubhe.anvilcraft.api.event.entity.LightningStrikeEvent;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class LightningEventListener {
    /**
     * 侦听雷击事件
     *
     * @param event 雷击事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onLightingStrike(@NotNull LightningStrikeEvent event) {
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        lightningCharge(pos, event.getLevel(), state);
        if (state.is(Blocks.LIGHTNING_ROD)) pos = pos.below();
        int depth = AnvilCraft.config.lightningStrikeDepth;
        int radius = AnvilCraft.config.lightningStrikeRadius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y < depth; y++) {
                    BlockPos offset = pos.offset(x, -y, z);
                    state = event.getLevel().getBlockState(offset);
                    if (
                            !state.is(Blocks.IRON_BLOCK)
                                    && !state.is(ModBlocks.FERRITE_CORE_MAGNET_BLOCK.get())
                                    && !state.is(ModBlocks.MAGNET_BLOCK.get())
                    ) continue;
                    BlockState state1 = ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState();
                    event.getLevel().setBlockAndUpdate(offset, state1);
                }
            }
        }
    }

    private void lightningCharge(BlockPos pos, Level level, BlockState state) {
        if (state.is(Blocks.COPPER_BLOCK) || state.is(Blocks.LIGHTNING_ROD)) {
            double unCharged = 32;
            Collection<Entry> nearestChargeCollect =
                    ChargeCollectorManager.getInstance(level).getNearestChargeCollect(pos);
            for (var floatChargeCollectorBlockEntityEntry : nearestChargeCollect) {
                ChargeCollectorBlockEntity blockEntity = floatChargeCollectorBlockEntityEntry.getBlockEntity();
                if (ChargeCollectorManager.getInstance(level).canCollect(blockEntity, pos)) {
                    unCharged = blockEntity.incomingCharge(unCharged);
                    if (unCharged <= 0) break;
                }
            }
        }
    }
}
