package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class TexturedButton extends Button {
    private final int texYDiff;
    private final int textureWidth;
    private final int textureHeight;
    private final ResourceLocation texture;
    /** 右键处理器；为 null 时该按钮不响应右键 */
    private final @Nullable OnPress rightPress;

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
        this(x, y, width, height, texture, texYDiff, textureWidth, textureHeight, onPress, Component.empty());
    }

    public TexturedButton(
        int x,
        int y,
        int width,
        int height,
        ResourceLocation texture,
        int texYDiff,
        int textureWidth,
        int textureHeight,
        OnPress onPress,
        Component message
    ) {
        this(x, y, width, height, texture, texYDiff, textureWidth, textureHeight, onPress, message, null);
    }

    public TexturedButton(
        int x,
        int y,
        int width,
        int height,
        ResourceLocation texture,
        int texYDiff,
        int textureWidth,
        int textureHeight,
        OnPress onPress,
        @Nullable OnPress rightPress
    ) {
        this(x, y, width, height, texture, texYDiff, textureWidth, textureHeight, onPress, Component.empty(), rightPress);
    }

    public TexturedButton(
        int x,
        int y,
        int width,
        int height,
        ResourceLocation texture,
        int texYDiff,
        int textureWidth,
        int textureHeight,
        OnPress onPress,
        Component message,
        @Nullable OnPress rightPress
    ) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);

        this.texYDiff = texYDiff;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.texture = texture;
        this.rightPress = rightPress;
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == 0 || button == 1 && this.rightPress != null;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (button == 1 && this.rightPress != null) {
            this.rightPress.onPress(this);
            return;
        }
        // 默认实现即调用两参数版本（Button.onClick → onPress）
        this.onClick(mouseX, mouseY);
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
        if (this.isHovered && !this.getMessage().getString().isEmpty()) {
            graphics.renderTooltip(Minecraft.getInstance().font, this.getMessage(), mouseX, mouseY);
        }
    }

    public OnPress getOnPress() {
        return this.onPress;
    }
}
