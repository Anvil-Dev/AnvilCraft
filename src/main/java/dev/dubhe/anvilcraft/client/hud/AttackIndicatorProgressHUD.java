package dev.dubhe.anvilcraft.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class AttackIndicatorProgressHUD {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace(
        "hud/crosshair_attack_indicator_background"
    );
    private static final Identifier PROGRESS = Identifier.withDefaultNamespace(
        "hud/crosshair_attack_indicator_progress"
    );
    private static final int WIDTH = 16;
    private static final int HEIGHT = 4;

    private AttackIndicatorProgressHUD() {
    }

    public static void render(GuiGraphicsExtractor graphics, float progress) {
        float clamped = Math.clamp(progress, 0.0F, 1.0F);
        int x = graphics.guiWidth() / 2 - 8;
        int y = graphics.guiHeight() / 2 + 9;
        int progressWidth = Math.min(WIDTH, (int) (clamped * 17.0F));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, WIDTH, HEIGHT);
        if (progressWidth > 0) {
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                PROGRESS,
                WIDTH,
                HEIGHT,
                0,
                0,
                x,
                y,
                progressWidth,
                HEIGHT
            );
        }
    }
}
