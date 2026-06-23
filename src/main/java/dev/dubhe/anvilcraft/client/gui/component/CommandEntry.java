package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class CommandEntry extends TexturedButton {
    @Getter
    private final MutableComponent text;
    private final OnPress onPress;

    public CommandEntry(int x, int y, int width, int height, Component text, OnPress onPress) {
        super(x, y, width, height, AnvilCraft.of(""), 0, 0, 0, (btn) -> {});
        this.onPress = onPress;
        this.text = text.copy();
        Style style = this.text.getStyle();
        style.withBold(false);
        this.text.setStyle(style);
    }

    public CommandEntry(int x, int y, int width, int height, Component text, OnPress onPress, Component message) {
        super(x, y, width, height, AnvilCraft.of(""), 0, 0, 0, (btn) -> {}, message);
        this.onPress = onPress;
        this.text = text.copy();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) {
            return;
        }
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        if (this.isFocused()) {
            graphics.horizontalLine(this.getX(), this.getX() + width, this.getY(), -1);
            graphics.horizontalLine(this.getX(), this.getX() + width, this.getY() + height, -1);
            graphics.verticalLine(this.getX(), this.getY(), this.getY() + height, -1);
            graphics.verticalLine(this.getX() + width, this.getY(), this.getY() + height, -1);
            graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + width - 1, this.getY() + height - 1, -16777216);
        }

        Font font = Minecraft.getInstance().font;
        int width = font.width(this.text);
        if (width > this.width - 4) {
            if (this.isHovered()) {
                graphics.drawScrollingString(
                    graphics.textRenderer(),
                    font,
                    this.text,
                    this.getX() + 3, this.getX() + this.width - 3,
                    this.getY() + 2
                );
            } else {
                Style style = this.text.getStyle();
                String string = this.text.getString();
                while (font.width(string + "...") > this.width - 4) {
                    string = string.substring(0, string.length() - 1);
                }
                graphics.text(font, Component.literal(string + "...").setStyle(style), this.getX() + 3, this.getY() + 3, -1, false);
            }
        } else {
            graphics.text(font, this.text, this.getX() + 3, this.getY() + 3, -1, false);
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {

    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.onPress(this);
    }

    public interface OnPress {
        void onPress(CommandEntry button);
    }
}
