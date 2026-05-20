package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 简单的图标按钮，用于替代ItemCollectorButton
 */
public class SimpleIconButton extends Button {

    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;

    /**
     * 创建简单图标按钮
     * 
     * @param x 按钮X坐标
     * @param y 按钮Y坐标
     * @param variant 贴图变体（如 "minus", "add" 等）
     * @param onPress 点击事件
     */
    public SimpleIconButton(int x, int y, String variant, OnPress onPress) {
        super(x, y, 10, 10, Component.literal(""), onPress, DEFAULT_NARRATION);
        this.texture = SharedTextures.textureGui("machine/button_%s".formatted(variant));
        this.textureWidth = 10;
        this.textureHeight = 20;
    }

    /**
     * 创建简单图标按钮（自定义尺寸）
     * 
     * @param x 按钮X坐标
     * @param y 按钮Y坐标
     * @param width 按钮宽度
     * @param height 按钮高度
     * @param texture 贴图路径
     * @param texYDiff 悬停时的Y轴偏移
     * @param textureWidth 贴图宽度
     * @param textureHeight 贴图高度
     * @param onPress 点击事件
     */
    public SimpleIconButton(
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
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int pvOffset = 0;
        if (this.isHovered()) {
            pvOffset += this.textureHeight / 2;
        }
        RenderSystem.enableDepthTest();
        guiGraphics.blit(
            this.texture,
            this.getX(),
            this.getY(),
            0,
            pvOffset,
            this.width,
            this.height,
            this.textureWidth,
            this.textureHeight
        );
    }
}
