package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TexturedButton extends Button {
    private final int texYDiff;
    private final int textureWidth;
    private final int textureHeight;
    private final ResourceLocation texture;

    public TexturedButton(
        int x,
        int y,
        int width,
        int height,
        ResourceLocation texture,
        int texYDiff,
        int textureWidth,
        int textureHeight,
        OnPress onPress
    ) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);

        this.texYDiff = texYDiff;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.texture = texture;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        int offsetV = 0;
        if (this.isHovered) {
            offsetV = texYDiff;
        }
        graphics.blit(texture, this.getX(), this.getY(), 0, offsetV, width, height, textureWidth, textureHeight);
    }
}
