package dev.dubhe.anvilcraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 侦听tnt破坏方块事件方块
 */
public interface TntDestroyListenerBlock {
    void tntWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos blockPos,
        @NotNull BlockState blockState
        );
}
