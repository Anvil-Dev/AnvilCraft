package dev.dubhe.anvilcraft.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class AttackIndicatorProgressHUD {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace(
        "hud/crosshair_attack_indicator_background");
    private static final ResourceLocation PROGRESS = ResourceLocation.withDefaultNamespace(
        "hud/crosshair_attack_indicator_progress");
    private static final int WIDTH = 16;
    private static final int HEIGHT = 4;

    private AttackIndicatorProgressHUD() {
    }

    public static void render(GuiGraphics graphics, float progress) {
        float clamped = Math.clamp(progress, 0.0F, 1.0F);
        int x = graphics.guiWidth() / 2 - 8;
        int y = graphics.guiHeight() / 2 - 7 + 16;
        int progressWidth = Math.min(WIDTH, (int) (clamped * 17.0F));
        graphics.blitSprite(BACKGROUND, x, y, WIDTH, HEIGHT);
        if (progressWidth > 0) {
            graphics.blitSprite(PROGRESS, WIDTH, HEIGHT, 0, 0, x, y, progressWidth, HEIGHT);
        }
    }
}
