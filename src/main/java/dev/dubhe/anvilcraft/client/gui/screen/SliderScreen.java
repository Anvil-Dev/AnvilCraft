package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.Slider;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import dev.dubhe.anvilcraft.network.SliderUpdatePacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class SliderScreen extends AbstractContainerScreen<SliderMenu> {
    public static final Identifier BACKGROUND = SharedTextures.bg("misc", "slider_like");
    public static final Identifier BUTTON_MAX = SharedTextures.textureGui("misc/slider_like/button_max");
    public static final Identifier BUTTON_ADD = SharedTextures.textureGui("misc/slider_like/button_add");
    public static final Identifier BUTTON_MINUS = SharedTextures.textureGui("misc/slider_like/button_minus");
    public static final Identifier BUTTON_MIN = SharedTextures.textureGui("misc/slider_like/button_min");
    private Slider slider = null;
    private EditBox value;

    public SliderScreen(SliderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 77);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        int offsetX = (this.width - this.getImageWidth()) / 2;
        int offsetY = (this.height - this.getImageHeight()) / 2;
        this.value = new EditBox(this.font, offsetX + 50, offsetY + 47, 76, 8, Component.literal("value"));
        this.slider = new Slider(8 + offsetX, 31 + offsetY, -14, 14, 160 - 16, this::update);
        this.value.setCanLoseFocus(true);
        this.value.setTextColor(-1);
        this.value.setTextColorUneditable(-1);
        this.value.setBordered(false);
        this.value.setMaxLength(50);
        this.value.setResponder(this::onValueInput);
        this.value.setValue("");
        TexturedButton max = new TexturedButton(
            152 + offsetX,
            43 + offsetY,
            16,
            16,
            BUTTON_MAX,
            16,
            16,
            32,
            _ -> this.slider.setValueWithUpdate(8192));
        TexturedButton add = new TexturedButton(
            134 + offsetX,
            43 + offsetY,
            16,
            16,
            BUTTON_ADD,
            16,
            16,
            32,
            _ -> this.value.setValue("" + Math.clamp(
                Integer.parseInt(this.value.getValue()) + this.slider.getAddValue(Integer.parseInt(this.value.getValue())),
                -8192,
                8192
            )));
        TexturedButton min = new TexturedButton(
            8 + offsetX,
            43 + offsetY,
            16,
            16,
            BUTTON_MIN,
            16,
            16,
            32,
            _ -> this.slider.setValueWithUpdate(-8192));
        TexturedButton minus = new TexturedButton(
            26 + offsetX,
            43 + offsetY,
            16,
            16,
            BUTTON_MINUS,
            16,
            16,
            32,
            _ -> this.value.setValue("" + Math.clamp(
                Integer.parseInt(this.value.getValue()) - this.slider.getAddValue(Integer.parseInt(this.value.getValue())),
                -8192,
                8192
            )));
        this.addRenderableWidget(max);
        this.addRenderableWidget(add);
        this.addRenderableWidget(min);
        this.addRenderableWidget(minus);
        this.addRenderableWidget(this.slider);
        this.addRenderableWidget(this.value);
        this.setInitialFocus(this.value);
    }

    public void setValue(int value) {
        if (this.slider != null) this.slider.setValue(value);
        this.value.setValue("" + value);
    }

    private void onValueInput(String value) {
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
        this.slider.setValue(v);
        ClientPacketDistributor.sendToServer(new SliderUpdatePacket(Math.clamp(v, -8192, 8192)));
    }

    public void setMin(int min) {
        if (this.slider != null) {
            this.slider.setMin(min);
        }
    }

    public void setMax(int max) {
        if (this.slider != null) {
            this.slider.setMax(max);
        }
    }

    @Override
    public void resize(int width, int height) {
        int lastValue = Integer.parseInt(this.value.getValue());
        this.init(width, height);
        this.value.setValue("" + lastValue);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == 0) {
            this.slider.onClick(event, handled);
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        this.slider.onDrag(event, dragX, dragY);
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.slider.onReleased();
        return super.mouseReleased(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 128);
    }

    private void update(int value) {
        ClientPacketDistributor.sendToServer(new SliderUpdatePacket(value));
        this.value.setValue(Integer.toString(value));
    }
}
