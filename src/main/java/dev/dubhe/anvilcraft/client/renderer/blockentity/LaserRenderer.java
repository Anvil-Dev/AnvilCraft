package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RubyLaserBlockEntity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserRenderer implements BlockEntityRenderer<BaseLaserBlockEntity> {

    @SuppressWarnings("unused")
    public LaserRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        BaseLaserBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) return;
        if (blockEntity.irradiateBlockPos == null) return;
        final TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5);
        float offest = 0;
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        float length = (float) (blockEntity
            .irradiateBlockPos
            .getCenter()
            .distanceTo(blockEntity.getBlockPos().getCenter())
            - 0.5);
        if (blockEntity instanceof RubyLaserBlockEntity) offest = 0.489f;
        poseStack.mulPose(blockEntity.getDirection().getRotation());
        renderBox(consumer, poseStack, -0.0625f, -offest, -0.0625f, 0.0625f, length, 0.0625f, sprite);
        renderBox(consumer, poseStack, -0.0625f, length, -0.0625f, 0.0625f, length + 0.3f, 0.0625f, 0.35f, sprite);
        renderBox(
            consumer,
            poseStack,
            -0.0625f,
            length + 0.3f,
            -0.0625f,
            0.0625f,
            length + 0.57f,
            0.0625f,
            0.15f,
            sprite);
        poseStack.popPose();
    }

    private static void renderBox(
        VertexConsumer consumer,
        PoseStack poseStack,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        TextureAtlasSprite sprite) {
        renderQuadX(consumer, poseStack, maxX, maxX, minY, minZ, maxY, maxZ, 0.5f, sprite);
        renderQuadX(consumer, poseStack, minX, minX, minY, maxZ, maxY, minZ, 0.5f, sprite);
        renderQuadY(consumer, poseStack, maxY, maxY, minX, minZ, maxX, maxZ, 0.5f, sprite);
        renderQuadY(consumer, poseStack, minY, minY, maxX, minZ, minX, maxZ, 0.5f, sprite);
        renderQuadZ(consumer, poseStack, maxZ, maxZ, minX, maxY, maxX, minY, 0.5f, sprite);
        renderQuadZ(consumer, poseStack, minZ, minZ, minX, minY, maxX, maxY, 0.5f, sprite);
    }

    private static void renderBox(
        VertexConsumer consumer,
        PoseStack poseStack,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        float a,
        TextureAtlasSprite sprite) {
        renderQuadX(consumer, poseStack, maxX, maxX, minY, minZ, maxY, maxZ, a, sprite);
        renderQuadX(consumer, poseStack, minX, minX, minY, maxZ, maxY, minZ, a, sprite);
        renderQuadY(consumer, poseStack, maxY, maxY, minX, minZ, maxX, maxZ, a, sprite);
        renderQuadY(consumer, poseStack, minY, minY, maxX, minZ, minX, maxZ, a, sprite);
        renderQuadZ(consumer, poseStack, maxZ, maxZ, minX, maxY, maxX, minY, a, sprite);
        renderQuadZ(consumer, poseStack, minZ, minZ, minX, minY, maxX, maxY, a, sprite);
    }

    private static void renderQuadX(
        VertexConsumer consumer,
        PoseStack poseStack,
        float minX,
        float maxX,
        float minY,
        float minZ,
        float maxY,
        float maxZ,
        float a,
        TextureAtlasSprite sprite) {
        addVertex(consumer, poseStack, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), a);
        addVertex(consumer, poseStack, minX, maxY, maxZ, sprite.getU0(), sprite.getV1(), a);
        addVertex(consumer, poseStack, maxX, minY, maxZ, sprite.getU0(), sprite.getV0(), a);
        addVertex(consumer, poseStack, maxX, minY, minZ, sprite.getU1(), sprite.getV0(), a);
    }

    private static void renderQuadY(
        VertexConsumer consumer,
        PoseStack poseStack,
        float minY,
        float maxY,
        float minX,
        float minZ,
        float maxX,
        float maxZ,
        float a,
        TextureAtlasSprite sprite) {
        addVertex(consumer, poseStack, minX, minY, minZ, sprite.getU1(), sprite.getV1(), a);
        addVertex(consumer, poseStack, minX, minY, maxZ, sprite.getU0(), sprite.getV1(), a);
        addVertex(consumer, poseStack, maxX, maxY, maxZ, sprite.getU0(), sprite.getV0(), a);
        addVertex(consumer, poseStack, maxX, maxY, minZ, sprite.getU1(), sprite.getV0(), a);
    }

    private static void renderQuadZ(
        VertexConsumer consumer,
        PoseStack poseStack,
        float minZ,
        float maxZ,
        float minX,
        float minY,
        float maxX,
        float maxY,
        float a,
        TextureAtlasSprite sprite) {
        addVertex(consumer, poseStack, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), a);
        addVertex(consumer, poseStack, maxX, maxY, minZ, sprite.getU0(), sprite.getV1(), a);
        addVertex(consumer, poseStack, maxX, minY, maxZ, sprite.getU0(), sprite.getV0(), a);
        addVertex(consumer, poseStack, minX, minY, maxZ, sprite.getU1(), sprite.getV0(), a);
    }

    @Override
    public AABB getRenderBoundingBox(BaseLaserBlockEntity blockEntity) {
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        return AABB.ofSize(
            blockEntity.getBlockPos().getCenter(), renderDistance * 2, renderDistance * 2, renderDistance * 2);
    }

    private static void addVertex(
        VertexConsumer consumer,
        PoseStack poseStack,
        float x,
        float y,
        float z,
        float u,
        float v,
        float a) {
        consumer.addVertex(poseStack.last().pose(), x, y, z)
            .setColor(1f, 0.05f, 0.05f, a)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2(0xF000F0 & '\uffff', 0xf000f0 >> 16 & '\uffff')
            .setNormal(1, 0, 0);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(BaseLaserBlockEntity blockEntity) {
        return true;
    }
}
