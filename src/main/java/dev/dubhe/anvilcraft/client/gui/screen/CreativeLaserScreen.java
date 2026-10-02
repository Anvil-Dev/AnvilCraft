package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.client.gui.component.SliderWidget;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.CreativeLaserMenu;
import dev.dubhe.anvilcraft.network.CreativeLaserUpdatePacket;
import dev.dubhe.anvilcraft.util.Callback;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreativeLaserScreen extends AbstractContainerScreen<CreativeLaserMenu> {
    public static final Identifier BACKGROUND = SharedTextures.bg("machine", "creative_laser");
    private static final Identifier LENS_NONE = SharedTextures.textureGui("machine/creative_laser/lens_none");
    private static final Identifier LENS_ROYAL = SharedTextures.textureGui("machine/creative_laser/lens_royal");
    private static final Identifier LENS_FROST = SharedTextures.textureGui("machine/creative_laser/lens_frost");
    private static final Identifier LENS_EMBER = SharedTextures.textureGui("machine/creative_laser/lens_ember");
    private static final Identifier TYPE_NORMAL = SharedTextures.textureGui("machine/creative_laser/normal");
    private static final Identifier TYPE_GAMMA = SharedTextures.textureGui("machine/creative_laser/gamma");
    private static final Identifier BUTTON_MIN = SharedTextures.textureGui("misc/slider_like/button_min");
    private static final Identifier BUTTON_MINUS = SharedTextures.textureGui("misc/slider_like/button_minus");
    private static final Identifier BUTTON_ADD = SharedTextures.textureGui("misc/slider_like/button_add");
    private static final Identifier BUTTON_MAX = SharedTextures.textureGui("misc/slider_like/button_max");
    private static final int LENS_BUTTON_X = 8;
    private static final int TYPE_BUTTON_X = 134;
    private static final int BUTTON_Y = 53;
    private static final int BUTTON_GAP = 18;
    private static final int CONTROL_BUTTON_Y = 29;

    private @Nullable LinearSlider slider;
    private @Nullable EditBox value;
    private final List<TriStateButton> lensButtons = new ArrayList<>();
    private final List<TriStateButton> typeButtons = new ArrayList<>();
    private int level = 0;
    private LensType lensType = LensType.NONE;
    private boolean gamma = false;

    public CreativeLaserScreen(CreativeLaserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 77);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        int offsetX = (this.width - this.getImageWidth()) / 2;
        int offsetY = (this.height - this.getImageHeight()) / 2;
        this.lensButtons.clear();
        this.typeButtons.clear();
        this.value = new EditBox(this.font, offsetX + 50, offsetY + 33, 76, 8, Component.literal("value"));
        this.value.setCanLoseFocus(true);
        this.value.setTextColor(-1);
        this.value.setTextColorUneditable(-1);
        this.value.setBordered(false);
        this.value.setMaxLength(2);
        this.slider = new LinearSlider(8 + offsetX, 17 + offsetY, 0, 64, 160 - 16, this::update);
        this.addRenderableWidget(this.slider);
        this.addRenderableWidget(this.value);

        Identifier[] lensTextures = {LENS_NONE, LENS_ROYAL, LENS_FROST, LENS_EMBER};
        for (int i = 0; i < lensTextures.length; i++) {
            int index = i;
            TriStateButton button = new TriStateButton(
                offsetX + LENS_BUTTON_X + i * BUTTON_GAP,
                offsetY + BUTTON_Y,
                16,
                16,
                lensTextures[i],
                16,
                48,
                btn -> this.selectLens(index),
                List.of(Component.translatable(
                    "screen.anvilcraft.creative_laser.lens." + LensType.values()[i].getSerializedName()
                ))
            );
            this.lensButtons.add(button);
            this.addRenderableWidget(button);
        }
        TriStateButton normal = new TriStateButton(
            offsetX + TYPE_BUTTON_X,
            offsetY + BUTTON_Y,
            16,
            16,
            TYPE_NORMAL,
            16,
            48,
            btn -> this.selectType(false),
            List.of(Component.translatable("screen.anvilcraft.creative_laser.type.normal"))
        );
        TriStateButton gamma = new TriStateButton(
            offsetX + TYPE_BUTTON_X + BUTTON_GAP,
            offsetY + BUTTON_Y,
            16,
            16,
            TYPE_GAMMA,
            16,
            48,
            btn -> this.selectType(true),
            List.of(Component.translatable("screen.anvilcraft.creative_laser.type.gamma"))
        );
        this.typeButtons.add(normal);
        this.typeButtons.add(gamma);
        this.addRenderableWidget(normal);
        this.addRenderableWidget(gamma);
        EditBox valueBox = this.value;
        LinearSlider sliderBox = this.slider;
        TexturedButton min = new TexturedButton(
            8 + offsetX, CONTROL_BUTTON_Y + offsetY, 16, 16,
            BUTTON_MIN, 16, 16, 32,
            btn -> valueBox.setValue("0"));
        TexturedButton minus = new TexturedButton(
            26 + offsetX, CONTROL_BUTTON_Y + offsetY, 16, 16,
            BUTTON_MINUS, 16, 16, 32,
            btn -> valueBox.setValue("" + Math.clamp(sliderBox.getValue() - 1, 0, 64)));
        TexturedButton add = new TexturedButton(
            134 + offsetX, CONTROL_BUTTON_Y + offsetY, 16, 16,
            BUTTON_ADD, 16, 16, 32,
            btn -> valueBox.setValue("" + Math.clamp(sliderBox.getValue() + 1, 0, 64)));
        TexturedButton max = new TexturedButton(
            152 + offsetX, CONTROL_BUTTON_Y + offsetY, 16, 16,
            BUTTON_MAX, 16, 16, 32,
            btn -> valueBox.setValue("64"));
        this.addRenderableWidget(min);
        this.addRenderableWidget(minus);
        this.addRenderableWidget(add);
        this.addRenderableWidget(max);
        this.value.setResponder(this::onValueInput);
        this.setInitialFocus(this.value);
    }

    public void setValue(int level, LensType lensType, boolean gamma) {
        this.level = Math.clamp(level, 0, 64);
        this.lensType = lensType;
        this.gamma = gamma;
        if (!this.lensButtons.isEmpty()) {
            for (int i = 0; i < this.lensButtons.size(); i++) {
                this.lensButtons.get(i).setSelected(LensType.values()[i] == lensType);
            }
        }
        if (!this.typeButtons.isEmpty()) {
            this.typeButtons.get(0).setSelected(!gamma);
            this.typeButtons.get(1).setSelected(gamma);
        }
        if (this.slider != null) {
            this.slider.setValue(this.level);
        }
        if (this.value != null) {
            this.value.setValue(Integer.toString(this.level));
        }
    }

    private void onValueInput(String text) {
        if (this.slider == null || this.value == null) return;
        if (text.isEmpty()) {
            this.slider.setValue(0);
            this.sendUpdate();
            return;
        }
        if (!text.matches("^[0-9]+$")) {
            this.value.setValue(Integer.toString(this.slider.getValue()));
            return;
        }
        this.slider.setValue(Math.clamp(Integer.parseInt(text), 0, 64));
        this.sendUpdate();
    }

    private void update(int level) {
        if (this.value == null) return;
        this.value.setValue(Integer.toString(level));
        this.sendUpdate();
    }

    private void selectLens(int index) {
        for (int i = 0; i < this.lensButtons.size(); i++) {
            this.lensButtons.get(i).setSelected(i == index);
        }
        this.sendUpdate();
    }

    private void selectType(boolean gamma) {
        this.typeButtons.get(0).setSelected(!gamma);
        this.typeButtons.get(1).setSelected(gamma);
        this.sendUpdate();
    }

    private void sendUpdate() {
        if (this.slider == null) return;
        int lensIndex = 0;
        for (int i = 0; i < this.lensButtons.size(); i++) {
            if (this.lensButtons.get(i).isSelected()) {
                lensIndex = i;
                break;
            }
        }
        this.level = this.slider.getValue();
        this.lensType = LensType.values()[lensIndex];
        this.gamma = this.typeButtons.get(1).isSelected();
        ClientPacketDistributor.sendToServer(new CreativeLaserUpdatePacket(this.level, this.lensType, this.gamma));
    }

    @Override
    public void resize(int width, int height) {
        int level = this.level;
        LensType lensType = this.lensType;
        boolean gamma = this.gamma;
        this.init(width, height);
        this.setValue(level, lensType, gamma);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == 0 && this.slider != null) {
            this.slider.onClick(event, handled);
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.slider != null) {
            this.slider.onDrag(event, dragX, dragY);
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.slider != null) {
            this.slider.onReleased();
        }
        return super.mouseReleased(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int offsetX = (this.width - this.getImageWidth()) / 2;
        int offsetY = (this.height - this.getImageHeight()) / 2;
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, offsetX, offsetY, 0, 0,
            this.getImageWidth(), this.getImageHeight(), 256, 128);
    }

    /**
     * 线性滑条：0-64 激光等级。
     */
    private static class LinearSlider extends SliderWidget {
        LinearSlider(int x, int y, int min, int max, int length, Callback<Integer> callback) {
            super(
                x,
                y,
                min,
                max,
                length,
                i -> SliderWidget.defaultValueFunction(i, min, max),
                i -> (i - min) / (double) (max - min),
                callback
            );
        }

        @Override
        public int getValue() {
            return (int) Math.round(SliderWidget.defaultValueFunction(this.getProportion(), this.getMin(), this.getMax()));
        }
    }
}
