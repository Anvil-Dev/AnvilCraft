package dev.dubhe.anvilcraft.api.hammer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IHasHammerEffect {
    boolean shouldRender();

    boolean shouldSkipRebuildBlock();

    BlockPos renderingBlockPos();

    BlockState renderingBlockState();

    RenderType renderType();
}
