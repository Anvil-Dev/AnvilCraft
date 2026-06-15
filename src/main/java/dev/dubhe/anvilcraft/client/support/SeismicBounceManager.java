package dev.dubhe.anvilcraft.client.support;

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

public class SeismicBounceManager {

    private static final SeismicBounceManager INSTANCE = new SeismicBounceManager();

    private static final int BOUNCE_DURATION_TICKS = 16;
    private static final float MAX_AMPLITUDE = 0.85f;
    private static final int CENTER_EXCLUSION_RADIUS = 1;

    private final Map<BlockPos, BounceData> activeBounces = new ConcurrentHashMap<>();
    private final RandomSource tesselateRandom = RandomSource.create();

    private SeismicBounceManager() {
    }

    public static SeismicBounceManager getInstance() {
        return INSTANCE;
    }

    public void triggerShock(BlockPos center, int radius) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // 跳过中心 3×3 区域，避免铁砧正下方方块弹跳
                if (Math.abs(dx) <= CENTER_EXCLUSION_RADIUS && Math.abs(dz) <= CENTER_EXCLUSION_RADIUS) continue;

                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);

                if (!state.isAir()
                    && state.getRenderShape() == RenderShape.MODEL
                    && level.isEmptyBlock(pos.above())
                    && level.getBlockEntity(pos) == null) {
                    // 振幅随机扰动 0.8~1.2 倍，使弹跳高度有自然差异
                    float amplitude = MAX_AMPLITUDE * (1.0f - (float) dist / radius)
                        * (0.8f + tesselateRandom.nextFloat() * 0.4f);
                    amplitude = Math.max(amplitude, 0.15f);
                    // 同圈延迟增加随机偏移 -1~+1 tick，让波纹更自然
                    int delay = (dist - 2) + tesselateRandom.nextInt(3) - 1;
                    startBounce(pos, amplitude, Math.max(delay, 0));
                }
            }
        }
    }

    public void startBounce(BlockPos pos, float amplitude, int startDelay) {
        BounceData existing = activeBounces.get(pos);
        if (existing != null) {
            existing.reset(amplitude, startDelay);
        } else {
            activeBounces.put(pos, new BounceData(amplitude, startDelay));
        }
    }

    public void tick() {
        if (activeBounces.isEmpty()) return;

        activeBounces.values().forEach(data -> data.remainingTicks--);
        activeBounces.entrySet().removeIf(entry -> entry.getValue().remainingTicks <= 0);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, double camX, double camY, double camZ) {
        if (activeBounces.isEmpty()) return;

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();
        for (Map.Entry<BlockPos, BounceData> entry : activeBounces.entrySet()) {
            BounceData data = entry.getValue();
            BlockPos pos = entry.getKey();

            float offsetY = data.getRenderOffsetY(partialTick);
            if (Math.abs(offsetY) < 0.001f) continue;

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;

            lightPos.set(pos.getX(), pos.getY() + Math.max(1, Math.round(offsetY)), pos.getZ());

            poseStack.pushPose();
            poseStack.translate(
                pos.getX() - camX,
                pos.getY() - camY + offsetY,
                pos.getZ() - camZ
            );

            // 微扩 0.1% 避免与原方块 z-fighting
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(1.0005f, 1.000f, 1.0005f);
            poseStack.translate(-0.5, -0.5, -0.5);

            var model = dispatcher.getBlockModel(state);
            long seed = state.getSeed(pos);
            for (var renderType : model.getRenderTypes(state, RandomSource.create(seed), ModelData.EMPTY)) {
                dispatcher.getModelRenderer().tesselateBlock(
                    level,
                    model,
                    state,
                    lightPos,
                    poseStack,
                    bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                    false,
                    tesselateRandom,
                    seed,
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    renderType
                );
            }

            poseStack.popPose();
        }
    }

    public static class BounceData {
        private int totalTicks;
        private int startDelay;
        @Getter
        private int remainingTicks;
        @Getter
        private float amplitude;

        BounceData(float amplitude, int startDelay) {
            this.reset(amplitude, startDelay);
        }

        void reset(float newAmplitude, int newStartDelay) {
            this.totalTicks = BOUNCE_DURATION_TICKS;
            this.startDelay = newStartDelay;
            this.remainingTicks = BOUNCE_DURATION_TICKS + newStartDelay;
            this.amplitude = newAmplitude;
        }

        public float getProgress() {
            int elapsed = (totalTicks + startDelay) - remainingTicks;
            int active = elapsed - startDelay;
            if (active <= 0) return 0f;
            return Math.min((float) active / totalTicks, 1.0f);
        }

        public float getRenderOffsetY(float partialTick) {
            int elapsed = (totalTicks + startDelay) - remainingTicks;
            int active = elapsed - startDelay;
            if (active < 0) return 0f;

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
