package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FeCollectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class FeCollectorRenderer implements BlockEntityRenderer<FeCollectorBlockEntity> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/fe_collector_head")
    );

    @SuppressWarnings("unused")
    public FeCollectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        FeCollectorBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());
        float rotation = blockEntity.getRotation() + (float) (Math.log(blockEntity.getServerPower() + 1) * 2.5f * partialTick);
        poseStack.translate(0.5F, 0.68F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(MODEL),
                0, 0, 0,
                LightTexture.FULL_BLOCK,
                packedOverlay
            );
        poseStack.popPose();
    }
}
