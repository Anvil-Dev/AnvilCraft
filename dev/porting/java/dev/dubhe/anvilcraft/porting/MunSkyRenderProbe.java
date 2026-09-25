package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSkyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunSkyRenderProbe {
    private static @Nullable Object cachedBuffers;
    private static int reusedFrames;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void extract(ExtractLevelRenderStateEvent event) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("anvilcraft.portMunSkyScene")
            || !CelestialTravelManager.MUN_LEVEL.equals(event.getLevel().dimension())) return;
        var clock = event.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
            .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
        var field = MunSkyRenderer.class.getDeclaredField("FRAME");
        field.setAccessible(true);
        var frame = (MunSkyRenderer.Frame) event.getRenderState().getRenderData((ContextKey<?>) field.get(null));
        if (frame == null) throw new IllegalStateException("Native custom sky did not extract a Mun frame");
        if (frame.time() != event.getLevel().clockManager().getTotalTicks(clock)) {
            throw new IllegalStateException("Moon frame did not use the synchronized world clock");
        }
        if (((dev.dubhe.anvilcraft.client.renderer.mun.MunClockRate) event.getLevel().clockManager())
            .anvilcraft$overworldClockRate() == 0 && frame.partialTime() != 0) {
            throw new IllegalStateException("Stopped world clock continued interpolating Moon motion");
        }
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        cachedBuffers = null;
        reusedFrames = 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void frame(RenderFrameEvent.Post event) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("anvilcraft.portMunSkyScene")) return;
        var client = Minecraft.getInstance();
        if (client.level == null || client.getOverlay() != null) return;
        Object current = buffers();
        if (current == null) return;
        if (cachedBuffers != null && cachedBuffers != current) {
            throw new IllegalStateException("Moon buffers were reallocated between frames");
        }
        cachedBuffers = current;
        if (++reusedFrames == 100) AnvilCraft.LOGGER.info("PORT_MUN_SKY_BUFFER_REUSE_PASSED: 100 frames");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shutdown(GameShuttingDownEvent event) throws ReflectiveOperationException {
        if (Boolean.getBoolean("anvilcraft.portMunSkyScene") && buffers() != null) {
            throw new IllegalStateException("Moon GPU buffers survived shutdown");
        }
    }

    private static @Nullable Object buffers() throws ReflectiveOperationException {
        var field = MunSkyRenderer.class.getDeclaredField("buffers");
        field.setAccessible(true);
        return field.get(null);
    }
}
