package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.SeismicBounceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * 震波弹跳的渲染事件监听器。
 *
 * <p>利用两阶段渲染：</p>
 * <ol>
 *   <li>{@link ExtractLevelRenderStateEvent} 中提取方块模型数据</li>
 *   <li>{@link SubmitCustomGeometryEvent} 中提交渲染</li>
 * </ol>
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class SeismicBounceRenderEventListener {

    /** 每帧缓存的 BlockModelRenderState */
    private static final Map<BlockPos, ExtractedModel> EXTRACTED_MODELS = new HashMap<>();

    @SubscribeEvent
    public static void onExtract(ExtractLevelRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        EXTRACTED_MODELS.clear();

        SeismicBounceManager manager = SeismicBounceManager.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (Map.Entry<BlockPos, SeismicBounceManager.BounceData> entry : manager.getActiveBounces().entrySet()) {
            BlockPos pos = entry.getKey();
            SeismicBounceManager.BounceData data = entry.getValue();

            float offsetY = data.getRenderOffsetY(partialTick);
            if (Math.abs(offsetY) < 0.001f) continue;

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;

            try {
                BlockModelRenderState model = new BlockModelRenderState();
                mc.getModelManager().getBlockStateModelSet().get(state).collectParts(
                    mc.level,
                    pos,
                    state,
                    RandomSource.create(),
                    model.setupModel(new Matrix4f(), false)
                );

                // 计算光照
                BlockPos lightPos = new BlockPos(
                    pos.getX(),
                    pos.getY() + Math.max(1, Math.round(offsetY)),
                    pos.getZ()
                );
                int skyLight = Math.max(0, mc.level.getBrightness(LightLayer.SKY, lightPos));
                int blockLight = mc.level.getBrightness(LightLayer.BLOCK, lightPos);
                int packedLight = (skyLight << 20) | (blockLight << 4);

                EXTRACTED_MODELS.put(pos, new ExtractedModel(model, offsetY, packedLight));
            } catch (Exception ignored) {
                // 模型提取失败时跳过
            }
        }
    }

    @SubscribeEvent
    public static void onRender(SubmitCustomGeometryEvent event) {
        if (EXTRACTED_MODELS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            EXTRACTED_MODELS.clear();
            return;
        }

        double camX = event.getLevelRenderState().cameraRenderState.pos.x();
        double camY = event.getLevelRenderState().cameraRenderState.pos.y();
        double camZ = event.getLevelRenderState().cameraRenderState.pos.z();

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector nodeCollector = event.getSubmitNodeCollector();

        for (Map.Entry<BlockPos, ExtractedModel> entry : EXTRACTED_MODELS.entrySet()) {
            BlockPos pos = entry.getKey();
            ExtractedModel em = entry.getValue();

            poseStack.pushPose();
            poseStack.translate(
                pos.getX() - camX,
                pos.getY() - camY + em.offsetY,
                pos.getZ() - camZ
            );

            // 微扩避免 z-fighting
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(1.0005f, 1.000f, 1.0005f);
            poseStack.translate(-0.5, -0.5, -0.5);

            em.model.submit(poseStack, nodeCollector, 1, OverlayTexture.NO_OVERLAY, em.packedLight);

            poseStack.popPose();
        }
    }

    /** 提取后的模型及渲染参数 */
    private record ExtractedModel(BlockModelRenderState model, float offsetY, int packedLight) {
    }
}
