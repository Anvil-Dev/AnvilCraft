package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.CycleFilterModeButton;
import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import dev.dubhe.anvilcraft.inventory.component.FilterOnlySlot;
import dev.dubhe.anvilcraft.network.ItemDetectorChangeRangePacket;
import dev.dubhe.anvilcraft.network.MachineCycleFilterModePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemDetectorScreen extends AbstractContainerScreen<ItemDetectorMenu>
    implements IFilterScreen<ItemDetectorMenu> {

    private static final ResourceLocation BACKGROUND_LOCATION =
        AnvilCraft.of("textures/gui/container/machine/background/item_detector.png");
    private final Component scrollToChangeTooltip =
        Component.translatable("screen.anvilcraft.filter.scroll_to_change")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    private final Component shiftToScrollFasterTooltip =
        Component.translatable("screen.anvilcraft.filter.shift_to_scroll_faster")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);

    private CycleFilterModeButton cycleFilterModeButton;

    public ItemDetectorScreen(ItemDetectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void init() {
        super.init();
        // filter mode
        this.cycleFilterModeButton = new CycleFilterModeButton(
            leftPos + 75,
            topPos + 54,
            b -> {
                if (!(b instanceof CycleFilterModeButton button)) return;
                PacketDistributor.sendToServer(new MachineCycleFilterModePacket(button.cycle()));
                this.menu.setFilterMode(button.cycle());
            },
            () -> this.menu.getBlockEntity().getFilterMode()
        );
        this.addRenderableWidget(this.cycleFilterModeButton);
        // range
        this.addRenderableWidget(new TextWidget(
            leftPos + 57,
            topPos + 24,
            20,
            8,
            Minecraft.getInstance().font,
            () -> Component.literal(
                String.valueOf(this.menu.getBlockEntity().getRange()))));
        // range - +
        this.addRenderableWidget(new ItemCollectorButton(leftPos + 43, topPos + 23, "minus", (b) -> {
            this.menu.getBlockEntity().decreaseRange();
            PacketDistributor.sendToServer(
                new ItemDetectorChangeRangePacket(this.menu.getBlockEntity().getRange())
            );
        }));
        this.addRenderableWidget(new ItemCollectorButton(leftPos + 81, topPos + 23, "add", (b) -> {
            this.menu.getBlockEntity().increaseRange();
            PacketDistributor.sendToServer(
                new ItemDetectorChangeRangePacket(this.menu.getBlockEntity().getRange())
            );
        }));
    }

    @Override
    public void renderSlot(@NotNull GuiGraphics guiGraphics, @NotNull Slot slot) {
        super.renderSlot(guiGraphics, slot);
        if (slot instanceof FilterOnlySlot && slot.getItem().isEmpty()) {
            this.renderDisabledSlot(guiGraphics, slot);
        }
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        this.renderSlotTooltip(guiGraphics, x, y);
    }

    private boolean hoveringNonEmptyFilterSlot() {
        return Optional.ofNullable(this.hoveredSlot)
            .map(h -> h instanceof FilterOnlySlot && h.hasItem())
            .orElse(false);
    }

    private boolean hoveringEmptyFilterSlot() {
        return Optional.ofNullable(this.hoveredSlot)
            .map(h -> h instanceof FilterOnlySlot && !h.hasItem())
            .orElse(false);
    }

    protected void renderSlotTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveringEmptyFilterSlot()) {
            guiGraphics.renderTooltip(this.font, Component.translatable("screen.anvilcraft.slot.disable.tooltip"), x, y);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> components = super.getTooltipFromContainerItem(stack);
        if (this.hoveringNonEmptyFilterSlot()) {
            components.add(scrollToChangeTooltip);
            components.add(shiftToScrollFasterTooltip);
        }
        return components;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void slotClicked(@NotNull Slot slot, int slotId, int button, @NotNull ClickType type) {
        if (type == ClickType.PICKUP
            && slot instanceof FilterOnlySlot filterSlot
            && (button == InputConstants.MOUSE_BUTTON_LEFT
            || button == InputConstants.MOUSE_BUTTON_RIGHT)) {
            ItemStack filterStack = this.menu.getCarried();
            int id = slot.getContainerSlot();
            if (!filterStack.isEmpty() && button == InputConstants.MOUSE_BUTTON_RIGHT) {
                filterStack = filterStack.copyWithCount(1);
            } else {
                filterStack = filterStack.copy();
            }
            filterSlot.set(filterStack);
            PacketDistributor.sendToServer(new SlotFilterChangePacket(id, filterStack, false));
            return;
        }
        super.slotClicked(slot, slotId, button, type);
    }

    private int getScrollSpeed() {
        return hasShiftDown() ? 5 : 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Slot slot = this.hoveredSlot;
        if (slot instanceof FilterOnlySlot filterSlot && scrollY != 0) {
            ItemStack item = filterSlot.getItem();
            int countBefore = item.getCount();
            int countAfter = countBefore + this.getScrollSpeed() * (scrollY > 0 ? 1 : -1);
            countAfter = Mth.clamp(countAfter, 1, item.getMaxStackSize());
            ItemStack newItem = item.copyWithCount(countAfter);
            filterSlot.set(newItem);
            PacketDistributor.sendToServer(new SlotFilterChangePacket(
                filterSlot.getContainerSlot(),
                newItem,
                false
            ));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public ItemDetectorMenu getFilterMenu() {
        return this.menu;
    }
}
