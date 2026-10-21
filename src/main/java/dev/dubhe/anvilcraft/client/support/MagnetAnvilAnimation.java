package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.MagnetUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MagnetAnvilAnimation {
    private static final Map<BlockPos, Animation> ANIMATIONS = new ConcurrentHashMap<>();
    private static long ticks;

    private MagnetAnvilAnimation() {
    }

    public static void start(Vec3 start, BlockPos end, BlockState state, boolean requiresMagnet) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || !AnvilCraft.CLIENT_CONFIG.effects.displayAnvilAnimation || start.y >= end.getY()
            || level.getBlockState(end) != state || (requiresMagnet && !MagnetUtil.hasMagnetism(level, end))) return;
        double time = ticks + client.getTimer().getGameTimeDeltaPartialTick(false);
        Animation previous = ANIMATIONS.get(end);
        Vec3 origin = previous == null ? start : previous.position(time);
        ANIMATIONS.put(end.immutable(), new Animation(origin, end.getBottomCenter(), state, time, requiresMagnet));
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

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) ANIMATIONS.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || ANIMATIONS.isEmpty()) return;
        double time = ticks + client.getTimer().getGameTimeDeltaPartialTick(client.isPaused());
        update(client, time);
        if (ANIMATIONS.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        BlockRenderDispatcher dispatcher = client.getBlockRenderer();
        RandomSource random = RandomSource.create();
        for (Map.Entry<BlockPos, Animation> entry : ANIMATIONS.entrySet()) {
            Animation animation = entry.getValue();
            if (animation.state.getRenderShape() != RenderShape.MODEL) continue;
            Vec3 position = animation.position(time);
            BlockPos pos = BlockPos.containing(position);
            long seed = animation.state.getSeed(entry.getKey());
            poseStack.pushPose();
            poseStack.translate(position.x - camera.x - 0.5, position.y - camera.y, position.z - camera.z - 0.5);
            var model = dispatcher.getBlockModel(animation.state);
            for (RenderType renderType : model.getRenderTypes(animation.state, random, ModelData.EMPTY)) {
                dispatcher.getModelRenderer().tesselateBlock(
                    level,
                    model,
                    animation.state,
                    pos,
                    poseStack,
                    buffers.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                    false,
                    random,
                    seed,
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    renderType
                );
            }
            poseStack.popPose();
        }
    }

    private static void update(Minecraft client, double time) {
        ClientLevel level = client.level;
        if (level == null) return;
        ANIMATIONS.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            Animation animation = entry.getValue();
            boolean invalid = !AnvilCraft.CLIENT_CONFIG.effects.displayAnvilAnimation || !level.hasChunkAt(pos)
                || level.getBlockState(pos) != animation.state
                || (animation.requiresMagnet && !MagnetUtil.hasMagnetism(level, pos));
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

    private static final class Animation {
        private final Vec3 start;
        private final Vec3 end;
        private final BlockState state;
        private final double startTime;
        private final double duration;
        private final boolean requiresMagnet;
        private volatile boolean hidden = true;

        private Animation(Vec3 start, Vec3 end, BlockState state, double startTime, boolean requiresMagnet) {
            this.start = start;
            this.end = end;
            this.state = state;
            this.startTime = startTime;
            this.requiresMagnet = requiresMagnet;
            this.duration = Mth.clamp(2 + (end.y - start.y - 1) / 3, 2, 4);
        }

        private Vec3 position(double time) {
            double progress = Mth.clamp((time - this.startTime) / this.duration, 0, 1);
            return this.start.lerp(this.end, progress);
        }
    }
}
