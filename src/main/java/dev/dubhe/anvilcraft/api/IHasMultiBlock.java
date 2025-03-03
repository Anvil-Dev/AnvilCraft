package dev.dubhe.anvilcraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 多方块方块
 */
public interface IHasMultiBlock {

    void onRemove(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state);

    void onPlace(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state);
}
