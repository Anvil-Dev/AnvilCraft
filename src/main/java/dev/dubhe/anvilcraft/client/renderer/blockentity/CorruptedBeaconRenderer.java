package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CorruptedBeaconRenderer implements BlockEntityRenderer<CorruptedBeaconBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;
    private final BlockState defaultLightState = Blocks.WHITE_CONCRETE.defaultBlockState();

    /// 四棱锥光束参数
    private static final float BEAM_BASE_Y = 0.5f;
    private static final float BEAM_INNER_HALF = 0.08f;
    private static final int BEAM_GLOW_LAYERS = 4;
    private static final float BEAM_GLOW_HALF_STEP = 0.06f;

    /// 暗色光束颜色（深紫黑）
    private static final float BEAM_R = 0.02f;
    private static final float BEAM_G = 0.0f;
    private static final float BEAM_B = 0.05f;

    /// 延迟渲染队列：光束在 AFTER_LEVEL 阶段渲染
    private static final List<BeamRenderData> deferredBeams = new ArrayList<>();
    private static final List<WeaponBeamRenderData> deferredWeaponBeams = new ArrayList<>();

    private record BeamRenderData(BlockPos pos, int beamTopY) {}

    private record WeaponBeamRenderData(Vec3 start, Vec3 end, @Nullable Matrix4f viewBobCompensation) {}

    @SuppressWarnings("unused")
    public CorruptedBeaconRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
        CorruptedBeaconBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        // 渲染紫色玻璃覆盖层
        final BakedModel model = blockRenderer.getBlockModel(defaultLightState);
        poseStack.pushPose();
        poseStack.translate(0.005f, 0.005f, 0.005f);
        poseStack.scale(0.99f, 0.99f, 0.99f);
        VertexConsumer vertexConsumer = buffer.getBuffer(ModRenderTypes.BEACON_GLASS);
        for (Direction value : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(
                null,
                value,
                level.random,
                ModelData.EMPTY,
                null
            );
            for (BakedQuad quad : quads) {
                vertexConsumer.putBulkData(
                    poseStack.last(),
                    quad,
                    109 / 255f,
                    1 / 255f,
                    206 / 255f,
                    0.3f,
                    packedLight,
                    packedOverlay
                );
            }
        }
        poseStack.popPose();

        BlockState state = level.getBlockState(blockEntity.getBlockPos());
        if (!state.hasProperty(CorruptedBeaconBlock.LIT) || !state.getValue(CorruptedBeaconBlock.LIT)) {
            return;
        }

        int beamTopY = blockEntity.getBeamHeight();
        int posY = blockEntity.getBlockPos().getY();
        if (beamTopY > posY + 1) {
            deferredBeams.add(new BeamRenderData(blockEntity.getBlockPos(), beamTopY));
        }
    }

    /**
     * 在 AFTER_LEVEL 阶段由 RenderEventListener 调用，渲染所有延迟光束。
     * 事件回调会立即提交对应的渲染批次。
     */
    public static void renderDeferredBeams(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera) {
        if (deferredBeams.isEmpty() && deferredWeaponBeams.isEmpty()) return;

        VertexConsumer vc = bufferSource.getBuffer(ModRenderTypes.CORRUPTED_BEACON_BEAM);

        for (BeamRenderData data : deferredBeams) {
            poseStack.pushPose();
            poseStack.translate(
                data.pos.getX() - camera.x,
                data.pos.getY() - camera.y,
                data.pos.getZ() - camera.z
            );

            float beamHeight = (float) (data.beamTopY - data.pos.getY()) - BEAM_BASE_Y;
            if (beamHeight > 0.01f) {
                renderBeam(vc, poseStack.last(), 0.5f, BEAM_BASE_Y, 0.5f, beamHeight);
            }

            poseStack.popPose();
        }

        for (WeaponBeamRenderData data : deferredWeaponBeams) {
            Vec3 direction = data.end.subtract(data.start);
            if (direction.lengthSqr() < 1.0E-6) continue;
            poseStack.pushPose();
            if (data.viewBobCompensation != null) {
                poseStack.last().pose().mul(data.viewBobCompensation);
            }
            poseStack.translate(
                data.start.x - camera.x,
                data.start.y - camera.y,
                data.start.z - camera.z
            );
            poseStack.mulPose(new Quaternionf().rotationTo(
                new Vector3f(0.0f, 1.0f, 0.0f),
                direction.toVector3f().normalize()
            ));
            poseStack.scale(0.5f, 1.0f, 0.5f);
            renderBeam(vc, poseStack.last(), 0.0f, 0.0f, 0.0f, (float) direction.length(), 0.5f);
            poseStack.popPose();
        }

        deferredBeams.clear();
        deferredWeaponBeams.clear();
    }

    public static void deferWeaponBeam(Vec3 start, Vec3 end, @Nullable Matrix4f viewBobCompensation) {
        deferredWeaponBeams.add(new WeaponBeamRenderData(start, end, viewBobCompensation));
    }

    public static void renderBeam(
        VertexConsumer vc,
        PoseStack.Pose pose,
        float centerX,
        float baseY,
        float centerZ,
        float length
    ) {
        renderBeam(vc, pose, centerX, baseY, centerZ, length, 1.0f);
    }

    public static void renderBeam(
        VertexConsumer vc,
        PoseStack.Pose pose,
        float centerX,
        float baseY,
        float centerZ,
        float length,
        float glowSpreadScale
    ) {
        float apexY = baseY + length;
        for (int layer = BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = BEAM_INNER_HALF + BEAM_GLOW_HALF_STEP * layer * glowSpreadScale;
            float falloff = 1.0f / (layer + 1);
            falloff *= falloff;
            float alpha = 0.45f * falloff;
            float tipFade = 0.3f * falloff;
            emitBeamPyramid(
                vc, pose, centerX, baseY, centerZ, half, apexY,
                BEAM_R, BEAM_G, BEAM_B, alpha, tipFade
            );
        }
        emitBeamPyramid(
            vc, pose, centerX, baseY, centerZ, BEAM_INNER_HALF, apexY,
            BEAM_R, BEAM_G, BEAM_B, 0.82f, 0.25f
        );
    }

    private static void emitBeamPyramid(
        VertexConsumer vc,
        PoseStack.Pose pose,
        float centerX,
        float baseY,
        float centerZ,
        float halfWidth,
        float apexY,
        float r, float g, float b,
        float alpha,
        float tipFade
    ) {
        float cx = centerX;
        float cz = centerZ;
        float x0 = cx - halfWidth;
        float x1 = cx + halfWidth;
        float z0 = cz - halfWidth;
        float z1 = cz + halfWidth;
        float[][] corners = {
            {x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}
        };
        float tipAlpha = alpha * tipFade;
        for (int i = 0; i < 4; i++) {
            float[] c0 = corners[i];
            float[] c1 = corners[(i + 1) % 4];
            vc.addVertex(pose, c0[0], baseY, c0[1]).setColor(r, g, b, alpha);
            vc.addVertex(pose, c1[0], baseY, c1[1]).setColor(r, g, b, alpha);
            vc.addVertex(pose, cx, apexY, cz).setColor(r, g, b, tipAlpha);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(CorruptedBeaconBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(CorruptedBeaconBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(CorruptedBeaconBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        int topY = Math.max(blockEntity.getBeamHeight(), pos.getY() + 1);
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, topY, pos.getZ() + 1);
    }
}
