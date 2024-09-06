package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
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
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.solid()),
                null,
                Minecraft.getInstance().getModelManager().getModel(HELIOSTATS_HEAD),
                0, 0, 0, packedLight, packedOverlay
        );
        poseStack.popPose();
    }
}
