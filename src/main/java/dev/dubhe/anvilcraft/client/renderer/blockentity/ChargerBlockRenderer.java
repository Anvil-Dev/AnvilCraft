package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.client.support.RenderModelSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

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
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, be.getLevel(), null, getSeed(be));

        AABB aabb = RenderModelSupport.getSize(model);

        double modelDepth = aabb.getZsize();

        double x = 0.5;
        double y = 0.5625 + modelDepth / 4;
        double z = 0.375;

        poseStack.pushPose();

        // 先平移到计算好的位置，再进行旋转
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));

        Minecraft.getInstance()
            .getItemRenderer()
            .render(stack, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, model);
        poseStack.popPose();
    }
}
