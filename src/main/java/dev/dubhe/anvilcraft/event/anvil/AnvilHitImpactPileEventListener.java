package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.ImpactPileBlock;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilHitImpactPileEventListener {
    /**
     * 侦听铁砧击中方块放置器事件
     *
     * @param event 铁砧落地事件
     */
    @SubscribeEvent
    public static void onLand(@NotNull AnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos().below();
        BlockState state = level.getBlockState(pos);
        BlockState belowState = level.getBlockState(pos.below());
        Block block = state.getBlock();
        if (level.getBlockState(pos).is(ModBlocks.IMPACT_PILE)
            && event.getFallDistance() + 1 >= 20
            && (level.getMinBuildHeight() <= pos.getY() && pos.getY() <= level.getMinBuildHeight() + 8)
            && (belowState.is(Blocks.DEEPSLATE) || belowState.is(Blocks.BEDROCK))) {
            ImpactPileBlock.impact(level, pos);
        }
    }
}
