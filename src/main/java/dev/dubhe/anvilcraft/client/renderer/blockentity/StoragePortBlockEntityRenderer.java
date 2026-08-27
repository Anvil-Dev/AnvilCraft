package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 仓储端口渲染器：已标记时在方块六个面上渲染标记物品（与创造板条箱一致）。
 */
public class StoragePortBlockEntityRenderer implements BlockEntityRenderer<StoragePortBlockEntity> {
    public StoragePortBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public void render(
        StoragePortBlockEntity port,
        float tickDelta,
        PoseStack poseStack,
        MultiBufferSource vertexConsumers,
        int light,
        int overlay
    ) {
        ItemStack markedItem = port.getMarkedItem();
        if (markedItem.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.9);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(markedItem, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, port.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.1);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(markedItem, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, port.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.9, 0.5, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(markedItem, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, port.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.1, 0.5, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(markedItem, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, port.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.1, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(markedItem, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, port.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.9, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(markedItem, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, port.getLevel(), 0);
        poseStack.popPose();
    }
}