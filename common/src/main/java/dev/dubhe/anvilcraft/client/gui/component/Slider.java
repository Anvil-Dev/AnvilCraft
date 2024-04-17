package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class Slider extends AbstractWidget {
    public static final ResourceLocation SLIDER = AnvilCraft.of("textures/gui/container/slider/slider.png");
    @Setter
    @Getter
    private int min;
    @Setter
    @Getter
    private int max;
    @Getter
    private int value;
    private final int posX;
    private final int posY;
    private final int length;
    public final SliderMenu.Update update;
    private int tooltipMsDelay;
    private long hoverOrFocusedStartTime;
    private boolean wasHoveredOrFocused;

    /**
     * @param x      X
     * @param y      Y
     * @param min    最小值
     * @param max    最大值
     * @param length 长度
     * @param update 更新回调
     */
    public Slider(int x, int y, int min, int max, int length, SliderMenu.Update update) {
        super(x, y, length, 8, Component.literal("Slider"));
        this.posX = x;
        this.posY = y;
        this.min = min;
        this.max = max;
        this.length = length;
        this.update = update;
    }

    public double getProportion() {
        return Math.max(0.0, Math.min(1.0, (double) (value - this.min) / (this.max - this.min)));
    }

    public void setProportion(double proportion) {
        this.value = (int) ((max - min) * proportion + min);
    }

    public void setValue(int value) {
        this.value = Math.max(this.min, Math.min(this.max, value));
    }

    /**
     * @param value 设置 Value 并更新
     */
    public void setValueWithUpdate(int value) {
        if (this.value == value) return;
        this.setValue(value);
        this.update();
    }

    private void update() {
        if (this.update != null) this.update.update(this.value);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            this.onDrag(mouseX, mouseY, dragX, dragY);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        if (!isInRange(mouseX, mouseY)) return;
        if (isInSlider(mouseX, mouseY)) {
            super.onClick(mouseX, mouseY);
            return;
        }
        double offset = 16.0 / this.length;
        int offsetX = posX + (int) ((length - 16) * this.getProportion());
        if (mouseX < offsetX) this.setProportion(Math.max(0.0, this.getProportion() - offset));
        else this.setProportion(Math.min(1.0, this.getProportion() + offset));
        this.update();
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        super.onDrag(mouseX, mouseY, dragX, dragY);
        if (!isInSlider(mouseX, mouseY)) return;
        double offset = dragX - mouseX / this.length;
        this.setProportion(Math.max(0.0, Math.min(1.0, this.getProportion() + offset)));
        this.update();
    }

    protected boolean isInSlider(double mouseX, double mouseY) {
        int offsetX = posX + (int) ((length - 16) * this.getProportion());
        return mouseX > offsetX && mouseX < offsetX + 16 && mouseY > posY && mouseY < posY + 8;
    }

    protected boolean isInRange(double mouseX, double mouseY) {
        return mouseX > posX && mouseX < posX + length && mouseY > posY && mouseY < posY + 8;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        this.isHovered = this.isInRange(mouseX, mouseY);
        this.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        this.updateTooltip();
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int offsetX = posX + (int) ((length - 16) * this.getProportion());
        guiGraphics.blit(SLIDER, offsetX, posY, 0, this.isHovered ? 8 : 0, 16, 8, 16, 16);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    private void updateTooltip() {
        if (this.getTooltip() == null) return;
        boolean bl = this.isHovered || this.isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
        if (bl != this.wasHoveredOrFocused) {
            if (bl) this.hoverOrFocusedStartTime = Util.getMillis();
            this.wasHoveredOrFocused = bl;
        }
        Screen screen;
        if (bl
            && Util.getMillis() - this.hoverOrFocusedStartTime > (long) this.tooltipMsDelay
            && (screen = Minecraft.getInstance().screen) != null
        ) {
            screen.setTooltipForNextRenderPass(this.getTooltip(), this.createTooltipPositioner(), this.isFocused());
        }
    }

    @Override
    public void setTooltipDelay(int tooltipMsDelay) {
        super.setTooltipDelay(tooltipMsDelay);
        this.tooltipMsDelay = tooltipMsDelay;
    }
}
