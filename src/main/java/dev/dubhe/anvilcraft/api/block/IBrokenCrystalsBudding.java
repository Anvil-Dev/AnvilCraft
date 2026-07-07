package dev.dubhe.anvilcraft.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;

public interface IBrokenCrystalsBudding {
    void anvilcraft$tryGrowBuds(Level level, BlockPos pos, BlockState state);

    void anvilcraft$tryBreakClusters(Level level, BlockPos pos, BlockState state, BiConsumer<BlockPos, BlockState> breaker);
}
