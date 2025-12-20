package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.Callback;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class Slider extends AbstractWidget {
    public static final ResourceLocation SLIDER = AnvilCraft.of("textures/gui/container/slider/slider.png");

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
    protected final int tooltipMsDelay = 1;
    private long hoverOrFocusedStartTime;
    private boolean wasHoveredOrFocused;
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

    @SuppressWarnings("deprecation")
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        if (this.isInSlider(mouseX, mouseY)) {
            scrolling = true;
            return;
        }
        scrolling = false;
    }

    public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        super.onDrag(mouseX, mouseY, dragX, dragY);
        if (scrolling || this.scroll) {
            if (scrolling) {
                this.scroll = true;
                scrolling = false;
            }
            double offset = (mouseX - 8 - this.posX) / this.length;
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
        int offsetX = posX + (int) (length * this.getProportion());
        return mouseX > offsetX && mouseX < offsetX + 16 && mouseY > posY && mouseY < posY + 8;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        this.isHovered = this.isInSlider(mouseX, mouseY);
        this.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        this.updateTooltip();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        double prop = this.getProportion();
        int offsetX = posX + (int) ((length) * prop);
        guiGraphics.blit(SLIDER, offsetX, posY, 0, this.isHovered || this.scroll ? 8 : 0, 16, 8, 16, 16);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    private void updateTooltip() {
        if (this.getTooltip() == null) return;
        boolean bl = this.isHovered
            || this.isFocused()
            && Minecraft.getInstance().getLastInputType().isKeyboard();
        if (bl != this.wasHoveredOrFocused) {
            if (bl) this.hoverOrFocusedStartTime = Util.getMillis();
            this.wasHoveredOrFocused = bl;
        }
        Screen screen;
        if (bl
            && Util.getMillis() - this.hoverOrFocusedStartTime > (long) this.tooltipMsDelay
            && (screen = Minecraft.getInstance().screen) != null) {
            screen.setTooltipForNextRenderPass(this.getTooltip(), DefaultTooltipPositioner.INSTANCE, this.isFocused());
        }
    }
}
