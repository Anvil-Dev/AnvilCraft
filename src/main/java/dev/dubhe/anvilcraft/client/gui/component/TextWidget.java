package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TextWidget extends StringWidget {
    private final TextProvider provider;
    protected float alignX = 0.5F;
    protected RenderMode mode = RenderMode.CLIP;

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

    @Override
    public TextWidget alignLeft() {
        this.horizontalAlignment(0.0F);
        return (TextWidget) super.alignLeft();
    }

    @Override
    public TextWidget alignCenter() {
        this.horizontalAlignment(0.5F);
        return (TextWidget) super.alignCenter();
    }

    @Override
    public TextWidget alignRight() {
        this.horizontalAlignment(1.0F);
        return (TextWidget) super.alignRight();
    }

    public TextWidget setRenderMode(RenderMode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component component = this.getMessage();
        Font font = this.getFont();
        switch (this.mode) {
            case CLIP -> super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            case SCROLLING -> guiGraphics.drawScrollingString(
                font, component,
                this.getX(), this.getX() + this.width, this.getY() + (this.getHeight() - 9) / 2,
                this.getColor());
            case SCALED -> {
                float scaleX = this.getWidth() / (float) font.width(component);
                float scaleY = this.getHeight() / (float) font.lineHeight;

                if (scaleX >= 1 && scaleY >= 1) {
                    super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                    return;
                }

                if (scaleX < 1 && scaleY > 1) scaleY = 1;
                else if (scaleY < 1 && scaleX > 1) scaleX = 1;

                double offsetX = scaleX >= 1 ? this.alignX * (this.getWidth() - font.width(component)) : 0;
                double offsetY = scaleY >= 1 ? (this.getHeight() - font.lineHeight) / 2.0 : 0;
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(this.getX() + offsetX, this.getY() + offsetY, 0);
                poseStack.scale(scaleX, scaleY, 1);
                guiGraphics.drawString(font, component, 0, 0, this.getColor());
                poseStack.popPose();
            }
            default -> {
                int i = this.getWidth();
                int j = font.width(component);
                int k = this.getX() + Math.round(this.alignX * (float) (i - j));
                int l = this.getY() + (this.getHeight() - font.lineHeight) / 2;
                guiGraphics.drawString(font, component.getVisualOrderText(), k, l, this.getColor());
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
