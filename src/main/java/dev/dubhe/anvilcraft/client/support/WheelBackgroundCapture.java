package dev.dubhe.anvilcraft.client.support;

import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelFrostedBackground;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.mixin.accessor.GuiBlurBoundaryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

import java.util.IdentityHashMap;
import java.util.Map;

public final class WheelBackgroundCapture {
    private static final Map<WheelFrostedBackground, Size> PENDING = new IdentityHashMap<>();
    private static boolean refreshing;
    private static boolean ownsBlurBoundary;

    private WheelBackgroundCapture() {
    }

    public static int sourceDiscColor(int color) {
        // Preserve the observed 1.21 disc opacity after its 0x66 frosted overlay, without an unbound sampler.
        float transmission = 1.0F - 0x66 / 255.0F;
        int alpha = 255 - Math.round((255 - ARGB.alpha(color)) * transmission);
        float premultiplier = ARGB.alpha(color) * transmission / alpha;
        return ARGB.color(alpha, Math.round(ARGB.red(color) * premultiplier),
            Math.round(ARGB.green(color) * premultiplier), Math.round(ARGB.blue(color) * premultiplier));
    }

    public static void beginFrame() {
        WheelBackgroundCapture.PENDING.clear();
        WheelBackgroundCapture.ownsBlurBoundary = false;
    }

    public static void afterDisc(GuiGraphicsExtractor graphics) {
        if (WheelBackgroundCapture.ownsBlurBoundary) return;
        var state = Minecraft.getInstance().gameRenderer.getGameRenderState().guiRenderState;
        if (((GuiBlurBoundaryAccessor) state).anvilcraft$firstStratumAfterBlur() != Integer.MAX_VALUE) return;
        graphics.nextStratum();
        graphics.blurBeforeThisStratum();
        WheelBackgroundCapture.ownsBlurBoundary = true;
    }

    public static boolean atBlurBoundary() {
        if (!WheelBackgroundCapture.ownsBlurBoundary) return false;
        WheelBackgroundCapture.ownsBlurBoundary = false;
        WheelBackgroundCapture.refresh();
        return true;
    }

    public static boolean isRefreshing() {
        return WheelBackgroundCapture.refreshing;
    }

    public static void request(WheelFrostedBackground background, int width, int height) {
        WheelBackgroundCapture.PENDING.put(background, new Size(width, height));
    }

    public static void forget(WheelFrostedBackground background) {
        WheelBackgroundCapture.PENDING.remove(background);
    }

    public static void beforeGui() {
        if (!WheelBackgroundCapture.ownsBlurBoundary) WheelBackgroundCapture.refresh();
    }

    private static void refresh() {
        if (WheelBackgroundCapture.PENDING.isEmpty()) return;
        var target = Minecraft.getInstance().getMainRenderTarget();
        WheelBackgroundCapture.refreshing = true;
        try {
            for (var entry : WheelBackgroundCapture.PENDING.entrySet()) {
                // A resize must not close a texture view already referenced by this frame's extracted GUI.
                if (entry.getValue().width() != target.width || entry.getValue().height() != target.height) continue;
                try {
                    entry.getKey().capture();
                } catch (RuntimeException error) {
                    AnvilCraft.LOGGER.error("Unable to capture the wheel background", error);
                }
            }
        } finally {
            WheelBackgroundCapture.refreshing = false;
            WheelBackgroundCapture.PENDING.clear();
        }
    }

    private record Size(int width, int height) {
    }
}
