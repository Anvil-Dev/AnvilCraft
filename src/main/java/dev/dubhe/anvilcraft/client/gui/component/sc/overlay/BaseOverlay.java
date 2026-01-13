package dev.dubhe.anvilcraft.client.gui.component.sc.overlay;

import dev.dubhe.anvilcraft.api.SyncListener;
import dev.dubhe.anvilcraft.client.gui.screen.ShulkerContainerScreen;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseOverlay extends AbstractWidget implements SyncListener<ClientSCStorage> {
    protected final ShulkerContainerScreen screen;

    public BaseOverlay(ShulkerContainerScreen screen) {
        super(screen.getGuiLeft(), screen.getGuiTop(), 300, 222, Component.empty());
        this.screen = screen;
    }

    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        return this.screen.addRenderableWidget(widget);
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

    protected void setTooltip(ItemStack stack) {
        List<FormattedCharSequence> tooltips = new ArrayList<>();
        for (Component component : Screen.getTooltipFromItem(this.minecraft(), stack)) {
            tooltips.add(component.getVisualOrderText());
        }
        this.setTooltip(tooltips);
    }

    protected void setTooltip(List<FormattedCharSequence> tooltip) {
        this.screen.setTooltipForNextRenderPass(tooltip);
    }

    protected void setTooltip(Component tooltip) {
        this.screen.setTooltipForNextRenderPass(tooltip);
    }

    @Override
    public void setTooltip(@Nullable Tooltip tooltip) {
        this.screen.setTooltipForNextRenderPass(tooltip.toCharSequence(this.minecraft()));
    }

    public int getGuiLeft() {
        return this.screen.getGuiLeft();
    }

    public int getGuiTop() {
        return this.screen.getGuiTop();
    }

    @Override
    public void whenSynced(ClientSCStorage storage) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        if (!this.isValidClickButton(button)) return false;
        if (!this.clicked(mouseX, mouseY)) return false;
        if (this.whenClick(mouseX, mouseY, button)) this.playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
    }

    public void onClose() {
    }

    public boolean whenClick(double mouseX, double mouseY, int button) {
        super.onClick(mouseX, mouseY, button);
        return false;
    }

    public Minecraft minecraft() {
        return this.screen.getMinecraft();
    }

    public ClientSCStorage storage() {
        return this.screen.storage;
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
