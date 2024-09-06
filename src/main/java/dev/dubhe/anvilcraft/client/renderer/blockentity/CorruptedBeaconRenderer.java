package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

public class CorruptedBeaconRenderer implements BlockEntityRenderer<CorruptedBeaconBlockEntity> {
    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation("textures/entity/beacon_beam.png");
    public static final int MAX_RENDER_Y = 1024;

    @SuppressWarnings("unused")
    public CorruptedBeaconRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        @NotNull CorruptedBeaconBlockEntity blockEntity,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) return;
        long l = blockEntity.getLevel().getGameTime();
        List<CorruptedBeaconBlockEntity.BeaconBeamSection> list = blockEntity.getBeamSections();
        int i = 0;
        for (int j = 0; j < list.size(); ++j) {
            CorruptedBeaconBlockEntity.BeaconBeamSection beaconBeamSection = list.get(j);
            CorruptedBeaconRenderer.renderBeaconBeam(
                poseStack, buffer, partialTick, l, i,
                j == list.size() - 1 ? MAX_RENDER_Y : beaconBeamSection.getHeight(), beaconBeamSection.getColor());
            i += beaconBeamSection.getHeight();
        }
    }

    private static void renderBeaconBeam(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        float partialTick,
        long gameTime,
        int pyOffset,
        int height,
        float[] colors
    ) {
        CorruptedBeaconRenderer.renderBeaconBeam(
            poseStack, bufferSource, BEAM_LOCATION, partialTick,
            1.0f, gameTime, pyOffset, height, colors,
            0.2f, 0.25f
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void renderBeaconBeam(
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        ResourceLocation beamLocation,
        float partialTick,
        float textureScale,
        long gameTime,
        int pyOffset,
        int height,
        float @NotNull [] colors,
        float beamRadius,
        float glowRadius
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        float f = (float) Math.floorMod(gameTime, 40) + partialTick;
        float g = height < 0 ? f : -f;
        float h = Mth.frac(g * 0.2f - (float) Mth.floor(g * 0.1f));
        float j = colors[0];
        float k = colors[1];
        float l = colors[2];
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 2.25f - 45.0f));
        float m;
        float n = beamRadius;
        float o = beamRadius;
        float q = -beamRadius;
        float t = -beamRadius;
        float w = -1.0f + h;
        float x = (float) height * textureScale * (0.5f / beamRadius) + w;
        int i = pyOffset + height;
        CorruptedBeaconRenderer.renderPart(
            poseStack, bufferSource.getBuffer(RenderType.beaconBeam(beamLocation, false)),
            j, k, l, 1.0f, pyOffset, i, 0.0f, n, o, 0.0f, q, 0.0f, 0.0f, t, x, w);
        poseStack.popPose();
        m = -glowRadius;
        n = -glowRadius;
        o = glowRadius;
        q = -glowRadius;
        t = glowRadius;
        w = -1.0f + h;
        x = (float) height * textureScale + w;
        float p = -glowRadius;
        CorruptedBeaconRenderer.renderPart(
            poseStack, bufferSource.getBuffer(RenderType.beaconBeam(beamLocation, true)), j, k, l,
            0.125f, pyOffset, i, m, n, o, p, q, glowRadius, glowRadius, t, x, w);
        poseStack.popPose();
    }

    private static void renderPart(
        @NotNull PoseStack poseStack, VertexConsumer consumer, float red, float green, float blue, float alpha,
        int minY, int maxY, float x0, float z0, float x1, float z1, float x2, float z2,
        float x3, float z3, float minV, float maxV
    ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        CorruptedBeaconRenderer.renderQuad(matrix4f, matrix3f,
            consumer, red, green, blue, alpha, minY, maxY, x0, z0, x1, z1, minV, maxV);
        CorruptedBeaconRenderer.renderQuad(matrix4f, matrix3f,
            consumer, red, green, blue, alpha, minY, maxY, x3, z3, x2, z2, minV, maxV);
        CorruptedBeaconRenderer.renderQuad(matrix4f, matrix3f,
            consumer, red, green, blue, alpha, minY, maxY, x1, z1, x3, z3, minV, maxV);
        CorruptedBeaconRenderer.renderQuad(matrix4f, matrix3f,
            consumer, red, green, blue, alpha, minY, maxY, x2, z2, x0, z0, minV, maxV);
    }

    private static void renderQuad(
        Matrix4f pose, Matrix3f normal, VertexConsumer consumer, float red, float green,
        float blue, float alpha, int minY, int maxY, float minX, float minZ,
        float maxX, float maxZ, float minV, float maxV
    ) {
        CorruptedBeaconRenderer.addVertex(pose, normal, consumer,
            red, green, blue, alpha, maxY, minX, minZ, (float) 1.0, minV);
        CorruptedBeaconRenderer.addVertex(pose, normal, consumer,
            red, green, blue, alpha, minY, minX, minZ, (float) 1.0, maxV);
        CorruptedBeaconRenderer.addVertex(pose, normal, consumer,
            red, green, blue, alpha, minY, maxX, maxZ, (float) 0.0, maxV);
        CorruptedBeaconRenderer.addVertex(pose, normal, consumer,
            red, green, blue, alpha, maxY, maxX, maxZ, (float) 0.0, minV);
    }

    /**
     * @param u the left-most coordinate of the texture region
     * @param v the top-most coordinate of the texture region
     */
    private static void addVertex(
        Matrix4f pose, Matrix3f normal, @NotNull VertexConsumer consumer, float red,
        float green, float blue, float alpha, int y, float x, float z, float u, float v) {
        consumer.vertex(pose, x, y, z).color(red, green, blue, alpha).uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
            .normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull CorruptedBeaconBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(@NotNull CorruptedBeaconBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }
}
