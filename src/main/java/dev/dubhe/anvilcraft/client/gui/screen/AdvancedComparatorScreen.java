package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.inventory.AdvancedComparatorMenu;
import dev.dubhe.anvilcraft.network.AdvancedComparatorUpdatePacket;
import dev.dubhe.anvilcraft.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class AdvancedComparatorScreen extends AbstractContainerScreen<AdvancedComparatorMenu> {
    private static final ResourceLocation CONTAINER_LOCATION =
        AnvilCraft.of("textures/gui/container/machine/background/advanced_comparator.png");

    private static final ResourceLocation BUTTON_REVERSE_OFF =
        AnvilCraft.of("textures/gui/container/machine/button_reverse_off.png");
    private static final ResourceLocation BUTTON_REVERSE_ON =
        AnvilCraft.of("textures/gui/container/machine/button_reverse_on.png");

    private static final ResourceLocation BUTTON_HYSTERESIS =
        AnvilCraft.of("textures/gui/container/machine/button_hysteresis.png");
    private static final ResourceLocation BUTTON_WINDOW =
        AnvilCraft.of("textures/gui/container/machine/button_window.png");

    private static final ResourceLocation BUTTON_REDSTONE_CONTROL =
        AnvilCraft.of("textures/gui/container/machine/button_redstone_control_off.png");
    private static final ResourceLocation BUTTON_REDSTONE_CONTROL_ON =
        AnvilCraft.of("textures/gui/container/machine/button_redstone_control_on.png");

    private static final ResourceLocation SLIDER =
        AnvilCraft.of("textures/gui/container/machine/advanced_comparator_slider.png");

    private final Minecraft minecraft;
    private final int GRID = 6;
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
        if (level != null)
            level.scheduleTick(comparator.getBlockPos(), comparator.getBlockState().getBlock(), AdvancedComparatorBlock.getDelay());
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        SwitchableButton compareMode = new SwitchableButton(
            this.leftPos + 8,
            this.topPos + 24,
            16, 16,
            List.of(BUTTON_HYSTERESIS, BUTTON_WINDOW),
            16, 16, 32,
            (button, index) -> this.menu.setCompareMode((byte) index)
        );
        SwitchableButton outputMode = new SwitchableButton(
            this.leftPos + 8,
            this.topPos + 42,
            16, 16,
            List.of(BUTTON_REVERSE_OFF, BUTTON_REVERSE_ON),
            16, 16, 32,
            (button, index) -> this.menu.setOutputInvert(index == 1)
        );
        SwitchableButton redstoneControl = new SwitchableButton(
            this.leftPos + 8,
            this.topPos + 60,
            16, 16,
            List.of(BUTTON_REDSTONE_CONTROL, BUTTON_REDSTONE_CONTROL_ON),
            16, 16, 32,
            (button, index) -> this.menu.setRedstoneControl(index == 1)
        );

        compareMode.setCurrent(this.menu.getBlockEntity().getCompareMode().index());
        outputMode.setCurrent(this.menu.getBlockEntity().isOutputInvert() ? 1 : 0);
        redstoneControl.setCurrent(this.menu.getBlockEntity().isRedstoneControl() ? 1 : 0);
        this.sliderY = this.topPos + 132;
        this.sliderMin = this.leftPos + 46;
        this.sliderMax = this.sliderMin + 91;
        this.slider1Pos = this.menu.getBlockEntity().getLowLimit();
        this.slider1X = snapOnGrid(this.slider1Pos, this.GRID, this.sliderMin, this.sliderMax);
        this.slider2Pos = this.menu.getBlockEntity().getHighLimit();
        this.slider2X = snapOnGrid(this.slider2Pos, this.GRID, this.sliderMin, this.sliderMax);
        this.addRenderableWidget(compareMode);
        this.addRenderableWidget(outputMode);
        this.addRenderableWidget(redstoneControl);
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        this.init(minecraft, width, height);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(CONTAINER_LOCATION, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        guiGraphics.blit(SLIDER, slider1X, sliderY, 0, 0, 7, 11, 7, 22);
        guiGraphics.blit(SLIDER, slider2X, sliderY, 0, 0, 7, 11, 7, 22);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 1);
        String pos1 = String.valueOf(slider1Pos);
        String pos2 = String.valueOf(slider2Pos);
        int width1 = this.minecraft.font.width(pos1);
        int width2 = this.minecraft.font.width(pos2);
        guiGraphics.drawString(this.minecraft.font, pos1, slider1X * 2 + 8 - width1 / 2, sliderY * 2 + 8, 0xFF404040, false);
        guiGraphics.drawString(this.minecraft.font, pos2, slider2X * 2 + 8 - width2 / 2, sliderY * 2 + 8, 0xFF404040, false);
        pose.popPose();
        int max = Math.max(slider1X, slider2X);
        int min = Math.min(slider1X, slider2X);
        if (this.menu.getBlockEntity().isOutputInvert()) {
            guiGraphics.fill(min + 3, sliderY, min + 4, sliderY - 90, 0xFF990000);
            guiGraphics.fill(max + 3, sliderY, max + 4, sliderY - 90, 0xFFFF0000);
            guiGraphics.fill(sliderMin + 3, sliderY - 90, max + 4, sliderY - 91, 0xFFFF0000);
            guiGraphics.fill(sliderMax + 15, sliderY, min + 3, sliderY - 1, 0xFF990000);
        } else {
            guiGraphics.fill(min + 3, sliderY, min + 4, sliderY - 90, 0xFFFF0000);
            guiGraphics.fill(max + 3, sliderY, max + 4, sliderY - 90, 0xFF990000);
            guiGraphics.fill(sliderMin - 4, sliderY, max + 4, sliderY - 1, 0xFF990000);
            guiGraphics.fill(sliderMax + 5, sliderY - 90, min + 3, sliderY - 91, 0xFFFF0000);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.menu.getBlockEntity().isRedstoneControl()) return super.mouseClicked(mouseX, mouseY, button);
            if (MathUtil.isInRange(mouseX, slider1X, slider1X + 7)) {
                this.scrolling1 = true;
            } else if (MathUtil.isInRange(mouseX, slider2X, slider2X + 7)) {
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
            this.slider1Pos = posInRange((int) (mouseX - this.sliderMin) / GRID, 0, 15);
            this.slider1X = snapOnGrid(this.slider1Pos, this.GRID, this.sliderMin, this.sliderMax);
            return true;
        } else if (this.scrolling2) {
            this.slider2Pos = posInRange((int) (mouseX - this.sliderMin) / GRID, 0, 15);
            this.slider2X = snapOnGrid(this.slider2Pos, this.GRID, this.sliderMin, this.sliderMax);
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    public int snapOnGrid(int sliderPos, int grid, int sliderMin, int sliderMax) {
        int pos = sliderPos * grid + sliderMin;
        if (sliderMin > sliderMax) {
            int v = sliderMin;
            sliderMin = sliderMax;
            sliderMax = v;
        }
        if (pos > sliderMax) return sliderMax;
        else if (pos < sliderMin) return sliderMin;
        return pos;
    }

    public int posInRange(int pos, int min, int max) {
        if (min > max) {
            int v = min;
            min = max;
            max = v;
        }
        if (pos > max) return max;
        return Math.max(pos, min);
    }
}
