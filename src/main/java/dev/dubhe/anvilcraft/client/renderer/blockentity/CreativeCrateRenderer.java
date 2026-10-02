package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.CreativeCrateRenderUtil;
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
            this.resolver.updateForTopItem(cluster.item, stack, ItemDisplayContext.FIXED, be.getLevel(), null, 0);
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
        CreativeCrateRenderUtil.submit(cluster.item, poseStack, submitNodeCollector,
            state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
