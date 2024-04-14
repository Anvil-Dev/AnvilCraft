package dev.dubhe.anvilcraft.client.gui.screen.inventory;


import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class ChuteScreen extends BaseMachineScreen<ChuteMenu> implements IFilterScreen {
    private static final ResourceLocation CONTAINER_LOCATION = AnvilCraft.of("textures/gui/container/chute.png");


    BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier = this
        .getEnableFilterButtonSupplier(134, 36);
    @Getter
    private EnableFilterButton enableFilterButton = null;
    private final ChuteMenu menu;

    public ChuteScreen(ChuteMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        this.enableFilterButton = enableFilterButtonSupplier.apply(this.leftPos, this.topPos);
        this.getDirectionButton().skip(Direction.UP);
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
    public IFilterMenu getFilterMenu() {
        return menu;
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
