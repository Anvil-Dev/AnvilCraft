package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.client.gui.component.OutputDirectionButton;
import dev.dubhe.anvilcraft.inventory.BaseMachineMenu;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.Supplier;

public abstract class BaseMachineScreen<T extends BaseMachineMenu> extends AbstractContainerScreen<T> {
    private final Supplier<OutputDirectionButton> directionButtonSupplier = () -> new OutputDirectionButton(this.leftPos + 134, this.topPos + 18, button -> {
        if (button instanceof OutputDirectionButton button1) {
            MachineOutputDirectionPack packet = new MachineOutputDirectionPack(button1.next());
            packet.send();
        }
    }, Direction.DOWN);
    @Getter
    private OutputDirectionButton directionButton = null;

    public BaseMachineScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.directionButton = directionButtonSupplier.get();
        this.addRenderableWidget(directionButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
