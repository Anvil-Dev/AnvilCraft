package dev.dubhe.anvilcraft.event.anvil;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.ImpactPileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AnvilHitImpactPileEventListener {
    /**
     * 侦听铁砧击中方块放置器事件
     *
     * @param event 铁砧落地事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onLand(@NotNull AnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos().below();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (level.getBlockState(pos).getBlock() instanceof ImpactPileBlock impactPileBlock
                && event.getFallDistance() + 1 >= 20
                && (level.getMinBuildHeight() <= pos.getY()
                    && pos.getY() <= level.getMinBuildHeight() + 4)
                && (level.getBlockState(pos.below()).is(Blocks.DEEPSLATE)
                    || level.getBlockState(pos.below()).is(Blocks.BEDROCK))
        )
            impactPileBlock.impact(level, pos);
    }
}
