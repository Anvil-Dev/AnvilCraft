package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
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

    public static final int STEP = 50;

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
        this.value = FluidRateSlider.clampSnap(value);
    }

    public void setExactValue(int value) {
        this.value = Math.clamp(value, 0, FluidPipeNetwork.maxSpeed());
    }

    public void setValueWithUpdate(int value) {
        this.setValue(value);
        this.update();
    }

    public void step(int direction) {
        if (direction < 0 && this.value == FluidPipeNetwork.maxSpeed() && this.value % STEP != 0) {
            this.setValueWithUpdate(this.value - this.value % STEP);
        } else {
            this.setValueWithUpdate(this.value + STEP * direction);
        }
    }

    private void update() {
        this.callback.onValueChange(this.value);
    }

    private static int clampSnap(double value) {
        int max = FluidPipeNetwork.maxSpeed();
        double clamped = Math.clamp(value, 0d, max);
        int lower = (int) (clamped / STEP) * STEP;
        int upper = Math.min(lower + STEP, max);
        return clamped - lower < upper - clamped ? lower : upper;
    }

    private double proportion() {
        return (double) this.value / FluidPipeNetwork.maxSpeed();
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
        this.value = clampSnap(offset * FluidPipeNetwork.maxSpeed());
        this.update();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) return;
        boolean hovered = this.scrolling || this.isInKnob(mouseX, mouseY);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            FluidRateSlider.SLIDER,
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
