package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.MagnetUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MagnetAnvilAnimation {
    private static final Map<BlockPos, Animation> ANIMATIONS = new ConcurrentHashMap<>();
    private static long ticks;

    private MagnetAnvilAnimation() {
    }

    public static void start(Vec3 start, BlockPos end, BlockState state) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || !AnvilCraft.CLIENT_CONFIG.displayAnvilAnimation || start.y >= end.getY()
            || level.getBlockState(end) != state) return;
        double time = ticks + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Animation previous = ANIMATIONS.get(end);
        Vec3 origin = previous == null ? start : previous.position(time);
        ANIMATIONS.put(end.immutable(), new Animation(origin, end.getBottomCenter(), state, time));
        client.levelRenderer.setBlockDirty(BlockPos.containing(start), true);
        client.levelRenderer.setBlockDirty(end, true);
    }

    // Read by chunk meshing workers; this never changes the world's block state or collision shape.
    public static boolean hidesBlock(BlockPos pos, BlockState state) {
        Animation animation = ANIMATIONS.get(pos);
        return animation != null && animation.state == state && animation.hidden;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.isPaused()) return;
        ticks++;
        update(client, ticks);
    }

    private static void update(Minecraft client, double time) {
        ClientLevel level = client.level;
        if (level == null) return;
        ANIMATIONS.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            Animation animation = entry.getValue();
            boolean invalid = !AnvilCraft.CLIENT_CONFIG.displayAnvilAnimation || !level.hasChunkAt(pos)
                || level.getBlockState(pos) != animation.state || !MagnetUtil.hasMagnetism(level, pos);
            double age = time - animation.startTime;
            if (invalid || age >= animation.duration) {
                if (animation.hidden) {
                    animation.hidden = false;
                    client.levelRenderer.setBlockDirty(pos, true);
                }
                // Keep the model at its final position while the section rebuild is handed back to terrain rendering.
                return invalid || age >= animation.duration + 2;
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) ANIMATIONS.clear();
    }

    @SubscribeEvent
    public static void onRender(SubmitCustomGeometryEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || ANIMATIONS.isEmpty()) return;
        double time = ticks + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        update(client, time);
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        for (var entry : ANIMATIONS.entrySet()) {
            Animation animation = entry.getValue();
            Vec3 position = animation.position(time);
            BlockPos pos = BlockPos.containing(position);
            MovingBlockRenderState renderState = new MovingBlockRenderState();
            renderState.blockState = animation.state;
            renderState.blockPos = pos;
            renderState.randomSeedPos = entry.getKey();
            renderState.biome = level.getBiome(pos);
            renderState.cardinalLighting = level.cardinalLighting();
            renderState.lightEngine = level.getLightEngine();
            renderState.modelData = level.getModelData(entry.getKey());
            var pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(position.x - camera.x - 0.5, position.y - camera.y, position.z - camera.z - 0.5);
            event.getSubmitNodeCollector().submitMovingBlock(pose, renderState);
            pose.popPose();
        }
    }

    private static final class Animation {
        private final Vec3 start;
        private final Vec3 end;
        private final BlockState state;
        private final double startTime;
        private final double duration;
        private volatile boolean hidden = true;

        private Animation(Vec3 start, Vec3 end, BlockState state, double startTime) {
            this.start = start;
            this.end = end;
            this.state = state;
            this.startTime = startTime;
            this.duration = Mth.clamp(2 + (end.y - start.y - 1) / 3, 2, 4);
        }

        private Vec3 position(double time) {
            double progress = Mth.clamp((time - this.startTime) / this.duration, 0, 1);
            return this.start.lerp(this.end, progress);
        }
    }
}
