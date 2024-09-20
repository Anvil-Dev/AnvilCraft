package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.inventory.BatchCrafterMenu;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;

import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class BatchCrafterScreen extends BaseMachineScreen<BatchCrafterMenu> implements IFilterScreen<BatchCrafterMenu> {
    private static final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/machine/background/auto_crafter.png");
    BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier =
            this.getEnableFilterButtonSupplier(116, 18);

    @Getter
    private EnableFilterButton enableFilterButton = null;

    private final BatchCrafterMenu menu;

    public BatchCrafterScreen(BatchCrafterMenu menu, Inventory playerInventory, Component title) {
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
        if (!(this.hoveredSlot instanceof SlotItemHandlerWithFilter)) return;
        if (!((SlotItemHandlerWithFilter) this.hoveredSlot).isFilter()) return;
        if (!this.isFilterEnabled()) return;
        if (!this.isSlotDisabled(this.hoveredSlot.getContainerSlot())) return;
        guiGraphics.renderTooltip(this.font, Component.translatable("screen.anvilcraft.slot.disable.tooltip"), x, y);
    }

    @Override
    public BatchCrafterMenu getFilterMenu() {
        return this.menu;
    }

    @Override
    public void flush() {
        this.enableFilterButton.flush();
    }

    protected void slotClicked(@NotNull Slot slot, int slotId, int mouseButton, @NotNull ClickType type) {
        if (type == ClickType.PICKUP) {
            if (slot instanceof SlotItemHandlerWithFilter && slot.getItem().isEmpty()) {
                ItemStack carriedItem = this.menu.getCarried();
                int realSlotId = slot.getContainerSlot();
                if (this.menu.isFilterEnabled()) {
                    PacketDistributor.sendToServer(new SlotDisableChangePacket(realSlotId, false));
                    PacketDistributor.sendToServer(new SlotFilterChangePacket(realSlotId, carriedItem.copy()));
                    menu.setSlotDisabled(realSlotId, false);
                    menu.setFilter(realSlotId, carriedItem.copy());
                    slot.set(carriedItem);
                } else {
                    PacketDistributor.sendToServer(new SlotDisableChangePacket(realSlotId, !this.menu.isSlotDisabled(realSlotId)));
                }
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public int getOffsetX() {
        return (this.width - this.imageWidth) / 2;
    }

    @Override
    public int getOffsetY() {
        return (this.height - this.imageHeight) / 2;
    }
}
