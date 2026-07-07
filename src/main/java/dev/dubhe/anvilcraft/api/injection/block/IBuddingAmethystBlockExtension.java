package dev.dubhe.anvilcraft.api.injection.block;

import dev.dubhe.anvilcraft.api.block.IBrokenCrystalsBudding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;

public interface IBuddingAmethystBlockExtension extends IBrokenCrystalsBudding {
    @Override
    default void anvilcraft$tryGrowBuds(Level level, BlockPos pos, BlockState state) {
        throw new AssertionError("Not implemented!");
    }

    @Override
    default void anvilcraft$tryBreakClusters(Level level, BlockPos pos, BlockState state, BiConsumer<BlockPos, BlockState> breaker) {
        throw new AssertionError("Not implemented!");
    }
}
