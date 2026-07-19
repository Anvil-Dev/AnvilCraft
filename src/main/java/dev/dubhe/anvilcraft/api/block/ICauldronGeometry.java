package dev.dubhe.anvilcraft.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Describes the interior and supporting blocks of a non-standard cauldron.
 */
public interface ICauldronGeometry {
    AABB getCauldronInnerArea(BlockPos pos, BlockState state);

    List<BlockPos> getCauldronBottomPositions(BlockPos pos, BlockState state);
}
