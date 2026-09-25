package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunSurfaceRenderProbe {
    private static @Nullable Object cachedUniforms;
    private static int frames;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void frame(RenderFrameEvent.Post event) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("anvilcraft.portMunSurfaceScene")) return;
        if ((boolean) field("worldPass") || (boolean) field("lightmapPass")) {
            throw new IllegalStateException("Moon rendering scope leaked beyond the world pass");
        }
        Object current = field("uniforms");
        if (current == null) return;
        if (cachedUniforms != null && cachedUniforms != current) {
            throw new IllegalStateException("Moon surface uniforms were reallocated between frames");
        }
        cachedUniforms = current;
        if (++frames == 100) AnvilCraft.LOGGER.info("PORT_MUN_SURFACE_SCOPE_REUSE_PASSED: 100 frames");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shutdown(GameShuttingDownEvent event) throws ReflectiveOperationException {
        if (Boolean.getBoolean("anvilcraft.portMunSurfaceScene") && field("uniforms") != null) {
            throw new IllegalStateException("Moon surface uniforms survived shutdown");
        }
    }

    private static @Nullable Object field(String name) throws ReflectiveOperationException {
        var field = MunSurfaceRenderer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
