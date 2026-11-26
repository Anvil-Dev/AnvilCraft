package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.QuadInstance;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LitBlockRenderState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

final class MountedWorldModel extends LitBlockRenderState {
    static ModelBlockRenderer renderer(boolean ambientOcclusion, BlockColors colors) {
        var renderer = new ModelBlockRenderer(ambientOcclusion, true, colors);
        renderer.lighter = new FaceLighting(renderer.lighter);
        return renderer;
    }

    static MountedWorldModel capture(ModelBlockRenderer renderer, BlockAndTintGetter view, BlockPos pos, BlockState state,
                                     BlockStateModel model, boolean cutoutLeaves) {
        var output = new MountedWorldModel();
        renderer.tesselateBlock(ModelBlockRenderer.forceOpaque(cutoutLeaves, state)
                ? (x, y, z, quad, lighting) -> output.addQuad(ChunkSectionLayer.SOLID, x, y, z, quad, lighting)
                : output::addQuad,
            0, 0, 0, view, pos, state, model, state.getSeed(pos));
        return output;
    }

    /** 只有采光需要查询周围真实方块的遮挡，面剔除仍使用安装物的部件。 */
    private static final class FaceLighting extends BlockModelLighter {
        private final BlockModelLighter delegate;

        private FaceLighting(BlockModelLighter delegate) {
            this.delegate = delegate;
        }

        private static BlockAndTintGetter view(BlockAndTintGetter level, BakedQuad quad) {
            return level instanceof MountedBlockView mounted ? mounted.forLighting(quad.materialInfo().shade()) : level;
        }

        @Override
        public void reset() {
            this.delegate.reset();
        }

        @Override
        public int getLightCoords(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return this.delegate.getLightCoords(state, level, pos);
        }

        @Override
        public void prepareQuadFlat(BlockAndTintGetter level, BlockState state, BlockPos pos, int light,
                                    BakedQuad quad, QuadInstance output) {
            this.delegate.prepareQuadFlat(view(level, quad), state, pos, light, quad, output);
        }

        @Override
        public void prepareQuadAmbientOcclusion(BlockAndTintGetter level, BlockState state, BlockPos pos,
                                                BakedQuad quad, QuadInstance output) {
            this.delegate.prepareQuadAmbientOcclusion(view(level, quad), state, pos, quad, output);
        }
    }
}
