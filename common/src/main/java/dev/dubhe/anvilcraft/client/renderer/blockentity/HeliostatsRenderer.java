package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class HeliostatsRenderer implements BlockEntityRenderer<HeliostatsBlockEntity> {
    private static final ModelResourceLocation HELIOSTATS_HEAD =
            new ModelResourceLocation("anvilcraft", "heliostats_head", "");

    @SuppressWarnings("unused")
    public HeliostatsRenderer(BlockEntityRendererProvider.Context context) {
    }

    private float getHorizontalAngle(float x, float z) {
        float angle = (float) Math.atan(x / z);
        return z < 0 ? (float) (angle + Math.PI) : angle;
    }

    @Override
    public void render(@NotNull HeliostatsBlockEntity blockEntity,
                       float partialTick,
                       @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer,
                       int packedLight,
                       int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5625, 0.5);
        /*
        if (!blockEntity.getNormalVector3f().equals(new Vector3f())) {
            final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(new ResourceLocation("block/white_concrete"));
            poseStack.mulPose(new Quaternionf().rotateTo(new Vector3f(0, 1, 0), blockEntity.getIrritateVector3f()));
            VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
            renderBox(
                    consumer, poseStack,
                    blockEntity.getIrritateDistance(), sprite);
            poseStack.mulPose(new Quaternionf().rotateTo(blockEntity.getIrritateVector3f(), new Vector3f(0, 1, 0)));
        }*/
        if (!blockEntity.getNormalVector3f().equals(new Vector3f())
                && !blockEntity.getNormalVector3f().equals(new Vector3f(Float.NaN))) {
            poseStack.mulPose(new Quaternionf().rotateY(
                    getHorizontalAngle(blockEntity.getNormalVector3f().x, blockEntity.getNormalVector3f().z)
            ));
            poseStack.mulPose(new Quaternionf().rotateX(
                    (float) (
                            Math.atan(Math.sqrt((blockEntity.getNormalVector3f().z * blockEntity.getNormalVector3f().z)
                                    + (blockEntity.getNormalVector3f().x * blockEntity.getNormalVector3f().x))
                                    / blockEntity.getNormalVector3f().y)
                    )
            ));
        }
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(HELIOSTATS_HEAD),
                0, 0, 0, packedLight, packedOverlay
        );
        poseStack.popPose();
    }

    @SuppressWarnings("unused")
    private static void renderBox(
            VertexConsumer consumer,
            @NotNull PoseStack poseStack,
            float maxY, TextureAtlasSprite sprite
    ) {
        renderQuadX(consumer, poseStack,
                (float) 0.0625, (float) 0.0625, (float) -0.0625, maxY, (float) 0.0625,
                sprite, Direction.EAST.getNormal());
        renderQuadX(consumer, poseStack,
                (float) -0.0625, (float) -0.0625, (float) 0.0625, maxY, (float) -0.0625,
                sprite, Direction.WEST.getNormal());
        renderQuadY(consumer, poseStack,
                maxY, maxY, (float) -0.0625, (float) 0.0625,
                sprite, Direction.DOWN.getNormal());
        renderQuadY(consumer, poseStack,
                (float) 0, (float) 0, (float) 0.0625, (float) -0.0625,
                sprite, Direction.UP.getNormal());
        renderQuadZ(consumer, poseStack,
                (float) 0.0625, (float) 0.0625, maxY, (float) 0,
                sprite, Direction.SOUTH.getNormal());
        renderQuadZ(consumer, poseStack,
                (float) -0.0625, (float) -0.0625, (float) 0, maxY,
                sprite, Direction.NORTH.getNormal());
    }

    private static void renderQuadX(
            VertexConsumer consumer,
            @NotNull PoseStack poseStack,
            float minX, float maxX, float minZ, float maxY, float maxZ, TextureAtlasSprite sprite, Vec3i normal
    ) {
        addVertex(consumer, poseStack, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), normal);
        addVertex(consumer, poseStack, minX, maxY, maxZ, sprite.getU0(), sprite.getV1(), normal);
        addVertex(consumer, poseStack, maxX, (float) 0.0, maxZ, sprite.getU0(), sprite.getV0(), normal);
        addVertex(consumer, poseStack, maxX, (float) 0.0, minZ, sprite.getU1(), sprite.getV0(), normal);
    }

    private static void renderQuadY(
            VertexConsumer consumer,
            @NotNull PoseStack poseStack,
            float minY, float maxY, float minX, float maxX, TextureAtlasSprite sprite, Vec3i normal
    ) {
        addVertex(consumer, poseStack, minX, minY, (float) -0.0625, sprite.getU1(), sprite.getV1(), normal);
        addVertex(consumer, poseStack, minX, minY, (float) 0.0625, sprite.getU0(), sprite.getV1(), normal);
        addVertex(consumer, poseStack, maxX, maxY, (float) 0.0625, sprite.getU0(), sprite.getV0(), normal);
        addVertex(consumer, poseStack, maxX, maxY, (float) -0.0625, sprite.getU1(), sprite.getV0(), normal);
    }

    private static void renderQuadZ(
            VertexConsumer consumer,
            @NotNull PoseStack poseStack,
            float minZ, float maxZ, float minY, float maxY, TextureAtlasSprite sprite, Vec3i normal
    ) {
        addVertex(consumer, poseStack, (float) -0.0625, maxY, minZ, sprite.getU1(), sprite.getV1(), normal);
        addVertex(consumer, poseStack, (float) 0.0625, maxY, minZ, sprite.getU0(), sprite.getV1(), normal);
        addVertex(consumer, poseStack, (float) 0.0625, minY, maxZ, sprite.getU0(), sprite.getV0(), normal);
        addVertex(consumer, poseStack, (float) -0.0625, minY, maxZ, sprite.getU1(), sprite.getV0(), normal);
    }

    private static void addVertex(
            @NotNull VertexConsumer consumer, @NotNull PoseStack poseStack,
            float x, float y, float z, float u, float v, Vec3i normal) {
        consumer
                .vertex(poseStack.last().pose(), x, y, z)
                .color(0.9726f, 0.92343f, 0.621093f, (float) 0.4)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(normal.getX(), normal.getY(), normal.getZ())
                .endVertex();
    }
}
