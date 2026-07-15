package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CorruptedBeaconRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final Map<BlockPos, BeamRenderData> DEFERRED_BEAMS = new LinkedHashMap<>();
    private static final List<WeaponBeamRenderData> DEFERRED_WEAPON_BEAMS = new ArrayList<>();

    private record BeamRenderData(BlockPos pos, float beamHeight) {
    }

    private record WeaponBeamRenderData(Vec3 start, Vec3 end, @Nullable Matrix4f viewBobCompensation) {
    }

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
        state.setBeamHeight((float) (beamTopY - posY) - BEAM_BASE_Y);
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
        DEFERRED_BEAMS.put(state.blockPos, new BeamRenderData(state.blockPos, beamHeight));
    }

    /**
     * 在关卡完成后渲染本帧收集的腐化信标光束。
     */
    public static void renderDeferredBeams(
        PoseStack pose,
        MultiBufferSource.BufferSource bufferSource,
        Vec3 cameraPosition
    ) {
        if (DEFERRED_BEAMS.isEmpty() && DEFERRED_WEAPON_BEAMS.isEmpty()) return;
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.CORRUPTED_BEACON_BEAM);
        for (BeamRenderData data : DEFERRED_BEAMS.values()) {
            pose.pushPose();
            pose.translate(
                data.pos().getX() - cameraPosition.x(),
                data.pos().getY() - cameraPosition.y(),
                data.pos().getZ() - cameraPosition.z()
            );
            emitDeferredBeam(consumer, pose.last().pose(), data.beamHeight());
            pose.popPose();
        }
        for (WeaponBeamRenderData data : DEFERRED_WEAPON_BEAMS) {
            Vec3 direction = data.end().subtract(data.start());
            if (direction.lengthSqr() < 1.0E-6) continue;
            pose.pushPose();
            if (data.viewBobCompensation() != null) {
                pose.last().pose().mul(data.viewBobCompensation());
            }
            pose.translate(
                data.start().x - cameraPosition.x,
                data.start().y - cameraPosition.y,
                data.start().z - cameraPosition.z
            );
            pose.mulPose(new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F),
                direction.toVector3f().normalize()
            ));
            pose.scale(0.5F, 1.0F, 0.5F);
            emitWeaponBeam(consumer, pose.last().pose(), (float) direction.length());
            pose.popPose();
        }
        bufferSource.endBatch(ModRenderTypes.CORRUPTED_BEACON_BEAM);
        DEFERRED_BEAMS.clear();
        DEFERRED_WEAPON_BEAMS.clear();
    }

    public static void deferWeaponBeam(Vec3 start, Vec3 end, @Nullable Matrix4f viewBobCompensation) {
        DEFERRED_WEAPON_BEAMS.add(new WeaponBeamRenderData(start, end, viewBobCompensation));
    }

    private static void emitWeaponBeam(VertexConsumer consumer, Matrix4f matrix, float length) {
        for (int layer = BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = BEAM_INNER_HALF + BEAM_GLOW_HALF_STEP * layer * 0.5F;
            float falloff = 1.0F / (layer + 1);
            float alpha = 0.65F * falloff;
            emitBeamPyramid(
                consumer,
                matrix,
                0.0F,
                0.0F,
                0.0F,
                half,
                length,
                GLOW_R,
                GLOW_G,
                GLOW_B,
                alpha,
                0.24F * falloff
            );
        }
        emitBeamPyramid(
            consumer,
            matrix,
            0.0F,
            0.0F,
            0.0F,
            BEAM_INNER_HALF,
            length,
            CORE_R,
            CORE_G,
            CORE_B,
            0.94F,
            0.22F
        );
    }

    private static void emitDeferredBeam(VertexConsumer consumer, Matrix4f matrix, float beamHeight) {
        float apexY = BEAM_BASE_Y + beamHeight;
        for (int layer = BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = BEAM_INNER_HALF + BEAM_GLOW_HALF_STEP * layer;
            float falloff = 1.0f / (layer + 1);
            float alpha = 0.65f * falloff;
            float tipFade = 0.24f * falloff;
            emitBeamPyramid(
                consumer,
                matrix,
                half,
                apexY,
                GLOW_R,
                GLOW_G,
                GLOW_B,
                alpha,
                tipFade
            );
        }
        emitBeamPyramid(
            consumer,
            matrix,
            BEAM_INNER_HALF,
            apexY,
            CORE_R,
            CORE_G,
            CORE_B,
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
        emitBeamPyramid(
            vc,
            matrix,
            0.5F,
            BEAM_BASE_Y,
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
            beamVertex(vc, matrix, c0[0], baseY, c0[1], red, green, blue, alpha);
            beamVertex(vc, matrix, c1[0], baseY, c1[1], red, green, blue, alpha);
            beamVertex(vc, matrix, cx, apexY, cz, red, green, blue, tipAlpha);
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
