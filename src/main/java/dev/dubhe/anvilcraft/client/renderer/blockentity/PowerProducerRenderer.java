package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class PowerProducerRenderer<T extends BlockEntity & IPowerProducer>
    implements BlockEntityRenderer<T>, ModelSelectionRenderer<T> {
    public static final float ROTATION_MAGIC = 0.001220703125f;

    @Override
    public void render(
        T blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());
        collectSelectionModels(blockEntity, partialTick, poseStack, (model, pose) -> Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                pose.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(model),
                0,
                0,
                0,
                LightTexture.FULL_BLOCK,
                packedOverlay
        ));
    }

    @Override
    public void collectSelectionModels(T blockEntity, float partialTick, PoseStack poseStack, ModelConsumer consumer) {
        poseStack.pushPose();
        float rotation = rotation(blockEntity, partialTick);
        poseStack.translate(0.5F, elevation(), 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        consumer.accept(getModel(), poseStack);
        poseStack.popPose();
    }

    protected float rotation(T blockEntity, float partialTick) {
        return ((float) blockEntity.getTime() + partialTick) * (float) Math.log(blockEntity.getServerPower() + 1) * magic() * 50.0f;
    }

    protected float elevation() {
        return 0.8f;
    }

    protected float magic() {
        return ROTATION_MAGIC;
    }

    protected abstract ModelResourceLocation getModel();
}
