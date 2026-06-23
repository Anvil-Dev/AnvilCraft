package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CreativeCrateRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
        if (!stack.isEmpty()) {
            state.setItem(FeatureRendererSupport.initialize(stack, this.resolver));
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

        // Render item on all 6 sides
        for (int side = 0; side < 6; side++) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            float scale = 0.8f;
            poseStack.scale(scale, scale, scale);
            switch (side) {
                case 0 -> { // +Z (south)
                    poseStack.translate(0, 0, 0.5);
                }
                case 1 -> { // -Z (north)
                    poseStack.translate(0, 0, -0.5);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
                case 2 -> { // +X (east)
                    poseStack.translate(0.5, 0, 0);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                }
                case 3 -> { // -X (west)
                    poseStack.translate(-0.5, 0, 0);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                }
                case 4 -> { // +Y (top)
                    poseStack.translate(0, 0.5, 0);
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                }
                case 5 -> { // -Y (bottom)
                    poseStack.translate(0, -0.5, 0);
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                }
                default -> {
                    // Should not happen
                }
            }
            cluster.item.submit(poseStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
