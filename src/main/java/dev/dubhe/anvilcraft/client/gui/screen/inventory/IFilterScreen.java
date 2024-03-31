package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.RecordMaterialButton;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import dev.dubhe.anvilcraft.network.MachineRecordMaterialPack;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public interface IFilterScreen {
    ResourceLocation DISABLED_SLOT = AnvilCraft.of("textures/gui/container/disabled_slot.png");

    NonNullList<Boolean> getDisabled();

    NonNullList<ItemStack> getFilter();

    RecordMaterialButton getRecordButton();

    ItemStack getCarried();
    @Nullable Slot getHoveredSlot();

    Font getFont();

    IFilterMenu getMenu();

    Player getPlayer();

    List<Component> getTooltipFromContainerItem(ItemStack stack);

    default boolean isRecord() {
        return this.getRecordButton() != null && this.getRecordButton().isRecord();
    }

    default void enableSlot(int i) {
        new SlotDisableChangePack(i, false).send();
    }

    default void disableSlot(int i) {
        new SlotDisableChangePack(i, true).send();
        new SlotFilterChangePack(i, ItemStack.EMPTY).send();
    }

    default void setFilter(int index, ItemStack filter) {
        new SlotFilterChangePack(index, filter).send();
    }

    default void changeSlotDisable(int index, boolean state) {
        this.getMenu().setSlotDisabled(index, state);
        this.getDisabled().set(index, state);
    }

    default void changeSlotFilter(int index, ItemStack filter) {
        this.getMenu().setFilter(index, filter);
        this.getFilter().set(index, filter);
    }

    default void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        logic:
        if (this.getCarried().isEmpty() && this.getHoveredSlot() != null && !this.getHoveredSlot().hasItem()) {
            int index = this.getHoveredSlot().index;
            if (index >= 9) break logic;
            ItemStack itemStack = this.getFilter().get(index);
            if (itemStack.isEmpty()) {
                if (isRecord() && getDisabled().get(index)) {
                    guiGraphics.renderTooltip(getFont(), List.of(Component.translatable("screen.anvilcraft.button.record.tooltip")), Optional.empty(), x, y);
                }
            } else {
                guiGraphics.renderTooltip(this.getFont(), this.getTooltipFromContainerItem(itemStack), itemStack.getTooltipImage(), x, y);
            }
        }
    }

    default BiFunction<Integer, Integer, RecordMaterialButton> getMaterialButtonSupplier(int x, int y) {
        return (i, j) -> new RecordMaterialButton(i + x, j + y, button -> {
            if (button instanceof RecordMaterialButton button1) {
                MachineRecordMaterialPack packet = new MachineRecordMaterialPack(button1.next());
                packet.send();
            }
        }, false);
    }

    default void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (!(slot instanceof FilterSlot crafterSlot)) {
            return;
        }
        if (this.getDisabled().get(slot.index)) {
            this.renderDisabledSlot(guiGraphics, crafterSlot);
            return;
        }
        ItemStack filter = this.getFilter().get(slot.index);
        if (!slot.hasItem() && !filter.isEmpty()) {
            this.renderFilterItem(guiGraphics, slot, filter);
        }
    }

    default void renderDisabledSlot(@NotNull GuiGraphics guiGraphics, @NotNull FilterSlot crafterSlot) {
        RenderSystem.enableDepthTest();
        guiGraphics.blit(DISABLED_SLOT, crafterSlot.x, crafterSlot.y, 0, 0, 16, 16, 16, 16);
    }

    default void renderFilterItem(@NotNull GuiGraphics guiGraphics, @NotNull Slot slot, @NotNull ItemStack stack) {
        int i = slot.x;
        int j = slot.y;
        guiGraphics.renderFakeItem(stack, i, j);
        guiGraphics.fill(i, j, i + 16, j + 16, 0x80ffaaaa);
    }

    default void slotClicked(Slot slot, int i, int j, ClickType clickType) {
        if (slot instanceof FilterSlot && !slot.hasItem() && !this.getPlayer().isSpectator()) {
            switch (clickType) {
                case PICKUP: {
                    if (this.getDisabled().get(i)) {
                        this.enableSlot(i);
                        break;
                    }
                    if (!this.getCarried().isEmpty() && this.isRecord()) {
                        this.setFilter(i, this.getCarried());
                        break;
                    }
                    this.disableSlot(i);
                    break;
                }
                case SWAP: {
                    ItemStack itemStack = this.getPlayer().getInventory().getItem(j);
                    if (!this.getDisabled().get(i) || itemStack.isEmpty()) break;
                    if (!this.getCarried().isEmpty() && this.isRecord()) {
                        this.setFilter(i, this.getCarried());
                    }
                    this.enableSlot(i);
                }
            }
        }
    }
}
