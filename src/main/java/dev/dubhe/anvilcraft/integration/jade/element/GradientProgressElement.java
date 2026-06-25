package dev.dubhe.anvilcraft.integration.jade.element;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.ui.Color;
import snownee.jade.impl.ui.ProgressOverlayElement;

public class GradientProgressElement extends ProgressOverlayElement {
    private final int color;

    public GradientProgressElement(int color) {
        this.color = color;
    }

    @Override
    public @Nullable Component getNarration() {
        return null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        Color color = Color.rgb(this.color);
        int lighter = Color.hsl(color.getHue(), color.getSaturation(), color.getLightness() * 0.7, color.getOpacity()).toInt();
        if (this.floatingRect == null) {
            graphics.fillGradient(
                this.getX(),
                this.getY(),
                this.getX() + this.width,
                this.getY() + this.height / 2,
                lighter,
                this.color
            );
            graphics.fillGradient(
                this.getX(),
                this.getY() + this.height / 2,
                this.getX() + this.width,
                this.getY() + this.height,
                lighter,
                this.color
            );
        } else {
            graphics.fillGradient(
                Mth.floor(this.floatingRect.getX()),
                Mth.floor(this.floatingRect.getY()),
                Mth.ceil(this.floatingRect.getX() + this.floatingRect.getWidth()),
                Mth.ceil(this.floatingRect.getY() + this.floatingRect.getHeight() / 2),
                lighter,
                this.color
            );
            graphics.fillGradient(
                Mth.floor(this.floatingRect.getX()),
                Mth.floor(this.floatingRect.getY() + this.floatingRect.getHeight() / 2),
                Mth.ceil(this.floatingRect.getX() + this.floatingRect.getWidth()),
                Mth.ceil(this.floatingRect.getY() + this.floatingRect.getHeight()),
                this.color,
                lighter
            );
        }
    }
}
