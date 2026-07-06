package dev.dubhe.anvilcraft.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IBrokenCrystalsCluster {
    default boolean isFullyGrown(Level level, BlockPos pos, BlockState state) {
        return true;
    }
}
