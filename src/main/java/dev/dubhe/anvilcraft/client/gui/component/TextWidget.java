package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

public class TextWidget extends StringWidget {
    private final TextProvider provider;
    protected float alignX = 0.5F;
    protected RenderMode mode = RenderMode.CLIP;
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    public TextWidget(int x, int y, int width, int height, Font font, TextProvider provider) {
        super(x, y, width, height, Component.empty(), font);
        this.provider = provider;
    }

    @Override
    public Component getMessage() {
        return provider.get();
    }

    protected void horizontalAlignment(float horizontalAlignment) {
        this.alignX = horizontalAlignment;
    }

    public TextWidget alignLeft() {
        this.horizontalAlignment(0.0F);
        return this;
    }

    public TextWidget alignCenter() {
        this.horizontalAlignment(0.5F);
        return this;
    }

    public TextWidget alignRight() {
        this.horizontalAlignment(1.0F);
        return this;
    }

    public TextWidget setRenderMode(RenderMode mode) {
        this.mode = mode;
        return this;
    }

    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Component component = this.getMessage();
        Font font = this.getFont();
        switch (this.mode) {
            case SCROLLING -> {
                // For scrolling mode, render the text normally without scrolling animation
                int i = this.getWidth();
                int j = font.width(component);
                int k = this.getX() + Math.round(this.alignX * (float) (i - j));
                int l = this.getY() + (this.getHeight() - font.lineHeight) / 2;
                graphics.text(font, component.getVisualOrderText(), k, l, DEFAULT_COLOR);
            }
            case SCALED -> {
                float scaleX = this.getWidth() / (float) font.width(component);
                float scaleY = this.getHeight() / (float) font.lineHeight;

                if (scaleX >= 1 && scaleY >= 1) {
                    int k = this.getX() + Math.round(this.alignX * (float) (this.getWidth() - font.width(component)));
                    int l = this.getY() + (this.getHeight() - font.lineHeight) / 2;
                    graphics.text(font, component, k, l, DEFAULT_COLOR);
                    return;
                }

                if (scaleX < 1 && scaleY > 1) {
                    scaleY = 1;
                } else if (scaleY < 1 && scaleX > 1) {
                    scaleX = 1;
                }

                float offsetX = scaleX >= 1 ? this.alignX * (this.getWidth() - font.width(component)) : 0;
                float offsetY = scaleY >= 1 ? (this.getHeight() - font.lineHeight) / 2.0F : 0;
                Matrix3x2fStack poseStack = graphics.pose();
                poseStack.pushMatrix();
                poseStack.translate(this.getX() + offsetX, this.getY() + offsetY);
                poseStack.scale(scaleX, scaleY);
                graphics.text(font, component, 0, 0, DEFAULT_COLOR);
                poseStack.popMatrix();
            }
            default -> {
                int i = this.getWidth();
                int j = font.width(component);
                int k = this.getX() + Math.round(this.alignX * (float) (i - j));
                int l = this.getY() + (this.getHeight() - font.lineHeight) / 2;
                graphics.text(font, component.getVisualOrderText(), k, l, DEFAULT_COLOR);
            }
        }
    }

    /**
     * 获取Widget文字
     */
    @FunctionalInterface
    public interface TextProvider {
        Component get();
    }

    /**
     * 文字渲染模式
     */
    public enum RenderMode {
        /**
         * 默认模式
         */
        DEFAULT,
        /**
         * 默认，不对文本进行操作
         */
        CLIP,
        /**
         * 滚动模式，使文本左右移动
         */
        SCROLLING,
        /**
         * 缩放模式，若文本某方向超出设定范围，则将文本在该方向上缩放
         */
        SCALED,
    }
}
