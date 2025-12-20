package dev.dubhe.anvilcraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 多方块方块
 */
public interface IHasMultiBlock {

    void onRemove(Level level, BlockPos pos, BlockState state);

    void onPlace(Level level, BlockPos pos, BlockState state);
}
