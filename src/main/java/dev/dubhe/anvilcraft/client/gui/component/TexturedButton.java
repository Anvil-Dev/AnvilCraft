package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TexturedButton extends Button {
    private final int texYDiff;
    private final int textureWidth;
    private final int textureHeight;
    private final Identifier texture;

    public TexturedButton(
        int x,
        int y,
        int width,
        int height,
        Identifier texture,
        int texYDiff,
        int textureWidth,
        int textureHeight,
        OnPress onPress
    ) {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);

        this.texYDiff = texYDiff;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.texture = texture;
    }

    public TexturedButton(
        int x,
        int y,
        int width,
        int height,
        Identifier texture,
        int texYDiff,
        int textureWidth,
        int textureHeight,
        OnPress onPress,
        Component message
    ) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);

        this.texYDiff = texYDiff;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.texture = texture;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) return;
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        int offsetV = 0;
        if (this.isHovered) {
            offsetV = this.texYDiff;
        }
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            this.texture,
            this.getX(),
            this.getY(),
            0,
            offsetV,
            this.width,
            this.height,
            this.textureWidth,
            this.textureHeight
        );
    }

    public void renderContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractContents(graphics, mouseX, mouseY, a);
    }
}
