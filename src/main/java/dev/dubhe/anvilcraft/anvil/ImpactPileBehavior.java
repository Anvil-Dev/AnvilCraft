package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.ImpactPileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ImpactPileBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        BlockState belowState = level.getBlockState(hitBlockPos.below());
        if (
            event.getFallDistance() + 1 >= 20
                && (level.getMinBuildHeight() <= hitBlockPos.getY() && hitBlockPos.getY() <= level.getMinBuildHeight() + 8)
                && (belowState.is(Blocks.DEEPSLATE) || belowState.is(Blocks.BEDROCK))
        ) {
            ImpactPileBlock.impact(level, hitBlockPos);
        }
        return true;
    }
}
