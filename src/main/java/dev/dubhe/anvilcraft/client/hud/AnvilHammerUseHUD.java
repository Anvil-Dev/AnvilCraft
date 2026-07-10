package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class AnvilHammerUseHUD {
    private static final int SEGMENTS = 48;
    private static final int BACKGROUND_COLOR = 0x99D0D0D0;
    private static final int PROGRESS_COLOR = 0xD8A9F59B;

    private AnvilHammerUseHUD() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null) return;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof AnvilHammerItem)) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(minecraft.isPaused());
        int remainingTicks = player.getUseItemRemainingTicks();
        float progress = Mth.clamp(
            (AnvilHammerItem.PORTABLE_ANVIL_USE_TICKS - remainingTicks + partialTick)
            / AnvilHammerItem.PORTABLE_ANVIL_USE_TICKS,
            0.0F,
            1.0F
        );
        if (progress <= 0.0F) return;

        renderRing(graphics, 24, minecraft.getWindow().getGuiScaledHeight() - 24, progress);
    }

    private static void renderRing(GuiGraphics graphics, int centerX, int centerY, float progress) {
        drawRing(graphics, centerX, centerY, SEGMENTS, BACKGROUND_COLOR);
        drawRing(graphics, centerX, centerY, Mth.ceil(SEGMENTS * progress), PROGRESS_COLOR);
    }

    private static void drawRing(GuiGraphics graphics, int centerX, int centerY, int segments, int color) {
        for (int segment = 0; segment < segments; segment++) {
            double angle = -Math.PI / 2.0D + Math.PI * 2.0D * segment / SEGMENTS;
            drawPoint(graphics, centerX, centerY, angle, 10, color);
            drawPoint(graphics, centerX, centerY, angle, 8, color);
        }
    }

    private static void drawPoint(GuiGraphics graphics, int centerX, int centerY, double angle, int radius, int color) {
        int x = centerX + Mth.floor(Math.cos(angle) * radius);
        int y = centerY + Mth.floor(Math.sin(angle) * radius);
        graphics.fill(x, y, x + 2, y + 2, color);
    }
}
