package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public abstract class PowerProducerRenderer<T extends BlockEntity & IPowerProducer> implements BlockEntityRenderer<T> {
    public static final float ROTATION_MAGIC = 0.001220703125f;

    @Override
    public void render(
        @NotNull T blockEntity,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();
        float rotation = rotation(blockEntity, partialTick);
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());
        poseStack.translate(0.5F, elevation(), 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(getModel()),
                0,
                0,
                0,
                LightTexture.FULL_BLOCK,
                packedOverlay
            );
        poseStack.popPose();
    }

    protected float rotation(T blockEntity, float partialTick) {
        return ((float) blockEntity.getTime() + partialTick) * blockEntity.getServerPower() * magic();
    }

    protected float elevation() {
        return 0.8f;
    }

    protected float magic() {
        return ROTATION_MAGIC;
    }

    protected abstract ModelResourceLocation getModel();
}
