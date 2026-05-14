package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.util.Callback;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class Slider extends AbstractWidget {
    public static final Identifier SLIDER = SharedTextures.textureGui("misc/slider_like/slider");

    @Setter
    @Getter
    private int min;

    @Setter
    @Getter
    private int max;

    private double proportion;
    private final int posX;
    private final int posY;
    private final int length;
    private final Function<Double, Double> valueFunction;
    private final Function<Integer, Double> argFunction;
    public final Callback<Integer> callback;
    private boolean scroll = false;
    public static boolean scrolling = false;

    /**
     * 构建一个Slider
     *
     * @param x        X
     * @param y        Y
     * @param min      最小值
     * @param max      最大值
     * @param length   长度
     * @param callback 更新回调
     */
    public Slider(
        int x,
        int y,
        int min,
        int max,
        int length,
        Function<Double, Double> valueFunction,
        Function<Integer, Double> argFunction,
        Callback<Integer> callback
    ) {
        super(x, y, length, 8, Component.literal("Slider"));
        this.posX = x;
        this.posY = y;
        this.min = min;
        this.max = max;
        this.length = length;
        this.valueFunction = valueFunction;
        this.argFunction = argFunction;
        this.callback = callback;
    }

    public Slider(
        int x,
        int y,
        int min,
        int max,
        int length,
        Callback<Integer> callback
    ) {
        this(
            x,
            y,
            min,
            max,
            length,
            i -> Slider.defaultValueFunction(i, min, max),
            i -> Slider.defaultArgFunction(i, min, max),
            callback
        );
    }

    public double getProportion() {
        return Math.clamp(this.proportion, 0.0, 1.0);
    }

    public void setProportion(double proportion) {
        this.proportion = Math.clamp(proportion, 0.0, 1.0);
    }

    public static double defaultValueFunction(double proportion, int min, int max) {
        return (max - min) * proportion + min;
    }

    public static double defaultArgFunction(int value, int min, int max) {
        if (value == 0) return Math.clamp(((double) value - min) / (max - min), 0.0, 1.0);
        double v = (Math.log(Math.abs(value)) / Math.log(2)) + 1;
        return Math.clamp(((value >= 0 ? v : -v) - min) / (max - min), 0.0, 1.0);
    }

    public void setValue(int value) {
        this.proportion = this.argFunction.apply(value);
    }

    public int getValue() {
        int v = (int) Math.round(this.valueFunction.apply(this.proportion));
        if (v == 0) return 0;
        return v > 0 ? (int) Math.pow(2, Math.abs(v - 1)) : -(int) Math.pow(2, Math.abs(v + 1));
    }

    public int getAddValue(int value) {
        if (Math.abs(value) < 4) return 1;
        return (int) Math.pow(2, Math.floor(Math.log(Math.abs(value)) / Math.log(2)) - 2);
    }

    /**
     * 设置 Value 并更新
     */
    public void setValueWithUpdate(int value) {
        this.setValue(value);
        this.update();
    }

    private void update() {
        if (this.callback != null) this.callback.onValueChange(this.getValue());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        if (this.isInSlider(event.x(), event.y())) {
            scrolling = true;
            return;
        }
        scrolling = false;
    }

    @Override
    public void onDrag(MouseButtonEvent event, double dx, double dy) {
        super.onDrag(event, dx, dy);
        if (scrolling || this.scroll) {
            if (scrolling) {
                this.scroll = true;
                scrolling = false;
            }
            double offset = (dx - 8 - this.posX) / this.length;
            this.setProportion(offset);
        }
        if (this.scroll) this.update();
    }

    public void onReleased() {
        if (this.scroll) this.update();
        this.scroll = false;
        scrolling = false;
    }

    protected boolean isInSlider(double mouseX, double mouseY) {
        int offsetX = this.posX + (int) (this.length * this.getProportion());
        return mouseX > offsetX && mouseX < offsetX + 16 && mouseY > this.posY && mouseY < this.posY + 8;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) return;
        this.isHovered = this.isInSlider(mouseX, mouseY);
        double prop = this.getProportion();
        int offsetX = this.posX + (int) ((this.length) * prop);
        graphics.blit(SLIDER, offsetX, this.posY, 0, this.isHovered || this.scroll ? 8 : 0, 16, 8, 16, 16);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
