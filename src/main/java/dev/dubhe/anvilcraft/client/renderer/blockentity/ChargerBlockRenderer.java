package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ChargerBlockRenderer extends BaseShowItemRenderer<ChargerBlockEntity> {
    public ChargerBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(ChargerBlockEntity blockEntity) {
        // 使用从服务端同步过来的显示物品
        return blockEntity.getDisplayItemStack();
    }

    @Override
    protected int getSeed(ChargerBlockEntity blockEntity) {
        return 0;
    }

    @Override
    public void render(
        ChargerBlockEntity be,
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
