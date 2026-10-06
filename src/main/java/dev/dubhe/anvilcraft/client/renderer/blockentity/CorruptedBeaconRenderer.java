package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CorruptedBeaconRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CorruptedBeaconRenderer implements BlockEntityRenderer<CorruptedBeaconBlockEntity, CorruptedBeaconRenderState> {

    private static final int GLASS_COLOR = 0x4C6D01CE;
    private static final float BEAM_BASE_Y = 0.5f;
    private static final float BEAM_INNER_HALF = 0.08f;
    private static final int BEAM_GLOW_LAYERS = 4;
    private static final float BEAM_GLOW_HALF_STEP = 0.06f;
    private static final float BEAM_R = 0.02f;
    private static final float BEAM_G = 0.0f;
    private static final float BEAM_B = 0.05f;

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
        List<BakedQuad> quads = new ArrayList<>();
        var client = Minecraft.getInstance();
        if (client.level != null && be.getLevel() != null) {
            BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
            var model = client.getModelManager().getBlockStateModelSet().get(white);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(client.level, be.getBlockPos(), white, client.level.getRandom(), parts);
            for (BlockStateModelPart part : parts) {
                for (Direction direction : Direction.values()) quads.addAll(part.getQuads(direction));
            }
        }
        state.setGlassQuads(List.copyOf(quads));
        BlockState blockState = be.getBlockState();
        boolean lit = be.getLevel() != null && blockState.hasProperty(CorruptedBeaconBlock.LIT)
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
        var quads = state.getGlassQuads();
        int light = state.lightCoords;
        if (!quads.isEmpty()) {
            pose.pushPose();
            pose.translate(0.005f, 0.005f, 0.005f);
            pose.scale(0.99f, 0.99f, 0.99f);
            collector.submitCustomGeometry(pose, ModRenderTypes.BEACON_GLASS, (last, consumer) -> {
                QuadInstance instance = new QuadInstance();
                instance.setColor(GLASS_COLOR);
                instance.setLightCoords(light);
                quads.forEach(quad -> consumer.putBakedQuad(last, quad, instance));
            });
            pose.popPose();
        }
        if (!state.isLit()) return;
        float beamHeight = state.getBeamHeight();
        if (beamHeight <= 0.5f) return;
        collector.submitCustomGeometry(
            pose,
            ModRenderTypes.CORRUPTED_BEACON_BEAM_CORE,
            (last, consumer) -> CorruptedBeaconRenderer.emitBeamPyramid(
                consumer, last.pose(), CorruptedBeaconRenderer.BEAM_INNER_HALF,
                CorruptedBeaconRenderer.BEAM_BASE_Y + beamHeight,
                CorruptedBeaconRenderer.BEAM_R, CorruptedBeaconRenderer.BEAM_G, CorruptedBeaconRenderer.BEAM_B, 1.0f, 1.0f)
        );
        collector.submitCustomGeometry(
            pose,
            ModRenderTypes.CORRUPTED_BEACON_BEAM,
            (last, consumer) -> CorruptedBeaconRenderer.renderBeamGlow(
                consumer, last.pose(), 0.5f, CorruptedBeaconRenderer.BEAM_BASE_Y, 0.5f, beamHeight, 1.0f)
        );
    }

    public static void renderWeaponBeam(VertexConsumer consumer, Matrix4f matrix, float length) {
        CorruptedBeaconRenderer.renderBeam(consumer, matrix, length, 0.5f);
    }

    public static void renderBeam(VertexConsumer consumer, Matrix4f matrix, float length, float glowSpreadScale) {
        CorruptedBeaconRenderer.renderBeamGlow(consumer, matrix, 0.0f, 0.0f, 0.0f, length, glowSpreadScale);
        CorruptedBeaconRenderer.emitBeamPyramid(
            consumer, matrix, 0.0f, 0.0f, 0.0f, CorruptedBeaconRenderer.BEAM_INNER_HALF, length,
            CorruptedBeaconRenderer.BEAM_R, CorruptedBeaconRenderer.BEAM_G, CorruptedBeaconRenderer.BEAM_B, 0.82f, 0.25f
        );
    }

    private static void renderBeamGlow(
        VertexConsumer consumer,
        Matrix4f matrix,
        float centerX,
        float baseY,
        float centerZ,
        float length,
        float glowSpreadScale
    ) {
        float apexY = baseY + length;
        for (int layer = CorruptedBeaconRenderer.BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = CorruptedBeaconRenderer.BEAM_INNER_HALF
                + CorruptedBeaconRenderer.BEAM_GLOW_HALF_STEP * layer * glowSpreadScale;
            float falloff = 1.0f / (layer + 1);
            falloff *= falloff;
            CorruptedBeaconRenderer.emitBeamPyramid(
                consumer, matrix, centerX, baseY, centerZ, half, apexY,
                CorruptedBeaconRenderer.BEAM_R, CorruptedBeaconRenderer.BEAM_G, CorruptedBeaconRenderer.BEAM_B,
                0.45f * falloff, 0.3f * falloff
            );
        }
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
