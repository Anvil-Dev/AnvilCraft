package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CorruptedBeaconRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public class CorruptedBeaconRenderer implements BlockEntityRenderer<CorruptedBeaconBlockEntity, CorruptedBeaconRenderState> {

    private static final float BEAM_BASE_Y = 0.5f;
    private static final float BEAM_INNER_HALF = 0.08f;
    private static final int BEAM_GLOW_LAYERS = 4;
    private static final float BEAM_GLOW_HALF_STEP = 0.02f;
    private static final float CORE_R = 0.008f;
    private static final float CORE_G = 0.0f;
    private static final float CORE_B = 0.018f;
    private static final float GLOW_R = 0.055f;
    private static final float GLOW_G = 0.004f;
    private static final float GLOW_B = 0.095f;

    public CorruptedBeaconRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public CorruptedBeaconRenderState createRenderState() {
        return new CorruptedBeaconRenderState();
    }

    @Override
    public void extractRenderState(
        CorruptedBeaconBlockEntity be,
        CorruptedBeaconRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = be.getBlockState();
        boolean lit = blockState.hasProperty(CorruptedBeaconBlock.LIT)
            && blockState.getValue(CorruptedBeaconBlock.LIT);
        state.setLit(lit);
        int beamTopY = be.getBeamHeight();
        int posY = be.getBlockPos().getY();
        state.setBeamHeight((float) (beamTopY - posY) - CorruptedBeaconRenderer.BEAM_BASE_Y);
    }

    @Override
    public void submit(
        CorruptedBeaconRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (!state.isLit()) return;
        float beamHeight = state.getBeamHeight();
        if (beamHeight <= 0.01f) return;
        collector.submitCustomGeometry(
            pose,
            ModRenderTypes.CORRUPTED_BEACON_BEAM,
            (last, consumer) -> CorruptedBeaconRenderer.emitBeaconBeam(consumer, last.pose(), beamHeight)
        );
    }

    public static void renderWeaponBeam(VertexConsumer consumer, Matrix4f matrix, float length) {
        for (int layer = CorruptedBeaconRenderer.BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = CorruptedBeaconRenderer.BEAM_INNER_HALF + CorruptedBeaconRenderer.BEAM_GLOW_HALF_STEP * layer * 0.5F;
            float falloff = 1.0F / (layer + 1);
            float alpha = 0.65F * falloff;
            CorruptedBeaconRenderer.emitBeamPyramid(
                consumer,
                matrix,
                0.0F,
                0.0F,
                0.0F,
                half,
                length,
                CorruptedBeaconRenderer.GLOW_R,
                CorruptedBeaconRenderer.GLOW_G,
                CorruptedBeaconRenderer.GLOW_B,
                alpha,
                0.24F * falloff
            );
        }
        CorruptedBeaconRenderer.emitBeamPyramid(
            consumer,
            matrix,
            0.0F,
            0.0F,
            0.0F,
            CorruptedBeaconRenderer.BEAM_INNER_HALF,
            length,
            CorruptedBeaconRenderer.CORE_R,
            CorruptedBeaconRenderer.CORE_G,
            CorruptedBeaconRenderer.CORE_B,
            0.94F,
            0.22F
        );
    }

    private static void emitBeaconBeam(VertexConsumer consumer, Matrix4f matrix, float beamHeight) {
        float apexY = CorruptedBeaconRenderer.BEAM_BASE_Y + beamHeight;
        for (int layer = CorruptedBeaconRenderer.BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = CorruptedBeaconRenderer.BEAM_INNER_HALF + CorruptedBeaconRenderer.BEAM_GLOW_HALF_STEP * layer;
            float falloff = 1.0f / (layer + 1);
            float alpha = 0.65f * falloff;
            float tipFade = 0.24f * falloff;
            CorruptedBeaconRenderer.emitBeamPyramid(
                consumer,
                matrix,
                half,
                apexY,
                CorruptedBeaconRenderer.GLOW_R,
                CorruptedBeaconRenderer.GLOW_G,
                CorruptedBeaconRenderer.GLOW_B,
                alpha,
                tipFade
            );
        }
        CorruptedBeaconRenderer.emitBeamPyramid(
            consumer,
            matrix,
            CorruptedBeaconRenderer.BEAM_INNER_HALF,
            apexY,
            CorruptedBeaconRenderer.CORE_R,
            CorruptedBeaconRenderer.CORE_G,
            CorruptedBeaconRenderer.CORE_B,
            0.94f,
            0.22f
        );
    }

    private static void emitBeamPyramid(
        VertexConsumer vc,
        Matrix4f matrix,
        float halfWidth,
        float apexY,
        float red,
        float green,
        float blue,
        float alpha,
        float tipFade
    ) {
        CorruptedBeaconRenderer.emitBeamPyramid(
            vc,
            matrix,
            0.5F,
            CorruptedBeaconRenderer.BEAM_BASE_Y,
            0.5F,
            halfWidth,
            apexY,
            red,
            green,
            blue,
            alpha,
            tipFade
        );
    }

    private static void emitBeamPyramid(
        VertexConsumer vc,
        Matrix4f matrix,
        float centerX,
        float baseY,
        float centerZ,
        float halfWidth,
        float apexY,
        float red,
        float green,
        float blue,
        float alpha,
        float tipFade
    ) {
        float cx = centerX;
        float cz = centerZ;
        float x0 = cx - halfWidth;
        float x1 = cx + halfWidth;
        float z0 = cz - halfWidth;
        float z1 = cz + halfWidth;
        float[][] corners = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
        float tipAlpha = alpha * tipFade;

        for (int i = 0; i < 4; i++) {
            float[] c0 = corners[i];
            float[] c1 = corners[(i + 1) % 4];
            CorruptedBeaconRenderer.beamVertex(vc, matrix, c0[0], baseY, c0[1], red, green, blue, alpha);
            CorruptedBeaconRenderer.beamVertex(vc, matrix, c1[0], baseY, c1[1], red, green, blue, alpha);
            CorruptedBeaconRenderer.beamVertex(vc, matrix, cx, apexY, cz, red, green, blue, tipAlpha);
        }
    }

    private static void beamVertex(
        VertexConsumer vc,
        Matrix4f matrix,
        float x,
        float y,
        float z,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        vc.addVertex(matrix, x, y, z)
            .setColor(red, green, blue, alpha);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(CorruptedBeaconBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        int topY = Math.max(blockEntity.getBeamHeight(), pos.getY() + 1);
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, topY, pos.getZ() + 1);
    }
}
