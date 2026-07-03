package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.util.Callback;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 控制阀流速滑条：0 在最左、{@value #MAX} mB/tick 在最右，全程每档 {@value #STEP} mB/tick 线性均匀。
 *
 * <p>交互仿创造发电机 {@link Slider}：按住左键可拖动；仅当鼠标悬停在<b>滑块</b>上时才变高亮贴图。
 * 由屏幕在 {@code mouseClicked/mouseDragged/mouseReleased} 中显式转发到本组件。
 */
public class FluidRateSlider extends AbstractWidget {
    public static final ResourceLocation SLIDER = SharedTextures.textureGui("misc/slider_like/slider");

    public static final int MAX = 2000;
    public static final int STEP = 50;
    public static final int TOTAL_STEPS = MAX / STEP; // 40

    private final int posX;
    private final int posY;
    private final int length;
    private final Callback<Integer> callback;
    @Getter
    private int value;
    private boolean scroll = false;

    public FluidRateSlider(int x, int y, int length, Callback<Integer> callback) {
        super(x, y, length, 8, Component.literal("FluidRateSlider"));
        this.posX = x;
        this.posY = y;
        this.length = length;
        this.callback = callback;
    }

    /** 仅设置值（不回调）。 */
    public void setValue(int value) {
        this.value = clampSnap(value);
    }

    /** 设置值并回调。 */
    public void setValueWithUpdate(int value) {
        this.value = clampSnap(value);
        update();
    }

    /** 在当前值基础上增减一档（±{@value #STEP}）。 */
    public void step(int direction) {
        setValueWithUpdate(value + STEP * direction);
    }

    private void update() {
        callback.onValueChange(value);
    }

    /** 将任意值吸附到最近的合法档位值（{@value #STEP} 的整数倍，钳制到 [0,{@value #MAX}]）。 */
    private static int clampSnap(int v) {
        int snapped = Math.round((float) v / STEP) * STEP;
        return Math.max(0, Math.min(MAX, snapped));
    }

    private double proportion() {
        return (double) value / MAX;
    }

    /** 滑块当前左上角 X（滑块宽 16，在 [posX, posX+length-16] 间移动）。 */
    private int knobX() {
        return posX + (int) ((length - 16) * proportion());
    }

    /** 鼠标是否落在滑块矩形上。 */
    private boolean isInKnob(double mouseX, double mouseY) {
        int kx = knobX();
        return mouseX >= kx && mouseX < kx + 16 && mouseY >= posY && mouseY < posY + 8;
    }

    // ---- 由屏幕显式转发的鼠标事件 ----

    /** 左键按下：命中滑块则进入拖动，同时按位置更新一次。 */
    public void onClick(double mouseX, double mouseY) {
        if (isInKnob(mouseX, mouseY) || (mouseX >= posX && mouseX < posX + length && mouseY >= posY && mouseY < posY + 8)) {
            this.scroll = true;
            applyMouse(mouseX);
        }
    }

    public void onDrag(double mouseX, double mouseY) {
        if (this.scroll) {
            applyMouse(mouseX);
        }
    }

    public void onReleased() {
        this.scroll = false;
    }

    private void applyMouse(double mouseX) {
        double offset = (mouseX - this.posX - 8.0) / (this.length - 16);
        int step = (int) Math.round(Math.max(0.0, Math.min(1.0, offset)) * TOTAL_STEPS);
        setValueWithUpdate(step * STEP);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        // 仅当悬停在滑块上（或正在拖动）才用高亮贴图（下半张）
        boolean hover = this.scroll || isInKnob(mouseX, mouseY);
        guiGraphics.blit(SLIDER, knobX(), posY, 0, hover ? 8 : 0, 16, 8, 16, 16);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
