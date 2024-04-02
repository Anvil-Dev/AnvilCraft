package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.LightningStrikeEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class LightningEventListener {
    @SubscribeEvent
    public void onLightingStrike(@NotNull LightningStrikeEvent event) {
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        if (state.is(Blocks.LIGHTNING_ROD)) pos = pos.below();
        int Depth = AnvilCraft.config.lightningStrikeDepth;
        for (int i = 0; i < Depth; i++) {
            state = event.getLevel().getBlockState(pos);
            if (state.is(Blocks.IRON_BLOCK)) {
                BlockState state1 = ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState();
                event.getLevel().setBlockAndUpdate(pos, state1);
            }
            pos = pos.below();
        }
    }
}
