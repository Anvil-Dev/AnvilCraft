package dev.dubhe.anvilcraft.client.renderer.shake;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端震波弹跳动画管理器。
 * <p>
 * 当巨型铁砧撼地时，记录受影响的方块位置并驱动其弹跳动画。
 * 在{@code RenderLevelStageEvent}中以叠加渲染的方式实现视觉弹跳，
 * 不改变BlockState，不修改原始方块在区块中的渲染。
 * </p>
 */
public class SeismicBounceManager {

    private static final SeismicBounceManager INSTANCE = new SeismicBounceManager();

    /** 每个弹跳方块的持续时间（tick） */
    private static final int BOUNCE_DURATION_TICKS = 16; // ~0.8秒

    /** 弹跳最大高度（方块单位） */
    private static final float MAX_AMPLITUDE = 2.0f;

    /** 活跃的弹跳动画 */
    private final Map<BlockPos, BounceData> activeBounces = new ConcurrentHashMap<>();

    private SeismicBounceManager() {
    }

    public static SeismicBounceManager getInstance() {
        return INSTANCE;
    }

    // ======================================================================
    //  公共 API
    // ======================================================================

    /**
     * 触发一片区域内的方块弹跳。
     *
     * @param center 震波中心（地面高度）
     * @param radius 影响半径（方块）
     */
    public void triggerShock(BlockPos center, int radius) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;

                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);

                if (!state.isAir() && state.getRenderShape() == RenderShape.MODEL) {
                    float amplitude = MAX_AMPLITUDE * (1.0f - (float) dist / radius);
                    amplitude = Math.max(amplitude, 0.4f);
                    startBounce(pos, amplitude);
                }
            }
        }
    }

    /**
     * 开始单个方块的弹跳动画。
     */
    public void startBounce(BlockPos pos, float amplitude) {
        BounceData existing = activeBounces.get(pos);
        if (existing != null) {
            existing.reset(amplitude);
        } else {
            activeBounces.put(pos, new BounceData(amplitude));
        }
    }

    /**
     * 每个客户端 tick 调用，更新动画状态。
     */
    public void tick() {
        if (activeBounces.isEmpty()) return;

        activeBounces.values().forEach(data -> data.remainingTicks--);
        activeBounces.entrySet().removeIf(entry -> entry.getValue().remainingTicks <= 0);
    }

    /**
     * 渲染所有弹跳方块。
     * 在 {@code RenderLevelStageEvent} 中调用。
     */
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, double camX, double camY, double camZ) {
        if (activeBounces.isEmpty()) return;

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        for (Map.Entry<BlockPos, BounceData> entry : activeBounces.entrySet()) {
            BlockPos pos = entry.getKey();
            BounceData data = entry.getValue();
            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;

            float offsetY = data.getRenderOffsetY(partialTick);
            if (Math.abs(offsetY) < 0.001f) continue;

            // 用弹跳后的高度算光照，避免侧面对比原位置AO而显得偏暗
            BlockPos lightPos = new BlockPos(
                pos.getX(),
                pos.getY() + Math.max(1, (int) Math.round(offsetY)),
                pos.getZ()
            );

            poseStack.pushPose();
            poseStack.translate(
                pos.getX() - camX,
                pos.getY() - camY + offsetY,
                pos.getZ() - camZ
            );

            var model = dispatcher.getBlockModel(state);
            for (var renderType : model.getRenderTypes(state, RandomSource.create(state.getSeed(pos)), ModelData.EMPTY)) {
                dispatcher.getModelRenderer().tesselateBlock(
                    level,
                    model,
                    state,
                    lightPos,
                    poseStack,
                    bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                    false,
                    RandomSource.create(),
                    state.getSeed(pos),
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    renderType
                );
            }

            poseStack.popPose();
        }
    }

    // ======================================================================
    //  弹跳数据
    // ======================================================================

    public static class BounceData {
        private int totalTicks;
        @Getter
        private int remainingTicks;
        @Getter
        private float amplitude;

        BounceData(float amplitude) {
            this.totalTicks = BOUNCE_DURATION_TICKS;
            this.remainingTicks = BOUNCE_DURATION_TICKS;
            this.amplitude = amplitude;
        }

        void reset(float newAmplitude) {
            this.totalTicks = BOUNCE_DURATION_TICKS;
            this.remainingTicks = BOUNCE_DURATION_TICKS;
            this.amplitude = newAmplitude;
        }

        public float getProgress() {
            return 1.0f - (float) remainingTicks / totalTicks;
        }

        /**
         * 获取当前帧的 Y 轴偏移量。
         * 单向阻尼弹跳曲线：快速弹起 → 回落。
         */
        public float getRenderOffsetY(float partialTick) {
            float progress = getProgress();
            progress += partialTick / totalTicks;
            progress = Math.min(progress, 1.0f);

            float bounce = (float) (
                Math.sin(progress * Math.PI)
                * Math.pow(1.0 - progress, 0.5)
            );

            return amplitude * bounce;
        }
    }
}
