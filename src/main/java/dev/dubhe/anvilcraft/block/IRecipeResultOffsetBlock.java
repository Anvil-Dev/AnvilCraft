package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface IRecipeResultOffsetBlock {
    Vec3 getOffset(Level level, BlockPos pos, BlockState state);
}
