package dev.dubhe.anvilcraft.api.rendering;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class FullBrightNoOpLighter extends BlockModelLighter {

    @Override
    public void prepareQuadAmbientOcclusion(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos centerPosition,
        BakedQuad quad,
        QuadInstance outputInstance
    ) {
        outputInstance.setColor(-1);
    }

    @Override
    protected void prepareQuadShape(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        BakedQuad quad,
        boolean ambientOcclusion
    ) {
    }

    @Override
    public void prepareQuadFlat(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        int lightCoords,
        BakedQuad quad,
        QuadInstance outputInstance
    ) {
    }

    @Override
    public int getLightCoords(BlockState state, BlockAndTintGetter level, BlockPos relativePos) {
        return 15728880;
    }
}
