package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.RecordMaterialButton;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import dev.dubhe.anvilcraft.inventory.component.AutoCrafterSlot;
import dev.dubhe.anvilcraft.network.MachineRecordMaterialPack;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AutoCrafterScreen extends BaseMachineScreen<AutoCrafterMenu> {
    private final NonNullList<Boolean> disabled = NonNullList.withSize(9, false);
    private final NonNullList<ItemStack> filter = NonNullList.withSize(9, ItemStack.EMPTY);
    private static final ResourceLocation CONTAINER_LOCATION = AnvilCraft.of("textures/gui/container/auto_crafter.png");
    private static final ResourceLocation DISABLED_SLOT = AnvilCraft.of("textures/gui/container/disabled_slot.png");
    Supplier<RecordMaterialButton> materialButtonSupplier = () -> new RecordMaterialButton(this.leftPos + 116, this.topPos + 18, button -> {
        if (button instanceof RecordMaterialButton button1) {
            MachineRecordMaterialPack packet = new MachineRecordMaterialPack(button1.next());
            packet.send();
        }
    }, false);
    @Getter
    private RecordMaterialButton recordButton = null;
    @Getter
    private final Player player;

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
    }

    @Override
    protected void slotClicked(Slot slot, int i, int j, ClickType clickType) {
        if (slot instanceof AutoCrafterSlot && !slot.hasItem() && !this.getPlayer().isSpectator()) {
            switch (clickType) {
                case PICKUP: {
                    if (this.disabled.get(i)) {
                        this.enableSlot(i);
                        break;
                    }
                    if (!this.menu.getCarried().isEmpty() && this.recordButton != null && this.recordButton.isRecord()) {
                        this.setFilter(i, this.menu.getCarried());
                        break;
                    }
                    this.disableSlot(i);
                    break;
                }
                case SWAP: {
                    ItemStack itemStack = this.player.getInventory().getItem(j);
                    if (!this.disabled.get(i) || itemStack.isEmpty()) break;
                    if (!this.menu.getCarried().isEmpty() && this.recordButton != null && this.recordButton.isRecord()) {
                        this.setFilter(i, this.menu.getCarried());
                    }
                    this.enableSlot(i);
                }
            }
        }
        super.slotClicked(slot, i, j, clickType);
    }

    private void enableSlot(int i) {
        new SlotDisableChangePack(i, false).send();
    }

    private void disableSlot(int i) {
        new SlotDisableChangePack(i, true).send();
        new SlotFilterChangePack(i, ItemStack.EMPTY).send();
    }

    private void setFilter(int index, ItemStack filter) {
        new SlotFilterChangePack(index, filter).send();
    }

    public void changeSlotDisable(int index, boolean state) {
        this.menu.setSlotDisabled(index, state);
        this.disabled.set(index, state);
    }

    public void changeSlotFilter(int index, ItemStack filter) {
        this.menu.setFilter(index, filter);
        this.filter.set(index, filter);
    }

    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);
        if (!(slot instanceof AutoCrafterSlot crafterSlot)) {
            return;
        }
        if (this.disabled.get(slot.index)) {
            this.renderDisabledSlot(guiGraphics, crafterSlot);
            return;
        }
        ItemStack filter = this.filter.get(slot.index);
        if (!slot.hasItem() && !filter.isEmpty()) {
            this.renderFilterItem(guiGraphics, slot, filter);
        }
    }

    private void renderDisabledSlot(@NotNull GuiGraphics guiGraphics, @NotNull AutoCrafterSlot crafterSlot) {
        RenderSystem.enableDepthTest();
        guiGraphics.blit(DISABLED_SLOT, crafterSlot.x, crafterSlot.y, 0, 0, 16, 16, 16, 16);
    }

    private void renderFilterItem(@NotNull GuiGraphics guiGraphics, @NotNull Slot slot, @NotNull ItemStack stack) {
        int i = slot.x;
        int j = slot.y;
        guiGraphics.renderFakeItem(stack, i, j);
        guiGraphics.fill(i, j, i + 16, j + 16, 0x80ffaaaa);
    }

    @Override
    protected void init() {
        super.init();
        this.recordButton = materialButtonSupplier.get();
        this.addRenderableWidget(recordButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        logic:
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            int index = this.hoveredSlot.index;
            if (index >= 9) break logic;
            ItemStack itemStack = this.filter.get(index);
            if (itemStack.isEmpty()) break logic;
            guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(itemStack), itemStack.getTooltipImage(), x, y);
        }
        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
