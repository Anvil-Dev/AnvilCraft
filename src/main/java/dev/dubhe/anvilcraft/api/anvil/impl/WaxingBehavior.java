package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WaxingBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        BlockPos belowPos = hitBlockPos.below();
        BlockState belowState = level.getBlockState(belowPos);
        HoneycombItem.getWaxed(belowState).ifPresent(state -> level.setBlockAndUpdate(belowPos, state));
        return false;
    }
}
