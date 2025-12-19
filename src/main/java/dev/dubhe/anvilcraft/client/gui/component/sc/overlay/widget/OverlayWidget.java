package dev.dubhe.anvilcraft.client.gui.component.sc.overlay.widget;

import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.client.gui.screen.ShulkerContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public abstract class OverlayWidget extends AbstractWidget {
    protected final ShulkerContainerScreen screen;

    public OverlayWidget(ShulkerContainerScreen screen, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.screen = screen;
    }

    @Override
    protected abstract void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    public int getGuiLeft() {
        return this.screen.getGuiLeft();
    }

    public int getGuiTop() {
        return this.screen.getGuiTop();
    }

    public ContainerStorage storage() {
        return this.screen.getMenu().storage;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
