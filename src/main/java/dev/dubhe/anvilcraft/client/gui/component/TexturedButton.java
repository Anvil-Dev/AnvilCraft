package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TexturedButton extends Button {
    private final int yDiffTex;
    private final int textureWidth;
    private final int textureHeight;
    private final ResourceLocation texture;

    public TexturedButton(
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        ResourceLocation texture,
        int yDiffTex,
        int textureWidth,
        int textureHeight,
        OnPress pOnPress
    ) {
        super(pX, pY, pWidth, pHeight, Component.empty(), pOnPress, DEFAULT_NARRATION);

        this.yDiffTex = yDiffTex;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.texture = texture;
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int pMouseX, int pMouseY, float pPartialTick) {
        int pVOffset = 0;
        if (this.isHovered) {
            pVOffset = yDiffTex;
        }
        gg.blit(
            texture,
            this.getX(),
            this.getY(),
            0,
            pVOffset,
            width,
            height,
            textureWidth,
            textureHeight
        );
    }
}
