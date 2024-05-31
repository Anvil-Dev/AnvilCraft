package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class LaseRenderer implements BlockEntityRenderer<BaseLaserBlockEntity> {

    @SuppressWarnings("unused")
    public LaseRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull BaseLaserBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) return;
        if (blockEntity.irradiateBlockPos == null) return;
        poseStack.pushPose();
        VertexConsumer consumer =
            buffer.getBuffer(RenderType.beaconBeam(new ResourceLocation("textures/entity/beacon_beam.png"), true));
        float length =
            (float) blockEntity.irradiateBlockPos.getCenter().distanceTo(blockEntity.getBlockPos().getCenter());
        poseStack.translate(0.5f, 0.5f, 0.5);
        switch (blockEntity.getDirection()) {
            case EAST -> renderBox(
                consumer, poseStack,
                0, -0.0625f, -0.0625f, length, 0.0625f, 0.0625f
            );
            case WEST -> renderBox(
                consumer, poseStack,
                -length, -0.0625f, -0.0625f, 0, 0.0625f, 0.0625f
            );
            case SOUTH -> renderBox(
                consumer, poseStack,
                -0.0625f, -0.0625f, 0, 0.0625f, 0.0625f, length
            );

            case NORTH -> renderBox(
                consumer, poseStack,
                -0.0625f, -0.0625f, -length, 0.0625f, 0.0625f, 0
            );
            case UP -> renderBox(
                consumer, poseStack,
                -0.0625f, 0, -0.0625f, 0.0625f, length, 0.0625f
            );
            default -> renderBox(
                consumer, poseStack,
                -0.0625f, -length, -0.0625f, 0.0625f, 0, 0.0625f
            );
        }
        poseStack.popPose();
    }

    private static void renderBox(
        VertexConsumer consumer,
        @NotNull PoseStack poseStack,
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ
    ) {
        renderQuadX(consumer, poseStack, maxX, maxX, minY, minZ, maxY, maxZ);
        renderQuadX(consumer, poseStack, minX, minX, minY, maxZ, maxY, minZ);
        renderQuadY(consumer, poseStack, maxY, maxY, minX, minZ, maxX, maxZ);
        renderQuadY(consumer, poseStack, minY, minY, maxX, minZ, minX, maxZ);
        renderQuadZ(consumer, poseStack, maxZ, maxZ, minX, maxY, maxX, minY);
        renderQuadZ(consumer, poseStack, minZ, minZ, minX, minY, maxX, maxY);
    }


    private static void renderQuadX(
        VertexConsumer consumer,
        @NotNull PoseStack poseStack,
        float minX, float maxX, float minY, float minZ, float maxY, float maxZ
    ) {
        addVertex(consumer, poseStack, minX, maxY, minZ);
        addVertex(consumer, poseStack, minX, maxY, maxZ);
        addVertex(consumer, poseStack, maxX, minY, maxZ);
        addVertex(consumer, poseStack, maxX, minY, minZ);
    }

    private static void renderQuadY(
        VertexConsumer consumer,
        @NotNull PoseStack poseStack,
        float minY, float maxY, float minX, float minZ, float maxX, float maxZ
    ) {
        addVertex(consumer, poseStack, minX, minY, minZ);
        addVertex(consumer, poseStack, minX, minY, maxZ);
        addVertex(consumer, poseStack, maxX, maxY, maxZ);
        addVertex(consumer, poseStack, maxX, maxY, minZ);
    }

    private static void renderQuadZ(
        VertexConsumer consumer,
        @NotNull PoseStack poseStack,
        float minZ, float maxZ, float minX, float minY, float maxX, float maxY
    ) {
        addVertex(consumer, poseStack, minX, maxY, minZ);
        addVertex(consumer, poseStack, maxX, maxY, minZ);
        addVertex(consumer, poseStack, maxX, minY, maxZ);
        addVertex(consumer, poseStack, minX, minY, maxZ);
    }

    private static void addVertex(
        @NotNull VertexConsumer consumer, @NotNull PoseStack poseStack, float x, float y, float z) {
        consumer
            .vertex(poseStack.last().pose(), x, y, z)
            .color(1f, 0, 0, 10f)
            .uv(1, 1)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(0xF000F0)
            .normal(1, 0, 0)
            .endVertex();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull BaseLaserBlockEntity blockEntity) {
        return true;
    }
}
