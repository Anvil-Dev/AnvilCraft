package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class DischargerBlockRenderer extends BaseShowItemRenderer<DischargerBlockEntity> {
    public DischargerBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(DischargerBlockEntity blockEntity) {
        return blockEntity.getDisplayItemStack();
    }

    @Override
    protected int getSeed(DischargerBlockEntity blockEntity) {
        return 0;
    }

    @Override
    public void render(
        DischargerBlockEntity be,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        ItemStack stack = getDisplayItemStack(be);
        if (stack == null || stack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.586, 0.5);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));

        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, be.getLevel(), 0);
        poseStack.popPose();
    }
}
