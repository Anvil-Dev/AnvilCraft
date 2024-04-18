package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class AutoCrafterScreen extends BaseMachineScreen<AutoCrafterMenu> implements IFilterScreen {
    private static final ResourceLocation CONTAINER_LOCATION =
        AnvilCraft.of("textures/gui/container/machine/background/auto_crafter.png");
    BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier = this
        .getEnableFilterButtonSupplier(116, 18);
    @Getter
    private EnableFilterButton enableFilterButton = null;
    private final AutoCrafterMenu menu;

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        this.enableFilterButton = enableFilterButtonSupplier.apply(this.leftPos, this.topPos);
        this.addRenderableWidget(this.enableFilterButton);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void renderSlot(@NotNull GuiGraphics guiGraphics, @NotNull Slot slot) {
        super.renderSlot(guiGraphics, slot);
        IFilterScreen.super.renderSlot(guiGraphics, slot);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        this.renderSlotTooltip(guiGraphics, x, y);
    }

    protected void renderSlotTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot == null) return;
        if (!(this.hoveredSlot instanceof ItemDepositorySlot)) return;
        if (!((ItemDepositorySlot) this.hoveredSlot).isFilter()) return;
        if (!this.isFilterEnabled()) return;
        if (!this.isSlotDisabled(this.hoveredSlot.getContainerSlot())) return;
        guiGraphics.renderTooltip(this.font, Component.translatable("screen.anvilcraft.slot.disable.tooltip"), x, y);
    }

    @Override
    public IFilterMenu getFilterMenu() {
        return this.menu;
    }

    @Override
    public void flush() {
        this.enableFilterButton.flush();
    }

    @Override
    protected void slotClicked(@NotNull Slot slot, int slotId, int mouseButton, @NotNull ClickType type) {
        start:
        if (type == ClickType.PICKUP) {
            if (!this.menu.getCarried().isEmpty()) break start;
            if (!(slot instanceof ItemDepositorySlot)) break start;
            if (!slot.getItem().isEmpty()) break start;
            int slot1 = slot.getContainerSlot();
            if (this.menu.isFilterEnabled()) {
                if (!this.menu.isSlotDisabled(slot1)) new SlotDisableChangePack(slot1, false).send();
                break start;
            }
            new SlotDisableChangePack(slot1, !this.menu.isSlotDisabled(slot1)).send();
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }
}
