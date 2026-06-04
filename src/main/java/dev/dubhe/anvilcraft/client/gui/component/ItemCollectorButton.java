package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ItemCollectorButton extends Button {

    private final Identifier texture;

    /// 物品收集器 screen 的加减按钮
    public ItemCollectorButton(int x, int y, String variant, OnPress onPress) {
        super(x, y, 10, 10, Component.literal(""), onPress, var -> Component.literal(variant));
        this.texture = SharedTextures.textureGui("machine/item_collector/button_%s".formatted(variant));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.renderTexture(graphics, this.texture, this.getX(), this.getY(), 0, 0, 10, this.width, this.height, 10, 20);
    }

    public void renderTexture(
        GuiGraphicsExtractor graphics,
        Identifier texture,
        int x,
        int y,
        int puOffset,
        int pvOffset,
        int textureDifference,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        int i = pvOffset;
        if (this.isHovered()) {
            i += textureDifference;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }
}
