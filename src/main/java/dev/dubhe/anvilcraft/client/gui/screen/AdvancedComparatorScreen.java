package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.AdvancedComparatorMenu;
import dev.dubhe.anvilcraft.network.AdvancedComparatorUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class AdvancedComparatorScreen extends AbstractContainerScreen<AdvancedComparatorMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "advanced_comparator");

    private static final ResourceLocation SLIDER = SharedTextures.textureGui("machine/advanced_comparator/slider");

    private final Minecraft minecraft;
    private static final int GRID = 6;
    private int sliderY;
    private int sliderMax;
    private int sliderMin;
    private int slider1X;
    private int slider1Pos = 0;
    private int slider2X;
    private int slider2Pos = 0;
    private boolean scrolling1;
    private boolean scrolling2;

    public AdvancedComparatorScreen(AdvancedComparatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.minecraft = Minecraft.getInstance();
        this.imageHeight = 166;
    }

    @Override
    public void onClose() {
        AdvancedComparatorBlockEntity comparator = this.menu.getBlockEntity();
        PacketDistributor.sendToServer(new AdvancedComparatorUpdatePacket(
            comparator.getCompareMode().index(),
            comparator.isOutputInvert(),
            comparator.isRedstoneControl(),
            comparator.getHighLimit(),
            comparator.getLowLimit(),
            comparator.getInputtingSignal()
        ));
        Level level = comparator.getLevel();
        if (level != null) {
            level.scheduleTick(comparator.getBlockPos(), comparator.getBlockState().getBlock(), AdvancedComparatorBlock.getDelay());
        }
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 3;
        SwitchableButton compareMode = new SwitchableButton(
            this.leftPos + 8,
            this.topPos + 24,
            16, 16,
            List.of(SharedTextures.BUTTON_HYSTERESIS, SharedTextures.BUTTON_WINDOW),
            16, 16, 32,
            (button, index) -> this.menu.setCompareMode((byte) index),
            List.of(
                Component.translatable("screen.anvilcraft.button.compare_mode_hysteresis"),
                Component.translatable("screen.anvilcraft.button.compare_mode_window")
            )
        );
        SwitchableButton outputMode = new SwitchableButton(
            this.leftPos + 8,
            this.topPos + 42,
            16, 16,
            List.of(SharedTextures.BUTTON_REVERSE_OFF, SharedTextures.BUTTON_REVERSE_ON),
            16, 16, 32,
            (button, index) -> this.menu.setOutputInvert(index == 1),
            List.of(
                Component.translatable("screen.anvilcraft.button.reverse_off"),
                Component.translatable("screen.anvilcraft.button.reverse")
            )
        );
        SwitchableButton redstoneControl = new SwitchableButton(
            this.leftPos + 8,
            this.topPos + 60,
            16, 16,
            List.of(SharedTextures.BUTTON_REDSTONE_CONTROL_OFF, SharedTextures.BUTTON_REDSTONE_CONTROL_ON),
            16, 16, 32,
            (button, index) -> this.menu.setRedstoneControl(index == 1),
            List.of(
                Component.translatable("screen.anvilcraft.button.redstone_control_off"),
                Component.translatable("screen.anvilcraft.button.redstone_control")
            )
        );
        compareMode.setCurrent(this.menu.getBlockEntity().getCompareMode().index());
        outputMode.setCurrent(this.menu.getBlockEntity().isOutputInvert() ? 1 : 0);
        redstoneControl.setCurrent(this.menu.getBlockEntity().isRedstoneControl() ? 1 : 0);
        this.sliderY = this.topPos + 132;
        this.sliderMin = this.leftPos + 46;
        this.sliderMax = this.sliderMin + 91;
        this.slider1Pos = this.menu.getBlockEntity().getLowLimit();
        this.slider1X = Math.clamp((long) this.slider1Pos * AdvancedComparatorScreen.GRID + this.sliderMin, this.sliderMin, this.sliderMax);
        this.slider2Pos = this.menu.getBlockEntity().getHighLimit();
        this.slider2X = Math.clamp((long) this.slider2Pos * AdvancedComparatorScreen.GRID + this.sliderMin, this.sliderMin, this.sliderMax);
        this.addRenderableWidget(compareMode);
        this.addRenderableWidget(outputMode);
        this.addRenderableWidget(redstoneControl);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.init(minecraft, width, height);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

    }

    @Override
    @SuppressWarnings("checkstyle:LocalVariableName")
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int vOffset1 = this.isInSlider(mouseX, mouseY, this.slider1X, this.sliderY) ? 11 : 0;
        int vOffset2 = this.isInSlider(mouseX, mouseY, this.slider2X, this.sliderY) ? 11 : 0;
        guiGraphics.blit(SLIDER, this.slider1X, this.sliderY, 0, vOffset1, 7, 11, 7, 22);
        guiGraphics.blit(SLIDER, this.slider2X, this.sliderY, 0, vOffset2, 7, 11, 7, 22);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 1);
        String pos1 = String.valueOf(this.slider1Pos);
        String pos2 = String.valueOf(this.slider2Pos);
        int width1 = this.minecraft.font.width(pos1);
        int width2 = this.minecraft.font.width(pos2);
        guiGraphics.drawString(this.minecraft.font, pos1, this.slider1X * 2 + 8 - width1 / 2, this.sliderY * 2 + 8, 0xFF404040, false);
        guiGraphics.drawString(this.minecraft.font, pos2, this.slider2X * 2 + 8 - width2 / 2, this.sliderY * 2 + 8, 0xFF404040, false);
        pose.popPose();
        int max = Math.max(this.slider1X, this.slider2X);
        int min = Math.min(this.slider1X, this.slider2X);
        if (this.menu.getBlockEntity().getCompareMode() == AdvancedComparatorBlockEntity.Mode.WINDOW) {
            guiGraphics.fill(min + 3, this.sliderY, min + 4, this.sliderY - 90, 0xFF990000);
            guiGraphics.fill(max + 3, this.sliderY, max + 4, this.sliderY - 90, 0xFF990000);
            guiGraphics.fill(min + 3, this.sliderY - 90, max + 4, this.sliderY - 91, 0xFFFF0000);
            guiGraphics.fill(max + 3, this.sliderY, this.sliderMax + 15, this.sliderY - 1, 0xFF990000);
            guiGraphics.fill(max + 3, this.sliderY, this.sliderMax + 15, this.sliderY - 1, 0xFF990000);
            guiGraphics.fill(this.sliderMin - 4, this.sliderY, min + 4, this.sliderY - 1, 0xFF990000);
            return;
        }
        if (this.menu.getBlockEntity().isOutputInvert()) {
            guiGraphics.fill(min + 3, this.sliderY, min + 4, this.sliderY - 90, 0xFF990000);
            guiGraphics.fill(max + 3, this.sliderY, max + 4, this.sliderY - 90, 0xFFFF0000);
            guiGraphics.fill(this.sliderMin + 3, this.sliderY - 90, max + 4, this.sliderY - 91, 0xFFFF0000);
            guiGraphics.fill(this.sliderMax + 15, this.sliderY, min + 3, this.sliderY - 1, 0xFF990000);
            return;
        }
        guiGraphics.fill(min + 3, this.sliderY, min + 4, this.sliderY - 90, 0xFFFF0000);
        guiGraphics.fill(max + 3, this.sliderY, max + 4, this.sliderY - 90, 0xFF990000);
        guiGraphics.fill(this.sliderMin - 4, this.sliderY, max + 4, this.sliderY - 1, 0xFF990000);
        guiGraphics.fill(this.sliderMax + 5, this.sliderY - 90, min + 3, this.sliderY - 91, 0xFFFF0000);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.menu.getBlockEntity().isRedstoneControl()) return super.mouseClicked(mouseX, mouseY, button);
            if (this.isInSlider(mouseX, mouseY, this.slider1X, this.sliderY)) {
                this.scrolling1 = true;
            } else if (this.isInSlider(mouseX, mouseY, this.slider2X, this.sliderY)) {
                this.scrolling2 = true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling1 = false;
        this.scrolling2 = false;
        this.menu.getBlockEntity().setHighLimit(Math.max(this.slider1Pos, this.slider2Pos));
        this.menu.getBlockEntity().setLowLimit(Math.min(this.slider1Pos, this.slider2Pos));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling1) {
            this.slider1Pos = Math.clamp((int) (mouseX - this.sliderMin) / GRID, 0, 15);
            this.slider1X = Math.clamp(
                (long) this.slider1Pos * AdvancedComparatorScreen.GRID + this.sliderMin,
                this.sliderMin,
                this.sliderMax
            );
            return true;
        } else if (this.scrolling2) {
            this.slider2Pos = Math.clamp((int) (mouseX - this.sliderMin) / GRID, 0, 15);
            this.slider2X = Math.clamp(
                (long) this.slider2Pos * AdvancedComparatorScreen.GRID + this.sliderMin,
                this.sliderMin,
                this.sliderMax
            );
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    private boolean isInSlider(double mouseX, double mouseY, int sliderX, int sliderY) {
        return MathUtil.isInRange(mouseX, sliderX - 1, sliderX + 7)
               && MathUtil.isInRange(mouseY, sliderY - 1, sliderY + 11);
    }
}
