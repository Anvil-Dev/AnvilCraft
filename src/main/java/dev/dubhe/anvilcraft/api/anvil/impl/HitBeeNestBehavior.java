package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HitBeeNestBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos pos,
        BlockState state,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        if (!state.hasBlockEntity()) return false;
        int honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < BeehiveBlock.MAX_HONEY_LEVELS) return false;
        BlockPos posBelowHive = pos.below();
        int filled = CauldronUtil.fill(level, posBelowHive, ModBlocks.HONEY_CAULDRON.get(), 1, true);
        if (filled <= 0) return false;
        CauldronUtil.fill(level, posBelowHive, ModBlocks.HONEY_CAULDRON.get(), 1, false);
        level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 2));
        return true;
    }
}
