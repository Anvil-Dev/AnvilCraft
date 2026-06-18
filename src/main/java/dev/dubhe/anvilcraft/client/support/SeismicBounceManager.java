package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.client.event.SeismicBounceRenderEventListener;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 震波方块弹跳管理器。
 *
 * <p>负责管理弹跳数据，模型提取与渲染由 {@link SeismicBounceRenderEventListener} 处理。</p>
 */
public class SeismicBounceManager {

    private static final SeismicBounceManager INSTANCE = new SeismicBounceManager();

    private static final int BOUNCE_DURATION_TICKS = 16;
    private static final float MAX_AMPLITUDE = 0.85f;
    private static final int CENTER_EXCLUSION_RADIUS = 1;

    /**
     * 黑名单方块
     */
    private static boolean isAttachmentBlock(BlockState state) {
        return state.is(BlockTags.BUTTONS)
            || state.is(BlockTags.PRESSURE_PLATES)
            || state.is(BlockTags.ALL_SIGNS)
            || state.is(BlockTags.BANNERS)
            || state.is(BlockTags.FLOWERS)
            || state.is(BlockTags.SAPLINGS)
            || state.is(BlockTags.CROPS)
            || state.is(BlockTags.RAILS)
            || state.is(BlockTags.CLIMBABLE)
            || state.is(BlockTags.CANDLES)
            || state.is(BlockTags.WOOL_CARPETS)
            || state.is(BlockTags.FIRE)
            || state.is(BlockTags.WALL_POST_OVERRIDE)
            || state.is(BlockTags.CAVE_VINES)
            || state.is(BlockTags.FLOWER_POTS)
            || state.is(BlockTags.CANDLE_CAKES)
            || state.is(BlockTags.ANVIL)
            || state.is(Blocks.BEDROCK)
            || state.is(Blocks.REDSTONE_BLOCK)
            || state.getBlock() instanceof RedStoneWireBlock;
    }

    @Getter
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
                if (Math.abs(dx) <= CENTER_EXCLUSION_RADIUS && Math.abs(dz) <= CENTER_EXCLUSION_RADIUS) continue;

                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);

                if (!state.isAir()
                    && state.getRenderShape() == RenderShape.MODEL
                    && level.isEmptyBlock(pos.above())
                    && level.getBlockEntity(pos) == null
                    && !isAttachmentBlock(state)) {
                    float amplitude = MAX_AMPLITUDE * (1.0f - (float) dist / radius)
                        * (0.8f + this.tesselateRandom.nextFloat() * 0.4f);
                    amplitude = Math.max(amplitude, 0.15f);
                    int delay = (dist - 2) + this.tesselateRandom.nextInt(3) - 1;
                    this.startBounce(pos, amplitude, Math.max(delay, 0));
                }
            }
        }
    }

    public void startBounce(BlockPos pos, float amplitude, int startDelay) {
        BounceData existing = this.activeBounces.get(pos);
        if (existing != null) {
            existing.reset(amplitude, startDelay);
        } else {
            this.activeBounces.put(pos, new BounceData(amplitude, startDelay));
        }
    }

    public void tick() {
        if (this.activeBounces.isEmpty()) return;

        this.activeBounces.values().forEach(data -> data.remainingTicks--);
        this.activeBounces.entrySet().removeIf(entry -> entry.getValue().remainingTicks <= 0);
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
            int elapsed = (this.totalTicks + this.startDelay) - this.remainingTicks;
            int active = elapsed - this.startDelay;
            if (active <= 0) return 0f;
            return Math.min((float) active / this.totalTicks, 1.0f);
        }

        public float getRenderOffsetY(float partialTick) {
            int elapsed = (this.totalTicks + this.startDelay) - this.remainingTicks;
            int active = elapsed - this.    startDelay;
            if (active < 0) return 0f;

            float progress = (float) active / this.totalTicks;
            progress += partialTick / this.totalTicks;
            progress = Math.min(progress, 1.0f);

            double bounce = Math.sin(progress * Math.PI)
                * Math.pow(1.0 - progress, 0.5);

            return (float) (this.amplitude * bounce);
        }
    }
}
