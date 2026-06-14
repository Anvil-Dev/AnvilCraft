package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.client.support.SeismicBounceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 震波弹跳渲染 —— ModelBlockRenderer.tesselateBlock 直通 SectionCompiler 的光照管线。
 */
@EventBusSubscriber(modid = "anvilcraft", value = Dist.CLIENT)
public class SeismicBounceRenderEventListener {

    @Nullable
    private static ModelBlockRenderer blockRenderer;

    @SubscribeEvent
    public static void onRender(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double camX = event.getLevelRenderState().cameraRenderState.pos.x();
        double camY = event.getLevelRenderState().cameraRenderState.pos.y();
        double camZ = event.getLevelRenderState().cameraRenderState.pos.z();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        var poseStack = event.getPoseStack();
        var nodeCollector = event.getSubmitNodeCollector();

        for (var entry : SeismicBounceManager.getInstance().getActiveBounces().entrySet()) {
            BlockPos pos = entry.getKey();
            var data = entry.getValue();

            float offsetY = data.getRenderOffsetY(partialTick);
            if (Math.abs(offsetY) < 0.001f) continue;

            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camX, pos.getY() - camY + offsetY, pos.getZ() - camZ);
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(1.0005f, 1.000f, 1.0005f);
            poseStack.translate(-0.5, -0.5, -0.5);

            if (blockRenderer == null) continue;
            var model = mc.getModelManager().getBlockStateModelSet().get(state);
            long seed = state.getSeed(pos);
            nodeCollector.submitCustomGeometry(poseStack, RenderTypes.solidMovingBlock(), (pose, consumer) ->
                blockRenderer.tesselateBlock(
                    (x, y, z, quad, instance) -> consumer.putBakedQuad(pose, quad, instance),
                    0, 0, 0,
                    mc.level, pos, state, model, seed
                )
            );

            poseStack.popPose();
        }
    }

}
