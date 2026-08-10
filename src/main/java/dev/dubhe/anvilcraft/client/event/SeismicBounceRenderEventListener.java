package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.client.support.SeismicBounceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.Map;

/**
 * 震波弹跳渲染 —— 直接获取原版光照管线。
 *
 * <p>
 * 因为 {@code submitMovingBlock} 会把模型里的 quad 按材质层 (SOLID / CUTOUT / TRANSLUCENT)
 * 分发到正确的 RenderPipeline，所以同时支持实体方块、裁剪方块以及透明方块。
 */
@EventBusSubscriber(modid = "anvilcraft", value = Dist.CLIENT)
public class SeismicBounceRenderEventListener {

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

        SeismicBounceRenderEventListener.submitEntries(
            event,
            SeismicBounceManager.getInstance().getActiveBounces(),
            camX,
            camY,
            camZ,
            partialTick
        );
        SeismicBounceRenderEventListener.submitEntries(
            event,
            SeismicBounceManager.getInstance().getActiveResonances(),
            camX,
            camY,
            camZ,
            partialTick
        );
    }

    private static void submitEntries(
        SubmitCustomGeometryEvent event,
        Map<BlockPos, ? extends SeismicBounceManager.RenderOffset> entries,
        double camX,
        double camY,
        double camZ,
        float partialTick
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var poseStack = event.getPoseStack();
        var nodeCollector = event.getSubmitNodeCollector();
        for (var entry : entries.entrySet()) {
            BlockPos pos = entry.getKey();
            var data = entry.getValue();

            float offsetX = data.getRenderOffsetX(partialTick);
            float offsetY = data.getRenderOffsetY(partialTick);
            float offsetZ = data.getRenderOffsetZ(partialTick);
            if (Math.abs(offsetX) < 0.001F && Math.abs(offsetY) < 0.001F && Math.abs(offsetZ) < 0.001F) continue;

            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;

            BlockModelResolver blockModelResolver = mc.getBlockModelResolver();
            BlockModelRenderState modelRenderState = new BlockModelRenderState();
            blockModelResolver.update(
                modelRenderState,
                state,
                BlockDisplayContext.create()
            );

            poseStack.pushPose();
            poseStack.translate(
                pos.getX() - camX + offsetX + 0.001,
                pos.getY() - camY + offsetY,
                pos.getZ() - camZ + offsetZ + 0.001
            );

            modelRenderState.submit(
                poseStack,
                nodeCollector,
                LevelRenderer.getLightCoords(LevelRenderer.BrightnessGetter.DEFAULT, mc.level, state, pos.above()),
                OverlayTexture.NO_OVERLAY,
                0
            );

            poseStack.popPose();
        }
    }

}
