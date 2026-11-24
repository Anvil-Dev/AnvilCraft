package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 按口袋图集绘制边框、填充、图标与槽位。坐标由 {@link PocketLayout} 以 GUI 左上角为原点计算。
 */
public final class PocketRenderer {
    private static final ResourceLocation TEXTURE = AnvilCraft.of("textures/gui/misc/equipment/pocket.png");
    private static final int TEXTURE_WIDTH = 38;
    private static final int TEXTURE_HEIGHT = 28;
    private static final int FILL_COLOR = 0xFFC6C6C6;
    private final GuiGraphics graphics;

    private PocketRenderer(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public static void render(GuiGraphics graphics, PocketLayout layout, int left, int top) {
        if (layout.isEmpty()) {
            return;
        }
        PocketRenderer renderer = new PocketRenderer(graphics);
        int x = left + layout.x();
        int y = top + layout.y();
        renderer.drawBorder(layout, x, y);
        renderer.drawFill(layout, x, y);
        renderer.blit(2, 2, 16, 7, layout.iconX() + left, layout.iconY() + top);
        for (int index = 0; index < layout.slots(); index++) {
            renderer.blit(18, 2, 18, 18, layout.slotBackgroundX(index) + left, layout.slotBackgroundY(index) + top);
        }
        graphics.flush();
    }

    private void drawBorder(PocketLayout layout, int x, int y) {
        if (layout.bottomStepped()) {
            this.drawBottomStepBorder(layout, x, y);
            return;
        }
        if (layout.stepped()) {
            this.drawSideStepBorder(layout, x, y);
            return;
        }
        final int width = layout.width();
        final int height = layout.height();
        this.blit(0, 0, 2, 2, x, y);
        this.blitScaled(2, 0, 34, 2, x + 2, y, width - 4, 2);
        this.blit(36, 0, 2, 2, x + width - 2, y);
        this.blitScaled(0, 2, 2, 22, x, y + 2, 2, height - 6);
        this.blitScaled(36, 2, 2, 22, x + width - 2, y + 2, 2, height - 6);
        this.blit(0, 24, 2, 4, x, y + height - 4);
        this.blitScaled(4, 24, 30, 4, x + 2, y + height - 4, width - 4, 4);
        this.blit(36, 24, 2, 4, x + width - 2, y + height - 4);
        this.graphics.flush();
    }

    private void drawSideStepBorder(PocketLayout layout, int x, int y) {
        final int width = layout.width();
        final int height = layout.height();
        final int stepY = y + layout.stepY() - layout.y();
        final int stepX = x + layout.stepBorderX() - layout.x();
        this.blit(0, 0, 2, 2, x, y);
        this.blitScaled(2, 0, 34, 2, x + 2, y, width - 4, 2);
        this.blit(36, 0, 2, 2, x + width - 2, y);
        if (layout.side() == 1) {
            this.blitScaled(0, 2, 2, 22, x, y + 2, 2, height - 6);
            this.blitScaled(36, 2, 2, 22, x + width - 2, y + 2, 2, stepY - (y + 2));
            this.blit(36, 24, 2, 4, x + width - 2, stepY);
            this.blitScaled(4, 24, 30, 4, stepX + 2, stepY, x + width - 2 - (stepX + 2), 4);
            this.blit(34, 24, 2, 4, stepX, stepY);
            this.blitScaled(36, 2, 2, 22, stepX, stepY + 4, 2, height - 8 - (stepY - y));
            this.blit(36, 24, 2, 4, stepX, y + height - 4);
            this.blitScaled(4, 24, 30, 4, x + 2, y + height - 4, stepX - (x + 2), 4);
            this.blit(0, 24, 2, 4, x, y + height - 4);
        } else {
            this.blitScaled(36, 2, 2, 22, x + width - 2, y + 2, 2, height - 6);
            this.blitScaled(0, 2, 2, 22, x, y + 2, 2, stepY - (y + 2));
            this.blit(0, 24, 2, 4, x, stepY);
            this.blitScaled(4, 24, 30, 4, x + 2, stepY, stepX - (x + 2), 4);
            this.blit(2, 24, 2, 4, stepX, stepY);
            this.blitScaled(0, 2, 2, 22, stepX, stepY + 4, 2, height - 8 - (stepY - y));
            this.blit(0, 24, 2, 4, stepX, y + height - 4);
            this.blitScaled(4, 24, 30, 4, stepX + 2, y + height - 4, x + width - 2 - (stepX + 2), 4);
            this.blit(36, 24, 2, 4, x + width - 2, y + height - 4);
        }
        this.graphics.flush();
    }

    private void drawBottomStepBorder(PocketLayout layout, int x, int y) {
        final int width = layout.width();
        final int height = layout.height();
        final int stepY = y + layout.stepY() - layout.y();
        final int stepX = x + layout.stepBorderX() - layout.x();
        final int bottom = y + height - 4;
        this.blit(0, 0, 2, 2, x, y);
        this.blitScaled(2, 0, 34, 2, x + 2, y, width - 4, 2);
        this.blit(36, 0, 2, 2, x + width - 2, y);
        if (layout.side() == 1) {
            this.blitScaled(0, 2, 2, 22, x, y + 2, 2, height - 6);
            this.blitScaled(36, 2, 2, 22, x + width - 2, y + 2, 2, stepY - (y + 2));
            this.blit(36, 24, 2, 4, x + width - 2, stepY);
            this.blitScaled(4, 24, 30, 4, stepX + 2, stepY, x + width - 2 - (stepX + 2), 4);
            this.blit(34, 24, 2, 4, stepX, stepY);
            this.blitScaled(36, 2, 2, 22, stepX, stepY + 4, 2, height - 8 - (stepY - y));
            this.blit(36, 24, 2, 4, stepX, bottom);
            this.blitScaled(4, 24, 30, 4, x + 2, bottom, stepX - (x + 2), 4);
            this.blit(0, 24, 2, 4, x, bottom);
        } else {
            this.blitScaled(36, 2, 2, 22, x + width - 2, y + 2, 2, height - 6);
            this.blitScaled(0, 2, 2, 22, x, y + 2, 2, stepY - (y + 2));
            this.blit(0, 24, 2, 4, x, stepY);
            this.blitScaled(4, 24, 30, 4, x + 2, stepY, stepX - (x + 2), 4);
            this.blit(2, 24, 2, 4, stepX, stepY);
            this.blitScaled(0, 2, 2, 22, stepX, stepY + 4, 2, height - 8 - (stepY - y));
            this.blit(0, 24, 2, 4, stepX, bottom);
            this.blitScaled(4, 24, 30, 4, stepX + 2, bottom, x + width - 2 - (stepX + 2), 4);
            this.blit(36, 24, 2, 4, x + width - 2, bottom);
        }
        this.graphics.flush();
    }

    private void drawFill(PocketLayout layout, int x, int y) {
        final int width = layout.width();
        final int height = layout.height();
        final int left = x + 2;
        final int top = y + 2;
        final int right = x + width - 2;
        final int bottom = y + height - 4;
        if (layout.bottomStepped() || layout.stepped()) {
            int stepY = y + layout.stepY() - layout.y();
            int stepX = x + layout.stepBorderX() - layout.x();
            this.fill(left, top, right, stepY);
            if (layout.side() == 1) {
                this.fill(left, stepY, stepX, bottom);
            } else {
                this.fill(stepX + 2, stepY, right, bottom);
            }
            this.graphics.flush();
            return;
        }
        this.fill(left, top, right, bottom);
        this.graphics.flush();
    }

    private void blit(int u, int v, int width, int height, int x, int y) {
        this.graphics.blit(
            PocketRenderer.TEXTURE,
            x,
            y,
            width,
            height,
            u,
            v,
            width,
            height,
            PocketRenderer.TEXTURE_WIDTH,
            PocketRenderer.TEXTURE_HEIGHT
        );
    }

    private void blitScaled(
        int u,
        int v,
        int sourceWidth,
        int sourceHeight,
        int x,
        int y,
        int width,
        int height
    ) {
        this.graphics.blit(
            PocketRenderer.TEXTURE,
            x,
            y,
            width,
            height,
            u,
            v,
            sourceWidth,
            sourceHeight,
            PocketRenderer.TEXTURE_WIDTH,
            PocketRenderer.TEXTURE_HEIGHT
        );
    }

    private void fill(int x0, int y0, int x1, int y1) {
        this.graphics.fill(x0, y0, x1, y1, PocketRenderer.FILL_COLOR);
    }
}
