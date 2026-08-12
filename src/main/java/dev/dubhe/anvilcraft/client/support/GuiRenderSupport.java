package dev.dubhe.anvilcraft.client.support;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class GuiRenderSupport {
    public static void centeredEllipsisText(GuiGraphics graphics, Font font, Component text, int x, int y, int max) {
        GuiRenderSupport.centeredEllipsisText(graphics, font, text, x, y, max, 0xFFFFFFFF);
    }

    public static void centeredEllipsisText(GuiGraphics graphics, Font font, Component text, int x, int y, int max, int color) {
        int width = font.width(text);
        if (width < max) { // 小于最大宽度，需要居中
            graphics.drawCenteredString(font, text, x + Math.floorDiv(max, 2), y, color);
        } else if (width > max) { // 大于最大宽度，需要截断
            FormattedText truncated = FormattedText.composite(
                font.substrByWidth(text, max - font.width(CommonComponents.ELLIPSIS)),
                CommonComponents.ELLIPSIS
            );
            graphics.drawString(font, Language.getInstance().getVisualOrder(truncated), x, y, color, true);
        } else { // 等于最大宽度，直接渲染
            graphics.drawString(font, text, x, y, color, true);
        }
    }
}
