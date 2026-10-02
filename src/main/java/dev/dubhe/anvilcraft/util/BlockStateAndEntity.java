package dev.dubhe.anvilcraft.util;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record BlockStateAndEntity(BlockState state, @Nullable BlockEntity be) {
    public BlockStateAndEntity(BlockState state) {
        this(state, null);
    }
}
