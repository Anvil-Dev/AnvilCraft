package dev.dubhe.anvilcraft.client.renderer.gui.pip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.gui.state.RotatedBlockRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class RotatedBlockRenderer extends PictureInPictureRenderer<RotatedBlockRenderState> {
    protected RotatedBlockRenderer(MultiBufferSource.BufferSource source) {
        super(source);
    }

    @Override
    public Class<RotatedBlockRenderState> getRenderStateClass() {
        return RotatedBlockRenderState.class;
    }

    @Override
    protected void renderToTexture(RotatedBlockRenderState state, PoseStack pose) {
        pose.pushPose();
        pose.translate(-7, 7, 0);
        pose.translate(state.translation().x, state.translation().y, state.translation().z);
        pose.scale(state.scale(), state.scale(), state.scale());
        pose.mulPose(new Matrix4f().scaling(1, -1, 1));
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(state.rotation());
        if (state.overrideCameraAngle() != null) pose.mulPose(state.overrideCameraAngle());
        pose.translate(-0.5F, -0.5F, -0.5F);

        BlockQuadOutput output = (x, y, z, quad, instance) -> putBakedQuad(
            pose,
            this.bufferSource,
            x,
            y,
            z,
            quad,
            instance,
            quad.materialInfo().layer()
        );
        BlockQuadOutput solidOutput = (x, y, z, quad, instance) -> putBakedQuad(
            pose,
            this.bufferSource,
            x,
            y,
            z,
            quad,
            instance,
            ChunkSectionLayer.SOLID
        );
        Minecraft minecraft = Minecraft.getInstance();
        boolean ambientOcclusion = minecraft.options.ambientOcclusion().get();
        boolean cutoutLeaves = minecraft.options.cutoutLeaves().get();
        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(ambientOcclusion, false, minecraft.getBlockColors());

        MovingBlockRenderState moving = state.state();
        BlockState blockState = moving.blockState;
        BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(blockState);
        BlockQuadOutput blockOutput = ModelBlockRenderer.forceOpaque(cutoutLeaves, blockState) ? solidOutput : output;
        long blockSeed = blockState.getSeed(moving.randomSeedPos);
        blockRenderer.tesselateBlock(
            blockOutput,
            0.0F,
            0.0F,
            0.0F,
            moving,
            moving.blockPos,
            blockState,
            model,
            blockSeed
        );
        pose.popPose();
    }

    @Override
    protected String getTextureLabel() {
        return "rotated_block";
    }

    private static void putBakedQuad(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        float x,
        float y,
        float z,
        BakedQuad quad,
        QuadInstance instance,
        ChunkSectionLayer layer
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);

        VertexConsumer buffer = bufferSource.getBuffer(switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        });
        buffer.putBakedQuad(poseStack.last(), quad, instance);
        poseStack.popPose();
    }
}
