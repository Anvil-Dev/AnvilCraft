package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.BaseShowItemRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BaseShowItemRenderer<B extends BlockEntity, S extends BaseShowItemRenderState> implements BlockEntityRenderer<B, S> {
    private final ItemModelResolver resolver;

    public BaseShowItemRenderer(BlockEntityRendererProvider.Context ctx) {
        this.resolver = ctx.itemModelResolver();
    }

    @Override
    public void extractRenderState(
        B be,
        S state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        ItemStack stack = this.getDisplayItemStack(be);
        if (stack == null || stack.isEmpty()) return;
        state.setRotation((be.getLevel().getGameTime() + partialTicks) * 2f);
        state.setDisplay(stack);
        state.setDisplayState(new ItemClusterRenderState());
        state.getDisplayState().extractItemGroupRenderState(null, stack, this.resolver);
    }

    @Nullable
    protected abstract ItemStack getDisplayItemStack(B blockEntity);

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        ItemStack display = state.getDisplay();
        final RandomSource random = RandomSource.create(Item.getId(display.getItem()) + display.getDamageValue());
        ItemClusterRenderState cluster = state.getDisplayState();
        int amount = cluster.count;
        if (amount == 0) return;
        random.setSeed(cluster.seed);
        ItemStackRenderState item = cluster.item;
        AABB modelBB = item.getModelBoundingBox();
        float modelDepth = (float) modelBB.getZsize();
        if (modelDepth > 0.0625F) {
            item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, cluster.outlineColor);

            for (int i = 1; i < amount; i++) {
                poseStack.pushPose();
                float xo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                float yo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                float zo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                if (cluster.shouldSpread) {
                    poseStack.translate(xo, yo, zo);
                }
                item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, cluster.outlineColor);
                poseStack.popPose();
            }
        } else {
            float offsetZ = modelDepth * 1.5F;
            poseStack.translate(0.0F, 0.0F, -(offsetZ * (amount - 1) / 2.0F));
            item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, cluster.outlineColor);
            poseStack.translate(0.0F, 0.0F, offsetZ);

            for (int i = 1; i < amount; i++) {
                poseStack.pushPose();
                float xo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                float yo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                if (cluster.shouldSpread) {
                    poseStack.translate(xo, yo, 0.0F);
                }
                item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, cluster.outlineColor);
                poseStack.popPose();
                poseStack.translate(0.0F, 0.0F, offsetZ);
            }
        }
    }
}
