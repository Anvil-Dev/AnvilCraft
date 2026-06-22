package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.event.LightningBoltStrikeEvent;
import dev.dubhe.anvilcraft.api.event.TeslaStrikeEvent;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import static dev.dubhe.anvilcraft.block.MagnetBlock.LIT;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LightningEventListener {
    @SubscribeEvent
    public static void onLightningStrike(LightningBoltStrikeEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        LightningEventListener.strikeOnLightningRod(level, pos, level.getBlockState(pos));
    }

    @SubscribeEvent
    public static void onTeslaStrike(TeslaStrikeEvent.TargetBlock event) {
        LightningEventListener.strikeOnLightningRod(event.getLevel(), event.getTargetPos(), event.getTargetState());
    }

    private static void strikeOnLightningRod(Level level, BlockPos targetPos, BlockState targetState) {
        LightningEventListener.lightningCharge(targetPos, level, targetState);
        if (targetState.is(Blocks.LIGHTNING_ROD)) targetPos = targetPos.below();
        int depth = AnvilCraft.CONFIG.lightningStrikeDepth;
        int radius = AnvilCraft.CONFIG.lightningStrikeRadius;
        for (BlockPos blockPos : BlockPos.betweenClosed(targetPos.offset(radius, 0, radius), targetPos.offset(-radius, -depth, -radius))) {
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
            ChargeCollectorManager.charge(32, level, pos);
        }
    }
}
