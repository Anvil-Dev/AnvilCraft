package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.PulseGeneratorMenu;
import dev.dubhe.anvilcraft.network.PulseGeneratorUpdatePacket;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class PulseGeneratorScreen extends AbstractContainerScreen<PulseGeneratorMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "pulse_generator");

    private static final Identifier BUTTON_ADD_T =
        SharedTextures.textureGui("machine/pulse_generator/button_add_t");
    private static final Identifier BUTTON_ADD_S =
        SharedTextures.textureGui("machine/pulse_generator/button_add_s");
    private static final Identifier BUTTON_ADD_M =
        SharedTextures.textureGui("machine/pulse_generator/button_add_m");
    private static final Identifier BUTTON_MINUS_T =
        SharedTextures.textureGui("machine/pulse_generator/button_minus_t");
    private static final Identifier BUTTON_MINUS_S =
        SharedTextures.textureGui("machine/pulse_generator/button_minus_s");
    private static final Identifier BUTTON_MINUS_M =
        SharedTextures.textureGui("machine/pulse_generator/button_minus_m");

    private final Minecraft minecraft;
    private byte pendingStartMode;
    private boolean pendingOutputInvert;
    private int pendingWaitingTime;
    private int pendingSignalDuration;
    private @Nullable TextWidget waitingTime;
    private @Nullable TextWidget signalDuration;

    public PulseGeneratorScreen(PulseGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 77);
        this.minecraft = Minecraft.getInstance();
        this.pendingStartMode = menu.getBlockEntity().getStartMode().index();
        this.pendingOutputInvert = menu.getBlockEntity().isOutputInvert();
        this.pendingWaitingTime = menu.getBlockEntity().getWaitingTime();
        this.pendingSignalDuration = menu.getBlockEntity().getSignalDuration();
    }

    @Override
    public void onClose() {
        ClientPacketDistributor.sendToServer(new PulseGeneratorUpdatePacket(
            this.pendingStartMode,
            this.pendingOutputInvert,
            this.pendingWaitingTime,
            this.pendingSignalDuration
        ));
        super.onClose();
    }

    private void addWaitingTime(int delta) {
        this.pendingWaitingTime = Math.clamp(this.pendingWaitingTime + delta, 0, 24000);
        if (this.pendingWaitingTime == 0 && this.pendingSignalDuration == 0) {
            this.pendingSignalDuration = 1;
        }
    }

    private void addSignalDuration(int delta) {
        this.pendingSignalDuration = Math.clamp(this.pendingSignalDuration + delta, 0, 24000);
        if (this.pendingSignalDuration == 0 && this.pendingWaitingTime == 0) {
            this.pendingWaitingTime = 1;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        final SwitchableButton startMode = new SwitchableButton(
            this.leftPos + 28,
            this.topPos + 25,
            16,
            16,
            List.of(SharedTextures.BUTTON_RISING_EDGE, SharedTextures.BUTTON_FALLING_EDGE, SharedTextures.BUTTON_LOOP),
            16,
            16,
            32,
            (_, index) -> this.pendingStartMode = (byte) index,
            List.of(
                Component.translatable("screen.anvilcraft.button.pulse_generator.start_mode.rising"),
                Component.translatable("screen.anvilcraft.button.pulse_generator.start_mode.falling"),
                Component.translatable("screen.anvilcraft.button.pulse_generator.start_mode.loop")
            )
        );
        final SwitchableButton outputMode = new SwitchableButton(
            this.leftPos + 28,
            this.topPos + 43,
            16,
            16,
            List.of(SharedTextures.BUTTON_REVERSE_OFF, SharedTextures.BUTTON_REVERSE_ON),
            16,
            16,
            32,
            (_, index) -> this.pendingOutputInvert = index == 1,
            List.of(
                Component.translatable("screen.anvilcraft.button.pulse_generator.reverse.off"),
                Component.translatable("screen.anvilcraft.button.pulse_generator.reverse.on")
            )
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> addTickFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 25,
            10, 10,
            PulseGeneratorScreen.BUTTON_ADD_T,
            10, 10, 20,
            _ -> tickAdder.accept(!this.minecraft.hasShiftDown() ? 1 : 5)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> addSecFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 25,
            10, 10,
            PulseGeneratorScreen.BUTTON_ADD_S,
            10, 10, 20,
            _ -> tickAdder.accept(!this.minecraft.hasShiftDown() ? 20 : 100)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> addMinFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 25,
            10, 10,
            PulseGeneratorScreen.BUTTON_ADD_M,
            10, 10, 20,
            _ -> tickAdder.accept(!this.minecraft.hasShiftDown() ? 1200 : 6000)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> minusTickFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 49,
            10, 10,
            PulseGeneratorScreen.BUTTON_MINUS_T,
            10, 10, 20,
            _ -> tickAdder.accept(!this.minecraft.hasShiftDown() ? -1 : -5)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> minusSecFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 49,
            10, 10,
            PulseGeneratorScreen.BUTTON_MINUS_S,
            10, 10, 20,
            _ -> tickAdder.accept(!this.minecraft.hasShiftDown() ? -20 : -100)
        );
        final BiFunction<Integer, Consumer<Integer>, TexturedButton> minusMinFunc = (offsetX, tickAdder) -> new TexturedButton(
            this.leftPos + offsetX,
            this.topPos + 49,
            10, 10,
            PulseGeneratorScreen.BUTTON_MINUS_M,
            10, 10, 20,
            _ -> tickAdder.accept(!this.minecraft.hasShiftDown() ? -1200 : -6000)
        );
        this.waitingTime = new TextWidget(
            this.leftPos + 63,
            this.topPos + 38,
            32, 9,
            this.minecraft.font,
            () -> Component.literal(FormattingUtil.toFormattedTime(this.pendingWaitingTime, 5))
        ).setRenderMode(TextWidget.RenderMode.SCALED).alignCenter();
        this.signalDuration = new TextWidget(
            this.leftPos + 115,
            this.topPos + 38,
            32, 9,
            this.minecraft.font,
            () -> Component.literal(FormattingUtil.toFormattedTime(this.pendingSignalDuration, 5))
        ).setRenderMode(TextWidget.RenderMode.SCALED).alignCenter();
        startMode.setCurrent(this.pendingStartMode);
        outputMode.setCurrent(this.pendingOutputInvert ? 1 : 0);
        this.addRenderableWidget(startMode);
        this.addRenderableWidget(outputMode);
        this.addRenderableOnly(this.waitingTime);
        this.addRenderableWidget(addTickFunc.apply(62, this::addWaitingTime));
        this.addRenderableWidget(addSecFunc.apply(74, this::addWaitingTime));
        this.addRenderableWidget(addMinFunc.apply(86, this::addWaitingTime));
        this.addRenderableWidget(minusTickFunc.apply(62, this::addWaitingTime));
        this.addRenderableWidget(minusSecFunc.apply(74, this::addWaitingTime));
        this.addRenderableWidget(minusMinFunc.apply(86, this::addWaitingTime));
        this.addRenderableOnly(this.signalDuration);
        this.addRenderableWidget(addTickFunc.apply(114, this::addSignalDuration));
        this.addRenderableWidget(addSecFunc.apply(126, this::addSignalDuration));
        this.addRenderableWidget(addMinFunc.apply(138, this::addSignalDuration));
        this.addRenderableWidget(minusTickFunc.apply(114, this::addSignalDuration));
        this.addRenderableWidget(minusSecFunc.apply(126, this::addSignalDuration));
        this.addRenderableWidget(minusMinFunc.apply(138, this::addSignalDuration));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            PulseGeneratorScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            128
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (MathUtil.isInRange(
            mouseX,
            mouseY,
            this.waitingTime.getX(),
            this.waitingTime.getY(),
            this.waitingTime.getX() + this.waitingTime.getWidth(),
            this.waitingTime.getY() + this.waitingTime.getHeight()
        )) {
            if (this.minecraft.hasControlDown()) {
                this.addWaitingTime(scrollY < 0 ? -20 : 20);
            } else if (this.minecraft.hasShiftDown()) {
                this.addWaitingTime(scrollY < 0 ? -1200 : 1200);
            } else {
                this.addWaitingTime(scrollY < 0 ? -1 : 1);
            }
        }
        if (MathUtil.isInRange(
            mouseX,
            mouseY,
            this.signalDuration.getX(),
            this.signalDuration.getY(),
            this.signalDuration.getX() + this.signalDuration.getWidth(),
            this.signalDuration.getY() + this.signalDuration.getHeight()
        )) {
            if (this.minecraft.hasControlDown()) {
                this.addSignalDuration(scrollY < 0 ? -20 : 20);
            } else if (this.minecraft.hasShiftDown()) {
                this.addSignalDuration(scrollY < 0 ? -1200 : 1200);
            } else {
                this.addSignalDuration(scrollY < 0 ? -1 : 1);
            }
        }
        return true;
    }
}
