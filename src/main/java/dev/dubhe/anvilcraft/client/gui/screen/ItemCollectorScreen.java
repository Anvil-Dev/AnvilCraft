package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

public class ItemCollectorScreen extends AbstractContainerScreen<ItemCollectorMenu>
        implements IFilterScreen<ItemCollectorMenu> {
    private static final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/machine/background/item_collector.png");
    BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier =
            this.getEnableFilterButtonSupplier(75, 54);

    @Getter
    private EnableFilterButton enableFilterButton = null;

    private final ItemCollectorMenu menu;

    /**
     * 物品收集器 Screen
     */
    public ItemCollectorScreen(ItemCollectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.menu = menu;
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        this.enableFilterButton = enableFilterButtonSupplier.apply(this.leftPos, this.topPos);
        this.addRenderableWidget(this.enableFilterButton);
        // range
        this.addRenderableWidget(new TextWidget(
                leftPos + 57,
                topPos + 24,
                20,
                8,
                minecraft.font,
                () -> Component.literal(
                        menu.getBlockEntity().getRangeRadius().get().toString())));
        // cooldown
        this.addRenderableWidget(new TextWidget(
                leftPos + 57,
                topPos + 38,
                20,
                8,
                minecraft.font,
                () -> Component.literal(
                        menu.getBlockEntity().getCooldown().get().toString())));
        // power cost
        this.addRenderableWidget(new TextWidget(
                leftPos + 43,
                topPos + 51,
                20,
                8,
                minecraft.font,
                () -> Component.literal(Integer.toString(menu.getBlockEntity().getInputPower()))));
        // range - +
        this.addRenderableWidget(new ItemCollectorButton(leftPos + 43, topPos + 23, "minus", (b) -> {
            menu.getBlockEntity().getRangeRadius().previous();
            menu.getBlockEntity().getRangeRadius().notifyServer();
        }));
        this.addRenderableWidget(new ItemCollectorButton(leftPos + 81, topPos + 23, "add", (b) -> {
            menu.getBlockEntity().getRangeRadius().next();
            menu.getBlockEntity().getRangeRadius().notifyServer();
        }));
        // cooldown - +
        this.addRenderableWidget(new ItemCollectorButton(leftPos + 43, topPos + 37, "minus", (b) -> {
            menu.getBlockEntity().getCooldown().previous();
            menu.getBlockEntity().getCooldown().notifyServer();
        }));
        this.addRenderableWidget(new ItemCollectorButton(leftPos + 81, topPos + 37, "add", (b) -> {
            menu.getBlockEntity().getCooldown().next();
            menu.getBlockEntity().getCooldown().notifyServer();
        }));
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
    public ItemCollectorMenu getFilterMenu() {
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
                    if (carriedItem.isEmpty()) {
                        PacketDistributor.sendToServer(
                                new SlotDisableChangePacket(realSlotId, !this.menu.isSlotDisabled(realSlotId)));
                    } else {
                        PacketDistributor.sendToServer(new SlotDisableChangePacket(realSlotId, false));
                    }
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
