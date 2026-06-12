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
 *
 *  <p>
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
    private static final float MAX_AMPLITUDE = 0.85f; // 实际弹起约 0.6 格

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
                    amplitude = Math.max(amplitude, 0.15f);
                    // 距离越远延迟越大，形成由内向外扩散的波纹效果
                    int delay = (dist - 2) * 1; // 每格 1 tick 延迟，快速扩散
                    startBounce(pos, amplitude, delay);
                }
            }
        }
    }

    /**
     * 开始单个方块的弹跳动画。
     *
     * @param pos      方块位置
     * @param amplitude 弹跳最大高度
     * @param startDelay 开始延迟（tick），用于实现波纹扩散
     */
    public void startBounce(BlockPos pos, float amplitude, int startDelay) {
        BounceData existing = activeBounces.get(pos);
        if (existing != null) {
            existing.reset(amplitude, startDelay);
        } else {
            activeBounces.put(pos, new BounceData(amplitude, startDelay));
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
        /** 活跃弹跳持续 tick 数 */
        private int totalTicks;
        /** 开始前的延迟 tick 数（扩散波纹用） */
        private int startDelay;
        /** 剩余总 tick（含延迟） */
        @Getter
        private int remainingTicks;
        @Getter
        private float amplitude;

        BounceData(float amplitude, int startDelay) {
            this.totalTicks = BOUNCE_DURATION_TICKS;
            this.startDelay = startDelay;
            this.remainingTicks = BOUNCE_DURATION_TICKS + startDelay;
            this.amplitude = amplitude;
        }

        void reset(float newAmplitude, int newStartDelay) {
            this.totalTicks = BOUNCE_DURATION_TICKS;
            this.startDelay = newStartDelay;
            this.remainingTicks = BOUNCE_DURATION_TICKS + newStartDelay;
            this.amplitude = newAmplitude;
        }

        /**
         * 弹跳进度 [0, 1]。
         * 延迟期间返回 0，延迟结束后从 0 逐渐到 1。
         */
        public float getProgress() {
            int elapsed = (totalTicks + startDelay) - remainingTicks;
            int active = elapsed - startDelay;
            if (active <= 0) return 0f;
            return Math.min((float) active / totalTicks, 1.0f);
        }

        /**
         * 获取当前帧的 Y 轴偏移量。
         * 单向阻尼弹跳曲线：快速弹起 → 回落。
         * 延迟期间返回 0。
         */
        public float getRenderOffsetY(float partialTick) {
            int elapsed = (totalTicks + startDelay) - remainingTicks;
            int active = elapsed - startDelay;
            if (active < 0) return 0f; // 仍在延迟中

            float progress = (float) active / totalTicks;
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
