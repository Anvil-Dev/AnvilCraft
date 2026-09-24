package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlProgram;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL42C;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunShadowReceiverProbe {
    private static long lastFrame;
    private static int frames;
    private static boolean notified;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portMunStandardScene")
            || AnvilCraft.CLIENT_CONFIG.munLightingQuality != MunLightingQuality.STANDARD) return;
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        var receiver = (MunShadowReceiver) field(MunSurfaceRenderer.class, "SHADOWS");
        long frame = (Long) field(receiver, "frame");
        if (frame == lastFrame || ((Map<?, ?>) field(receiver, "programs")).isEmpty()) return;
        lastFrame = frame;
        check(!(Boolean) field(receiver, "active"), "Receiver resources escaped the world pass");
        var history = field(receiver, "history");
        check(!(Boolean) field(history, "active"), "History writes escaped the world pass");
        int[] textures = (int[]) field(receiver, "savedTextures");
        int[] samplers = (int[]) field(receiver, "savedSamplers");
        int previous = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        try {
            for (int i = 0; i < 10; i++) {
                int unit = i + 4;
                GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
                check(GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D) == textures[i], "Texture leaked at shadow unit " + unit);
                check(GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, unit) == samplers[i], "Sampler leaked at shadow unit " + unit);
            }
        } finally {
            GL13C.glActiveTexture(previous);
        }
        if ((Boolean) field(receiver, "historyEnabled")) {
            int[][] saved = (int[][]) field(history, "savedImages");
            for (int unit = 0; unit < 2; unit++) {
                check(GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, unit) == saved[unit][0], "History image binding leaked");
            }
        }
        for (Object key : ((Map<?, ?>) field(receiver, "programs")).keySet()) {
            int id = ((GlProgram) key).getProgramId();
            int count = GL20C.glGetUniformLocation(id, "ShadowCount");
            check(count >= 0 && GL20C.glGetUniformi(id, count) == 3, "World shader did not receive three shadow cascades");
        }
        if (!notified && frames >= 100) {
            var pos = client.player.blockPosition().above(12);
            var original = client.level.getBlockState(pos);
            try {
                client.level.setBlock(pos, original.isAir() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                check(((Set<?>) field(receiver.map, "invalidated")).contains(ChunkPos.containing(pos).pack()),
                    "Block change notification did not invalidate the receiver cache");
            } finally {
                client.level.setBlock(pos, original, 2);
            }
            var chunk = ChunkPos.containing(pos);
            var boundary = new BlockPos(chunk.getMaxBlockX(), pos.getY(), chunk.getMinBlockZ() + 8);
            var state = client.level.getBlockState(boundary);
            client.level.sendBlockUpdated(boundary, state, state, 8);
            var invalidated = (Set<?>) field(receiver.map, "invalidated");
            check(invalidated.contains(chunk.pack()) && invalidated.contains(new ChunkPos(chunk.x() + 1, chunk.z()).pack()),
                "Model update did not invalidate the adjacent chunk");
            notified = true;
        }
        if (++frames == 100) AnvilCraft.LOGGER.info("PORT_MUN_RECEIVER_SCOPE_PASSED: 100 world frames and all ten texture slots");
    }

    @SubscribeEvent
    public static void shutdown(GameShuttingDownEvent event) {
        if (!Boolean.getBoolean("anvilcraft.portMunStandardScene")) return;
        check(frames >= 100 && notified, "Receiver scope or change-notification checks did not finish");
        AnvilCraft.LOGGER.info("PORT_MUN_RECEIVER_PASSED: frames={}, block and model notifications", frames);
    }

    private static Object field(Object owner, String name) {
        try {
            Class<?> type = owner instanceof Class<?> clazz ? clazz : owner.getClass();
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return Objects.requireNonNull(field.get(owner instanceof Class<?> ? null : owner));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
