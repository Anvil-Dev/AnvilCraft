package dev.dubhe.anvilcraft.util;

import lombok.experimental.UtilityClass;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

@UtilityClass
public class RenderUtil {
    public static int drawScrollingShadowlessString(
        GuiGraphics guiGraphics,
        Font font,
        Component text,
        int minX, int maxX,
        int y, int color
    ) {
        int maxWidth = maxX - minX;
        int textWidth = font.width(text.getVisualOrderText());
        if (textWidth <= maxWidth) {
            return guiGraphics.drawString(font, text, minX, y, color);
        } else {
            RenderUtil.renderScrollingString(guiGraphics, font, text, (minX + maxX) / 2, minX, y, maxX, y + font.lineHeight, color);
            return maxWidth;
        }
    }

    public static void renderScrollingString(
        GuiGraphics guiGraphics, Font font, Component text, int centerX, int minX, int minY, int maxX, int maxY, int color
    ) {
        int i = font.width(text);
        int j = (minY + maxY - 9) / 2 + 1;
        int k = maxX - minX;
        if (i > k) {
            int l = i - k;
            double d0 = (double) Util.getMillis() / 1000.0;
            double d1 = Math.max((double) l * 0.5, 3.0);
            double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d0 / d1)) / 2.0 + 0.5;
            double d3 = Mth.lerp(d2, 0.0, l);
            guiGraphics.enableScissor(minX, minY, maxX, maxY);
            guiGraphics.drawString(font, text, minX - (int) d3, j, color, false);
            guiGraphics.disableScissor();
        } else {
            int i1 = Mth.clamp(centerX, minX + i / 2, maxX - i / 2);
            guiGraphics.drawCenteredString(font, text, i1, j, color);
        }
    }
}
