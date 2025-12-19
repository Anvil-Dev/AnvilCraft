package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.WheelWidget;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.network.SwitchMultitoolModePacket;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Objects;

@Getter
public class MultitoolScreen extends Screen {
    private final LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
    private final InteractionHand hand;
    private final int mode;

    private WheelWidget wheel;

    public MultitoolScreen(InteractionHand hand, int mode) {
        super(Component.translatable("screen.anvilcraft.multiphase.title"));
        this.hand = hand;
        this.mode = mode;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int leftPos = (this.width - 75) / 2;
        int topPos = (this.height - 75) / 2;
        ItemStack itemStack = player.getItemInHand(hand);
        WheelWidget wheel = new WheelWidget(
            leftPos,
            topPos,
            75,
            75,
            12.5f,
            32.5f,
            0.75f,
            List.of(
                new WheelWidget.RawSection(
                    Component.translatable("screen.anvilcraft.multitool.all"),
                    MultitoolScreen.render(itemStack, MultitoolItem.ALL_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("item.minecraft.shears"),
                    MultitoolScreen.render(itemStack, MultitoolItem.SHEARS_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("item.minecraft.flint_and_steel"),
                    MultitoolScreen.render(itemStack, MultitoolItem.FLINT_AND_STEEL_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("item.minecraft.brush"),
                    MultitoolScreen.render(itemStack, MultitoolItem.BRUSH_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("item.minecraft.spyglass"),
                    MultitoolScreen.render(itemStack, MultitoolItem.SPYGLASS_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("item.anvilcraft.magnet"),
                    MultitoolScreen.render(itemStack, MultitoolItem.MAGNET_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("item.minecraft.fishing_rod"),
                    MultitoolScreen.render(itemStack, MultitoolItem.FISHING_ROD_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("item.minecraft.carrot_on_a_stick"),
                    MultitoolScreen.render(itemStack, MultitoolItem.CARROT_ON_A_STICK_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("item.minecraft.warped_fungus_on_a_stick"),
                    MultitoolScreen.render(itemStack, MultitoolItem.WARPED_FUNGUS_ON_A_STICK_MODE)
                )
            )
        ).setCurrentIndex(this.wheel != null ? this.wheel.getCurrentSectionIndex() : this.mode);
        this.wheel = this.addRenderableWidget(wheel);
    }

    @Override
    public void removed() {
        super.removed();
        PacketDistributor.sendToServer(new SwitchMultitoolModePacket(this.hand, this.wheel.getCurrentSectionIndex()));
    }

    private static WheelWidget.SectionRenderer render(ItemStack itemStack, int mode) {
        return (graphics, pose, width, height) -> {
            ItemStack item = itemStack.copy();
            item.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(mode));
            graphics.renderItem(item, 2, 2, 9910597);
        };
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
