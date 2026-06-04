package dev.dubhe.anvilcraft.api.injection.block.state;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockStateExtension {
    private BlockState self() {
        return (BlockState) this;
    }

    /// Determines if this block can stick to another block when pushed by a piston.
    ///
    /// @param pos      My pos
    /// @param otherPos Other pos
    /// @param other    Other state
    /// @return True to link blocks
    default boolean anvilcraft$canStickTo(BlockPos pos, BlockPos otherPos, BlockState other) {
        return this.self().getBlock().anvilcraft$canStickTo(pos, this.self(), otherPos, other);
    }
}
