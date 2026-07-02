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

/// 腐化信标渲染器：绘制锻星砧风格的四棱锥暗色光束（深紫黑，普通半透明混合以呈"暗色遮挡"感）。
/// 玻璃外壳已由方块模型自身渲染，故此处只画光束。
/// 采用 26.1 extract/submit 架构 + collector.submitCustomGeometry（与超新星光束同一条可靠路径，无闪烁）。
public class CorruptedBeaconRenderer
    implements BlockEntityRenderer<CorruptedBeaconBlockEntity, CorruptedBeaconRenderState> {

    /// 四棱锥光束参数
    private static final float BEAM_BASE_Y = 0.5f;
    private static final float BEAM_INNER_HALF = 0.08f;
    private static final int BEAM_GLOW_LAYERS = 4;
    private static final float BEAM_GLOW_HALF_STEP = 0.06f;

    /// 暗色光束颜色（深紫黑）
    private static final float BEAM_R = 0.02f;
    private static final float BEAM_G = 0.0f;
    private static final float BEAM_B = 0.05f;

    @SuppressWarnings("unused")
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
    public void submit(CorruptedBeaconRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (!state.isLit()) return;
        float beamHeight = state.getBeamHeight();
        if (beamHeight <= 0.01f) return;
        float apexY = BEAM_BASE_Y + beamHeight;
        collector.submitCustomGeometry(pose, ModRenderTypes.CORRUPTED_BEACON_BEAM, (last, consumer) -> {
            Matrix4f matrix = last.pose();
            for (int layer = BEAM_GLOW_LAYERS; layer >= 1; layer--) {
                float half = BEAM_INNER_HALF + BEAM_GLOW_HALF_STEP * layer;
                float falloff = 1.0f / (layer + 1);
                falloff *= falloff;
                float alpha = 0.45f * falloff;
                float tipFade = 0.3f * falloff;
                emitBeamPyramid(consumer, matrix, half, apexY, alpha, tipFade);
            }
            emitBeamPyramid(consumer, matrix, BEAM_INNER_HALF, apexY, 0.82f, 0.25f);
        });
    }

    /// 发射一个以 (0.5,0.5) 为水平中心的四棱锥：方形底面在 BEAM_BASE_Y、半宽 halfWidth，锥尖在 (0.5, apexY, 0.5)。
    private static void emitBeamPyramid(VertexConsumer vc, Matrix4f matrix, float halfWidth,
                                        float apexY, float alpha, float tipFade) {
        float cx = 0.5f;
        float cz = 0.5f;
        float x0 = cx - halfWidth;
        float x1 = cx + halfWidth;
        float z0 = cz - halfWidth;
        float z1 = cz + halfWidth;
        float[][] corners = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
        float tipAlpha = alpha * tipFade;
        for (int i = 0; i < 4; i++) {
            float[] c0 = corners[i];
            float[] c1 = corners[(i + 1) % 4];
            beamVertex(vc, matrix, c0[0], BEAM_BASE_Y, c0[1], alpha);
            beamVertex(vc, matrix, c1[0], BEAM_BASE_Y, c1[1], alpha);
            beamVertex(vc, matrix, cx, apexY, cz, tipAlpha);
            beamVertex(vc, matrix, cx, apexY, cz, tipAlpha);
        }
    }

    private static void beamVertex(VertexConsumer vc, Matrix4f matrix,
                                   float x, float y, float z, float alpha) {
        vc.addVertex(matrix, x, y, z).setColor(BEAM_R, BEAM_G, BEAM_B, alpha)
            .setUv(0.5f, 0.5f).setUv1(0, 0).setUv2(240, 240).setNormal(0, 1, 0);
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
