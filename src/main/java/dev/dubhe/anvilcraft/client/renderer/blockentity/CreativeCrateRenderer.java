package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CreativeCrateRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CreativeCrateRenderer implements BlockEntityRenderer<CreativeCrateBlockEntity, CreativeCrateRenderState> {
    private final ItemModelResolver resolver;

    public CreativeCrateRenderer(BlockEntityRendererProvider.Context ctx) {
        this.resolver = ctx.itemModelResolver();
    }

    @Override
    public CreativeCrateRenderState createRenderState() {
        return new CreativeCrateRenderState();
    }

    @Override
    public void extractRenderState(
        CreativeCrateBlockEntity be,
        CreativeCrateRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        ItemStack stack = be.getDisplayStack();
        state.setItem(null);
        if (!stack.isEmpty()) {
            ItemClusterRenderState cluster = new ItemClusterRenderState();
            cluster.seed = ItemClusterRenderState.getSeedForItemStack(stack);
            this.resolver.updateForTopItem(cluster.item, stack, ItemDisplayContext.FIXED, null, null, cluster.seed);
            cluster.count = ItemClusterRenderState.getRenderedAmount(stack.getCount());
            state.setItem(cluster);
        }
    }

    @Override
    public void submit(
        CreativeCrateRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        ItemClusterRenderState cluster = state.getItem();
        if (cluster == null) return;
        int light = state.lightCoords;

        // 在六个面的近表面渲染物品，参考 1.21.1 的渲染位置
        for (int side = 0; side < 6; side++) {
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 0.5f);
            switch (side) {
                case 0 -> poseStack.translate(1f, 1f, 0.2f);
                case 1 -> {
                    poseStack.translate(1f, 1f, 1.8f);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
                case 2 -> {
                    poseStack.translate(0.2f, 1f, 1f);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                }
                case 3 -> {
                    poseStack.translate(1.8f, 1f, 1f);
                    poseStack.mulPose(Axis.YP.rotationDegrees(270));
                }
                case 4 -> {
                    poseStack.translate(1f, 0.2f, 1f);
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(270));
                }
                case 5 -> {
                    poseStack.translate(1f, 1.8f, 1f);
                    poseStack.mulPose(Axis.XP.rotationDegrees(270));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(270));
                }
                default -> {
                    // This should never happen as side is always 0-5
                }
            }
            poseStack.scale(10.0f / 16.0f, 10.0f / 16.0f, 10.0f / 16.0f);
            cluster.item.submit(poseStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
