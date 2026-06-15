package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.item.InfinityItemStackHandler;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CreativeCrateBlockEntityRenderer implements BlockEntityRenderer<CreativeCrateBlockEntity> {
    public CreativeCrateBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public void render(
        CreativeCrateBlockEntity crate, float tickDelta, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, int overlay) {
        InfinityItemStackHandler itemStackHandler = crate.getItemStackHandler();
        ItemStack stackInSlot = itemStackHandler.getStackInSlot(0);
        if (stackInSlot.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.9);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(stackInSlot, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, crate.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.1);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(stackInSlot, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, crate.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.9, 0.5, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(stackInSlot, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, crate.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.1, 0.5, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(stackInSlot, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, crate.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.1, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(stackInSlot, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, crate.getLevel(), 0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.9, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(stackInSlot, ItemDisplayContext.FIXED, light, overlay, poseStack, vertexConsumers, crate.getLevel(), 0);
        poseStack.popPose();
    }
}
