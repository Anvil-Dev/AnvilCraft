package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.PulseGeneratorMenu;
import dev.dubhe.anvilcraft.network.PulseGeneratorUpdatePacket;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class PulseGeneratorScreen extends AbstractContainerScreen<PulseGeneratorMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "pulse_generator");

    private static final ResourceLocation BUTTON_ADD_T =
        SharedTextures.textureGui("machine/pulse_generator/button_add_t");
    private static final ResourceLocation BUTTON_ADD_S =
        SharedTextures.textureGui("machine/pulse_generator/button_add_s");
    private static final ResourceLocation BUTTON_ADD_M =
        SharedTextures.textureGui("machine/pulse_generator/button_add_m");
    private static final ResourceLocation BUTTON_MINUS_T =
        SharedTextures.textureGui("machine/pulse_generator/button_minus_t");
    private static final ResourceLocation BUTTON_MINUS_S =
        SharedTextures.textureGui("machine/pulse_generator/button_minus_s");
    private static final ResourceLocation BUTTON_MINUS_M =
        SharedTextures.textureGui("machine/pulse_generator/button_minus_m");

    private final Minecraft minecraft;
    private TextWidget waitingTime;
    private TextWidget signalDuration;

    public PulseGeneratorScreen(PulseGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.minecraft = Minecraft.getInstance();
        this.imageHeight = 77;
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new PulseGeneratorUpdatePacket(
            this.menu.getBlockEntity().getStartMode().index(),
            this.menu.getBlockEntity().isOutputInvert(),
            this.menu.getBlockEntity().getWaitingTime(),
            this.menu.getBlockEntity().getSignalDuration()
        ));
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 3;
        final SwitchableButton startMode = new SwitchableButton(
            this.leftPos + 28,
            this.topPos + 25,
            16, 16,
            List.of(SharedTextures.BUTTON_RISING_EDGE, SharedTextures.BUTTON_FALLING_EDGE, SharedTextures.BUTTON_LOOP),
            16, 16, 32,
            (button, index) -> this.menu.setStartMode((byte) index),
            List.of(Component.translatable("screen.anvilcraft.button.pulse_generator.start_mode.rising"),
                Component.translatable("screen.anvilcraft.button.pulse_generator.start_mode.falling"),
                Component.translatable("screen.anvilcraft.button.pulse_generator.start_mode.loop"))
        );
        final SwitchableButton outputMode = new SwitchableButton(
            this.leftPos + 28,
            this.topPos + 43,
            16, 16,
            List.of(SharedTextures.BUTTON_REVERSE_OFF, SharedTextures.BUTTON_REVERSE_ON),
            16, 16, 32,
            (button, index) -> this.menu.setOutputInvert(index == 1),
            List.of(Component.translatable("screen.anvilcraft.button.pulse_generator.reverse.off"),
                Component.translatable("screen.anvilcraft.button.pulse_generator.reverse.on"))
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> addTickFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 25,
            10, 10,
            BUTTON_ADD_T,
            10, 10, 20,
            button -> tickAdder.accept(!hasShiftDown() ? 1 : 5)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> addSecFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 25,
            10, 10,
            BUTTON_ADD_S,
            10, 10, 20,
            button -> tickAdder.accept(!hasShiftDown() ? 20 : 100)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> addMinFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 25,
            10, 10,
            BUTTON_ADD_M,
            10, 10, 20,
            button -> tickAdder.accept(!hasShiftDown() ? 1200 : 6000)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> minusTickFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 49,
            10, 10,
            BUTTON_MINUS_T,
            10, 10, 20,
            button -> tickAdder.accept(!hasShiftDown() ? -1 : -5)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> minusSecFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 49,
            10, 10,
            BUTTON_MINUS_S,
            10, 10, 20,
            button -> tickAdder.accept(!hasShiftDown() ? -20 : -100)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> minusMinFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 49,
            10, 10,
            BUTTON_MINUS_M,
            10, 10, 20,
            button -> tickAdder.accept(!hasShiftDown() ? -1200 : -6000)
        );
        this.waitingTime = new TextWidget(
            this.leftPos + 63,
            this.topPos + 38,
            32, 9,
            minecraft.font,
            () -> Component.literal(FormattingUtil.toFormattedTime(this.menu.getBlockEntity().getWaitingTime(), 5))
        ).setRenderMode(TextWidget.RenderMode.SCALED);
        this.signalDuration = new TextWidget(
            this.leftPos + 115,
            this.topPos + 38,
            32, 9,
            minecraft.font,
            () -> Component.literal(FormattingUtil.toFormattedTime(this.menu.getBlockEntity().getSignalDuration(), 5))
        ).setRenderMode(TextWidget.RenderMode.SCALED);
        startMode.setCurrent(this.menu.getBlockEntity().getStartMode().index());
        outputMode.setCurrent(this.menu.getBlockEntity().isOutputInvert() ? 1 : 0);
        this.addRenderableWidget(startMode);
        this.addRenderableWidget(outputMode);
        this.addRenderableOnly(this.waitingTime);
        this.addRenderableWidget(addTickFunc.apply(62, this.menu::addWaitingTime));
        this.addRenderableWidget(addSecFunc.apply(74, this.menu::addWaitingTime));
        this.addRenderableWidget(addMinFunc.apply(86, this.menu::addWaitingTime));
        this.addRenderableWidget(minusTickFunc.apply(62, this.menu::addWaitingTime));
        this.addRenderableWidget(minusSecFunc.apply(74, this.menu::addWaitingTime));
        this.addRenderableWidget(minusMinFunc.apply(86, this.menu::addWaitingTime));
        this.addRenderableOnly(this.signalDuration);
        this.addRenderableWidget(addTickFunc.apply(114, this.menu::addSignalDuration));
        this.addRenderableWidget(addSecFunc.apply(126, this.menu::addSignalDuration));
        this.addRenderableWidget(addMinFunc.apply(138, this.menu::addSignalDuration));
        this.addRenderableWidget(minusTickFunc.apply(114, this.menu::addSignalDuration));
        this.addRenderableWidget(minusSecFunc.apply(126, this.menu::addSignalDuration));
        this.addRenderableWidget(minusMinFunc.apply(138, this.menu::addSignalDuration));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 128);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (MathUtil.isInRange(
            mouseX, mouseY,
            this.waitingTime.getX(), this.waitingTime.getY(),
            this.waitingTime.getX() + this.waitingTime.getWidth(),
            this.waitingTime.getY() + this.waitingTime.getHeight())
        ) {
            if (hasControlDown()) {
                this.menu.addWaitingTime(scrollY < 0 ? -20 : 20);
            } else if (hasShiftDown()) {
                this.menu.addWaitingTime(scrollY < 0 ? -1200 : 1200);
            } else {
                this.menu.addWaitingTime(scrollY < 0 ? -1 : 1);
            }
        }
        if (MathUtil.isInRange(
            mouseX, mouseY,
            this.signalDuration.getX(), this.signalDuration.getY(),
            this.signalDuration.getX() + this.signalDuration.getWidth(),
            this.signalDuration.getY() + this.signalDuration.getHeight())
        ) {
            if (hasControlDown()) {
                this.menu.addSignalDuration(scrollY < 0 ? -20 : 20);
            } else if (hasShiftDown()) {
                this.menu.addSignalDuration(scrollY < 0 ? -1200 : 1200);
            } else {
                this.menu.addSignalDuration(scrollY < 0 ? -1 : 1);
            }
        }
        return true;
    }
}
