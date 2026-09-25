package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.ScaledGuiItemAtlases;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GatewayItemCacheProbe {
    private static @Nullable Set<Object> renderers;
    private static long allocations;
    private static int frames;

    public static void sample(Minecraft client) {
        try {
            var guiField = GameRenderer.class.getDeclaredField("guiRenderer");
            guiField.setAccessible(true);
            Object gui = guiField.get(client.gameRenderer);
            var oversized = GuiRenderer.class.getDeclaredField("oversizedItemRenderers");
            oversized.setAccessible(true);
            var instances = new HashSet<Object>(((Map<?, ?>) oversized.get(gui)).values());
            if (instances.isEmpty()) return;
            var scaledField = GuiRenderer.class.getDeclaredField("anvilcraft$scaledItems");
            scaledField.setAccessible(true);
            var scaled = (ScaledGuiItemAtlases) scaledField.get(gui);
            long currentAllocations = scaled == null ? 0 : scaled.allocations();
            if (renderers == null) {
                renderers = instances;
                allocations = currentAllocations;
            } else if (!renderers.equals(instances) || allocations != currentAllocations) {
                throw new IllegalStateException("Stable gallery recreated its item renderer or scaled atlas");
            }
            frames++;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    public static void verify() {
        if (frames < 3 || renderers == null) throw new IllegalStateException("Insufficient live item-cache samples: " + frames);
        AnvilCraft.LOGGER.info("PORT_GATEWAY_CACHE_REUSE_PASSED: {} frames, {} renderers, {} atlas allocations",
            frames, renderers.size(), allocations);
    }
}
