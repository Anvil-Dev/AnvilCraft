package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.RenderUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        if (this.isFocused()) {
            graphics.hLine(this.getX(), this.getX() + width, this.getY(), -1);
            graphics.hLine(this.getX(), this.getX() + width, this.getY() + height, -1);
            graphics.vLine(this.getX(), this.getY(), this.getY() + height, -1);
            graphics.vLine(this.getX() + width, this.getY(), this.getY() + height, -1);
            graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + width - 1, this.getY() + height - 1, -16777216);
        }

        Font font = Minecraft.getInstance().font;
        int width = font.width(this.text);
        if (width > this.width - 4) {
            if (this.isHovered()) {
                RenderUtil.drawScrollingShadowlessString(
                    graphics,
                    font,
                    this.text,
                    this.getX() + 3, this.getX() + this.width - 3,
                    this.getY() + 2, -1
                );
            } else {
                Style style = this.text.getStyle();
                String string = this.text.getString();
                while (font.width(string + "...") > this.width - 4) {
                    string = string.substring(0, string.length() - 1);
                }
                graphics.drawString(font, Component.literal(string + "...").setStyle(style), this.getX() + 3, this.getY() + 3, -1, false);
            }
        } else {
            graphics.drawString(font, this.text, this.getX() + 3, this.getY() + 3, -1, false);
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {

    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(CommandEntry button);
    }
}
