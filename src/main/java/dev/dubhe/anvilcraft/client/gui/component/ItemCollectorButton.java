package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ItemCollectorButton extends Button {

    private final ResourceLocation texture;

    /**
     * 物品收集器 screen 的加减按钮
     */
    public ItemCollectorButton(int x, int y, String variant, OnPress onPress) {
        super(x, y, 10, 10, Component.literal(""), onPress, (var) -> Component.literal(variant));
        texture = SharedTextures.textureGui("machine/button_%s".formatted(variant));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTexture(guiGraphics, texture, this.getX(), this.getY(), 0, 0, 10, this.width, this.height, 10, 20);
    }

    public void renderTexture(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
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
        RenderSystem.enableDepthTest();
        guiGraphics.blit(texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }
}
