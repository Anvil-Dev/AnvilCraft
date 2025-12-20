package dev.dubhe.anvilcraft.client.gui.component.sc.overlay;

import dev.dubhe.anvilcraft.client.gui.screen.ShulkerContainerScreen;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class BaseOverlay extends AbstractWidget {
    protected final ShulkerContainerScreen screen;

    public BaseOverlay(ShulkerContainerScreen screen) {
        super(screen.getGuiLeft(), screen.getGuiTop(), 300, 222, Component.empty());
        this.screen = screen;
    }

    public abstract BaseOverlay recreate();

    public abstract ResourceLocation bg();

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(
            this.bg(),
            this.getGuiLeft(),
            this.getGuiTop(),
            0,
            0,
            300,
            222,
            512,
            256
        );
    }

    public void refreshTooltip(int x, int y) {
    }

    public void setTooltip(Component tooltip) {
        this.screen.setTooltipForNextRenderPass(tooltip);
    }

    public int getGuiLeft() {
        return this.screen.getGuiLeft();
    }

    public int getGuiTop() {
        return this.screen.getGuiTop();
    }

    public void whenSynced(ContainerStorage storage) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        if (!this.isValidClickButton(button)) return false;
        if (!this.clicked(mouseX, mouseY)) return false;
        if (this.whenClick(mouseX, mouseY, button)) this.playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
    }

    public boolean whenClick(double mouseX, double mouseY, int button) {
        super.onClick(mouseX, mouseY, button);
        return false;
    }

    public Minecraft minecraft() {
        return this.screen.getMinecraft();
    }

    public ContainerStorage storage() {
        return this.screen.getMenu().storage;
    }

    public boolean hasSlots() {
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        if (!this.active || !this.visible) return false;
        if (mouseX < this.getGuiLeft() + 106) {
            return mouseX >= this.getGuiLeft()
                   && mouseY >= this.getGuiTop()
                   && mouseY < this.getGuiTop() + 222;
        } else {
            return mouseX < this.getGuiLeft() + 300
                   && mouseY >= this.getGuiTop()
                   && mouseY < this.getGuiTop() + 133;
        }
    }
}
