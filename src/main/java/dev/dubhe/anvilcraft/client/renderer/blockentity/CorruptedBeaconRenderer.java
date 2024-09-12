package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CorruptedBeaconRenderer implements BlockEntityRenderer<CorruptedBeaconBlockEntity> {
    public static final ResourceLocation BEAM_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    public static final int MAX_RENDER_Y = 1024;

    @SuppressWarnings("unused")
    public CorruptedBeaconRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            @NotNull CorruptedBeaconBlockEntity blockEntity,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (blockEntity.getLevel() == null) return;
        long l = blockEntity.getLevel().getGameTime();
        List<CorruptedBeaconBlockEntity.BeaconBeamSection> list = blockEntity.getBeamSections();
        int i = 0;
        for (int j = 0; j < list.size(); ++j) {
            CorruptedBeaconBlockEntity.BeaconBeamSection beaconBeamSection = list.get(j);
            CorruptedBeaconRenderer.renderBeaconBeam(
                    poseStack,
                    buffer,
                    partialTick,
                    l,
                    i,
                    j == list.size() - 1 ? MAX_RENDER_Y : beaconBeamSection.getHeight(),
                    beaconBeamSection.getColor());
            i += beaconBeamSection.getHeight();
        }
    }

    private static void renderBeaconBeam(
            PoseStack pPoseStack,
            MultiBufferSource pBufferSource,
            float pPartialTick,
            long pGameTime,
            int pYOffset,
            int pHeight,
            int pColor) {
        renderBeaconBeam(
                pPoseStack,
                pBufferSource,
                BEAM_LOCATION,
                pPartialTick,
                1.0F,
                pGameTime,
                pYOffset,
                pHeight,
                pColor,
                0.2F,
                0.25F);
    }

    public static void renderBeaconBeam(
            PoseStack pPoseStack,
            MultiBufferSource pBufferSource,
            ResourceLocation pBeamLocation,
            float pPartialTick,
            float pTextureScale,
            long pGameTime,
            int pYOffset,
            int pHeight,
            int pColor,
            float pBeamRadius,
            float pGlowRadius) {
        int i = pYOffset + pHeight;
        pPoseStack.pushPose();
        pPoseStack.translate(0.5, 0.0, 0.5);
        float f = (float) Math.floorMod(pGameTime, 40) + pPartialTick;
        float f1 = pHeight < 0 ? f : -f;
        float f2 = Mth.frac(f1 * 0.2F - (float) Mth.floor(f1 * 0.1F));
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(f * 2.25F - 45.0F));
        float f3;
        float f5;
        float f6 = -pBeamRadius;
        float f9 = -pBeamRadius;
        float f12 = -1.0F + f2;
        float f13 = (float) pHeight * pTextureScale * (0.5F / pBeamRadius) + f12;
        renderPart(
                pPoseStack,
                pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, false)),
                pColor,
                pYOffset,
                i,
                0.0F,
                pBeamRadius,
                pBeamRadius,
                0.0F,
                f6,
                0.0F,
                0.0F,
                f9,
                0.0F,
                1.0F,
                f13,
                f12);
        pPoseStack.popPose();
        f3 = -pGlowRadius;
        float f4 = -pGlowRadius;
        f5 = -pGlowRadius;
        f6 = -pGlowRadius;
        f12 = -1.0F + f2;
        f13 = (float) pHeight * pTextureScale + f12;
        renderPart(
                pPoseStack,
                pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, true)),
                FastColor.ARGB32.color(32, pColor),
                pYOffset,
                i,
                f3,
                f4,
                pGlowRadius,
                f5,
                f6,
                pGlowRadius,
                pGlowRadius,
                pGlowRadius,
                0.0F,
                1.0F,
                f13,
                f12);
        pPoseStack.popPose();
    }

    private static void renderPart(
            PoseStack pPoseStack,
            VertexConsumer pConsumer,
            int pColor,
            int pMinY,
            int pMaxY,
            float pX1,
            float pZ1,
            float pX2,
            float pZ2,
            float pX3,
            float pZ3,
            float pX4,
            float pZ4,
            float pMinU,
            float pMaxU,
            float pMinV,
            float pMaxV) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        renderQuad(
                posestack$pose,
                pConsumer,
                pColor,
                pMinY,
                pMaxY,
                pX1,
                pZ1,
                pX2,
                pZ2,
                pMinU,
                pMaxU,
                pMinV,
                pMaxV);
        renderQuad(
                posestack$pose,
                pConsumer,
                pColor,
                pMinY,
                pMaxY,
                pX4,
                pZ4,
                pX3,
                pZ3,
                pMinU,
                pMaxU,
                pMinV,
                pMaxV);
        renderQuad(
                posestack$pose,
                pConsumer,
                pColor,
                pMinY,
                pMaxY,
                pX2,
                pZ2,
                pX4,
                pZ4,
                pMinU,
                pMaxU,
                pMinV,
                pMaxV);
        renderQuad(
                posestack$pose,
                pConsumer,
                pColor,
                pMinY,
                pMaxY,
                pX3,
                pZ3,
                pX1,
                pZ1,
                pMinU,
                pMaxU,
                pMinV,
                pMaxV);
    }

    private static void renderQuad(
            PoseStack.Pose pPose,
            VertexConsumer pConsumer,
            int pColor,
            int pMinY,
            int pMaxY,
            float pMinX,
            float pMinZ,
            float pMaxX,
            float pMaxZ,
            float pMinU,
            float pMaxU,
            float pMinV,
            float pMaxV) {
        addVertex(pPose, pConsumer, pColor, pMaxY, pMinX, pMinZ, pMaxU, pMinV);
        addVertex(pPose, pConsumer, pColor, pMinY, pMinX, pMinZ, pMaxU, pMaxV);
        addVertex(pPose, pConsumer, pColor, pMinY, pMaxX, pMaxZ, pMinU, pMaxV);
        addVertex(pPose, pConsumer, pColor, pMaxY, pMaxX, pMaxZ, pMinU, pMinV);
    }

    private static void addVertex(
            PoseStack.Pose pPose,
            VertexConsumer pConsumer,
            int pColor,
            int pY,
            float pX,
            float pZ,
            float pU,
            float pV) {
        pConsumer
                .addVertex(pPose, pX, (float) pY, pZ)
                .setColor(pColor)
                .setUv(pU, pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setUv2(0xF000F0 & '\uffff', 0xF000F0 & '\uffff')
                .setNormal(pPose, 0.0F, 1.0F, 0.0F);
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
    public boolean shouldRender(
            @NotNull CorruptedBeaconBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
                .multiply(1.0, 0.0, 1.0)
                .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }
}
