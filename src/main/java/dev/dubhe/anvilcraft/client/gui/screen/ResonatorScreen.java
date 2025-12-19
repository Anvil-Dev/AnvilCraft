package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.WheelWidget;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import dev.dubhe.anvilcraft.network.SwitchResonateModePacket;
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

public class ResonatorScreen extends Screen {
    private final LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
    private final InteractionHand hand;
    private final int mode;

    public WheelWidget wheel;

    public ResonatorScreen(InteractionHand hand, int mode) {
        super(Component.translatable("screen.anvilcraft.resonator.title"));
        this.hand = hand;
        this.mode = mode;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int leftPos = (this.width - 75) / 2;
        int topPos = (this.height - 75) / 2;
        ItemStack holding = this.player.getItemInHand(this.hand);
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
                    Component.translatable("screen.anvilcraft.resonator.auto"),
                    ResonatorScreen.renderResonator(holding, ResonatorItem.AUTO_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("screen.anvilcraft.resonator.axe"),
                    ResonatorScreen.renderResonator(holding, ResonatorItem.AXE_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("screen.anvilcraft.resonator.shovel"),
                    ResonatorScreen.renderResonator(holding, ResonatorItem.SHOVEL_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("screen.anvilcraft.resonator.hoe"),
                    ResonatorScreen.renderResonator(holding, ResonatorItem.HOE_MODE)
                ),
                new WheelWidget.RawSection(
                    Component.translatable("screen.anvilcraft.resonator.pickaxe"),
                    ResonatorScreen.renderResonator(holding, ResonatorItem.PICKAXE_MODE)
                )
            )
        ).setCurrentIndex(this.wheel != null ? this.wheel.getCurrentSectionIndex() : this.mode);
        this.wheel = this.addRenderableWidget(wheel);
    }

    private static WheelWidget.SectionRenderer renderResonator(ItemStack holding, int mode) {
        return (graphics, pose, width, height) -> {
            ItemStack stack = holding.copy();
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(mode));
            graphics.renderItem(stack, 2, 2, 9910597);
        };
    }

    @Override
    public void removed() {
        super.removed();
        PacketDistributor.sendToServer(new SwitchResonateModePacket(this.hand, this.wheel.getCurrentSectionIndex()));
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
