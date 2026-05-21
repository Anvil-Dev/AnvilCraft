package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.BaseShowItemRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class ChargerRenderer extends BaseShowItemRenderer<ChargerBlockEntity, BaseShowItemRenderState> {
    public ChargerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BaseShowItemRenderState createRenderState() {
        return new BaseShowItemRenderState();
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(ChargerBlockEntity blockEntity) {
        // 使用从服务端同步过来的显示物品
        return blockEntity.getDisplayItemStack();
    }

    @Override
    public void submit(
        BaseShowItemRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        ItemClusterRenderState cluster = state.getDisplayState();
        ItemStackRenderState item = cluster.item;
        AABB aabb = item.getModelBoundingBox();

        double modelDepth = aabb.getZsize();

        double x = 0.5;
        double y = 0.5625 + modelDepth / 4;
        double z = 0.375;

        poseStack.pushPose();

        // 先平移到计算好的位置，再进行旋转
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, cluster.outlineColor);
        poseStack.popPose();
    }
}
