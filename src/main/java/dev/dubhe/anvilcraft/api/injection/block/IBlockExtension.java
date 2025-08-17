package dev.dubhe.anvilcraft.api.injection.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockExtension {
    private Block self() {
        return (Block) this;
    }

    /**
     * Determines if this block can stick to another block when pushed by a piston.
     *
     * @param pos      My pos
     * @param state    My state
     * @param otherPos Other pos
     * @param other    Other state
     * @return True to link blocks
     */
    default boolean canStickTo(BlockPos pos, BlockState state, BlockPos otherPos, BlockState other) {
        return self().canStickTo(state, other);
    }
}
