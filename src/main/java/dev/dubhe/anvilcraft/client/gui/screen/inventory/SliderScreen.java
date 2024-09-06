package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.Slider;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import dev.dubhe.anvilcraft.network.SliderUpdatePack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class SliderScreen extends AbstractContainerScreen<SliderMenu> {
    public static final ResourceLocation LOCATION = AnvilCraft.of("textures/gui/container/slider/background.png");
    public static final ResourceLocation BUTTON_MAX = AnvilCraft.of("textures/gui/container/slider/button_max.png");
    public static final ResourceLocation BUTTON_ADD = AnvilCraft.of("textures/gui/container/slider/button_add.png");
    public static final ResourceLocation BUTTON_MINUS = AnvilCraft.of("textures/gui/container/slider/button_minus.png");
    public static final ResourceLocation BUTTON_MIN = AnvilCraft.of("textures/gui/container/slider/button_min.png");
    private Slider slider = null;
    private EditBox value;

    /**
     * @param menu      菜单
     * @param inventory 背包
     * @param title     标题
     */
    public SliderScreen(SliderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 77;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        int offsetX = (this.width - this.imageWidth) / 2;
        int offsetY = (this.height - this.imageHeight) / 2;
        this.slider = new Slider(8 + offsetX, 31 + offsetY, 0, 16, 160, this::update);
        this.value = new EditBox(this.font, offsetX + 50, offsetY + 47, 76, 8, Component.literal("value"));
        this.value.setCanLoseFocus(false);
        this.value.setTextColor(-1);
        this.value.setTextColorUneditable(-1);
        this.value.setBordered(false);
        this.value.setMaxLength(50);
        this.value.setResponder(this::onValueInput);
        this.value.setValue("");
        ImageButton max = new ImageButton(
            152 + offsetX, 43 + offsetY,
            16, 16, 0, 0, 16, BUTTON_MAX, 16, 32,
            (btn) -> this.slider.setValueWithUpdate(slider.getMax())
        );
        ImageButton add = new ImageButton(
            134 + offsetX, 43 + offsetY,
            16, 16, 0, 0, 16, BUTTON_ADD, 16, 32,
            (btn) -> this.slider.setValueWithUpdate(Math.min(slider.getMax(), slider.getValue() + 1))
        );
        ImageButton min = new ImageButton(
            8 + offsetX, 43 + offsetY,
            16, 16, 0, 0, 16, BUTTON_MIN, 16, 32,
            (btn) -> this.slider.setValueWithUpdate(slider.getMin())
        );
        ImageButton minus = new ImageButton(
            26 + offsetX, 43 + offsetY,
            16, 16, 0, 0, 16, BUTTON_MINUS, 16, 32,
            (btn) -> this.slider.setValueWithUpdate(Math.max(slider.getMin(), slider.getValue() - 1))
        );
        this.addRenderableWidget(max);
        this.addRenderableWidget(add);
        this.addRenderableWidget(min);
        this.addRenderableWidget(minus);
        this.addRenderableWidget(this.slider);
        this.addRenderableWidget(this.value);
        this.setInitialFocus(this.value);
    }

    public void setValue(int value) {
        if (this.slider != null) slider.setValue(value);
        this.value.setValue("" + value);
    }

    private void onValueInput(@NotNull String value) {
        String regex = "^[+-]?[0-9]+$";
        int v;
        if (value.matches(regex)) {
            v = Integer.parseInt(value);
        } else if (value.isEmpty()) {
            v = 0;
        } else if (value.equals("-")) {
            return;
        } else if (value.equals("0-")) {
            this.value.setValue("-");
            return;
        } else {
            this.value.setValue("" + this.slider.getValue());
            return;
        }
        this.slider.setValueWithUpdate(v);
    }

    public void setMin(int min) {
        if (this.slider != null) slider.setMin(min);
    }

    public void setMax(int max) {
        if (this.slider != null) slider.setMax(max);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int offsetX = (this.width - this.imageWidth) / 2;
        int offsetY = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(LOCATION, offsetX, offsetY, 0, 0, this.imageWidth, this.imageHeight, 256, 128);
    }

    private void update(int value) {
        new SliderUpdatePack(value).send();
        this.value.setValue("" + value);
    }
}
