package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class ItemCollectorScreen extends AbstractContainerScreen<ItemCollectorMenu> implements IFilterScreen {
    private static final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/machine/background/item_collector.png");
    BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier =
            this.getEnableFilterButtonSupplier(75, 55);

    @Getter
    private EnableFilterButton enableFilterButton = null;
    private final TextWidget rangeWidget;
    private final TextWidget cooldownWidget;
    private final TextWidget powerCostWidget;
    private final ItemCollectorMenu menu;

    public ItemCollectorScreen(ItemCollectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.menu = menu;
        this.minecraft = Minecraft.getInstance();
        rangeWidget = new TextWidget(
                57, 24,
                20, 8,
                minecraft.font,
                () -> Component.literal(menu.getBlockEntity().getRangeRadius().get().toString())
        );
        cooldownWidget = new TextWidget(
                57, 38,
                20, 8,
                minecraft.font,
                () -> Component.literal(menu.getBlockEntity().getCooldown().get().toString())
        );
        powerCostWidget = new TextWidget(
                43, 57,
                20, 8,
                minecraft.font,
                () -> Component.literal(Integer.toString(menu.getBlockEntity().getInputPower()))
        );
    }

    @Override
    protected void init() {
        super.init();
        this.enableFilterButton = enableFilterButtonSupplier.apply(this.leftPos, this.topPos);
        this.addRenderableWidget(this.enableFilterButton);
        this.addRenderableWidget(rangeWidget);
        this.addRenderableWidget(cooldownWidget);
        this.addRenderableWidget(powerCostWidget);
        //range - +
        this.addRenderableWidget(Button.builder(Component.literal("-"), (b) -> {
            menu.getBlockEntity().getRangeRadius().previous();
        }).bounds(43, 23, 10, 10).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), (b) -> {
            menu.getBlockEntity().getRangeRadius().next();
        }).bounds(81, 23, 10, 10).build());
        //cooldown - +
        this.addRenderableWidget(Button.builder(Component.literal("-"), (b) -> {
            menu.getBlockEntity().getCooldown().previous();
        }).bounds(43, 37, 10, 10).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), (b) -> {
            menu.getBlockEntity().getCooldown().next();
        }).bounds(81, 37, 10, 10).build());
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
