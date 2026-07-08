package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.util.Callback;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FluidRateSlider extends AbstractWidget {
    public static final Identifier SLIDER = SharedTextures.textureGui("misc/slider_like/slider");

    public static final int MAX = 2000;
    public static final int STEP = 50;
    private static final int TOTAL_STEPS = MAX / STEP;

    private final int posX;
    private final int posY;
    private final int length;
    private final Callback<Integer> callback;
    @Getter
    private int value;
    private boolean scrolling = false;

    public FluidRateSlider(int x, int y, int length, Callback<Integer> callback) {
        super(x, y, length, 8, Component.literal("FluidRateSlider"));
        this.posX = x;
        this.posY = y;
        this.length = length;
        this.callback = callback;
    }

    public void setValue(int value) {
        this.value = clampSnap(value);
    }

    public void setValueWithUpdate(int value) {
        this.setValue(value);
        this.update();
    }

    public void step(int direction) {
        this.setValueWithUpdate(this.value + STEP * direction);
    }

    private void update() {
        this.callback.onValueChange(this.value);
    }

    private static int clampSnap(int value) {
        int snapped = Math.round((float) value / STEP) * STEP;
        return Math.clamp(snapped, 0, MAX);
    }

    private double proportion() {
        return (double) this.value / MAX;
    }

    private int knobX() {
        return this.posX + (int) ((this.length - 16) * this.proportion());
    }

    private boolean isInKnob(double mouseX, double mouseY) {
        int knobX = this.knobX();
        return mouseX >= knobX && mouseX < knobX + 16 && mouseY >= this.posY && mouseY < this.posY + 8;
    }

    private boolean isInTrack(double mouseX, double mouseY) {
        return mouseX >= this.posX && mouseX < this.posX + this.length && mouseY >= this.posY && mouseY < this.posY + 8;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        if (!this.active || event.button() != 0 || !this.isInTrack(event.x(), event.y())) return;
        this.scrolling = true;
        this.applyMouse(event.x());
    }

    @Override
    public void onDrag(MouseButtonEvent event, double dx, double dy) {
        super.onDrag(event, dx, dy);
        if (!this.active || !this.scrolling) return;
        this.applyMouse(event.x());
    }

    public void onReleased() {
        this.scrolling = false;
    }

    private void applyMouse(double mouseX) {
        double offset = (mouseX - this.posX - 8.0) / (this.length - 16);
        int step = (int) Math.round(Math.clamp(offset, 0.0, 1.0) * TOTAL_STEPS);
        this.setValueWithUpdate(step * STEP);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) return;
        boolean hovered = this.scrolling || this.isInKnob(mouseX, mouseY);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            SLIDER,
            this.knobX(),
            this.posY,
            0,
            hovered ? 8 : 0,
            16,
            8,
            16,
            16
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
