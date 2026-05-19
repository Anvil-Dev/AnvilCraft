package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.client.gui.component.CycleFilterModeButton;
import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import dev.dubhe.anvilcraft.inventory.component.FilterOnlySlot;
import dev.dubhe.anvilcraft.network.ItemDetectorChangeRangePacket;
import dev.dubhe.anvilcraft.network.MachineCycleFilterModePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.Optional;

public class ItemDetectorScreen extends AbstractContainerScreen<ItemDetectorMenu>
    implements IFilterScreen<ItemDetectorMenu> {

    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "item_detector");
    private final Component scrollToChangeTooltip =
        Component.translatable("screen.anvilcraft.filter.scroll_to_change")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    private final Component shiftToScrollFasterTooltip =
        Component.translatable("screen.anvilcraft.filter.shift_to_scroll_faster")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);

    protected CycleFilterModeButton cycleFilterModeButton;

    public ItemDetectorScreen(ItemDetectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            this.getImageWidth(),
            this.getImageHeight()
        );
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        // filter mode
        this.cycleFilterModeButton = new CycleFilterModeButton(
            leftPos + 75,
            topPos + 54,
            b -> {
                if (!(b instanceof CycleFilterModeButton button)) return;
                ClientPacketDistributor.sendToServer(new MachineCycleFilterModePacket(button.cycle()));
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
            () -> Component.literal(String.valueOf(this.menu.getBlockEntity().getRange()))
        ));
        // range - +
        this.addRenderableWidget(new ItemCollectorButton(
            leftPos + 43,
            topPos + 23,
            "minus",
            _ -> {
                this.menu.getBlockEntity().decreaseRange();
                ClientPacketDistributor.sendToServer(
                    new ItemDetectorChangeRangePacket(this.menu.getBlockEntity().getRange())
                );
            }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            leftPos + 81,
            topPos + 23,
            "add",
            _ -> {
                this.menu.getBlockEntity().increaseRange();
                ClientPacketDistributor.sendToServer(
                    new ItemDetectorChangeRangePacket(this.menu.getBlockEntity().getRange())
                );
            }
        ));
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(graphics, slot, mouseX, mouseY);
        if (slot instanceof FilterOnlySlot && slot.getItem().isEmpty()) {
            this.extractDisabledSlot(graphics, slot);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        this.extractSlotTooltip(graphics, mouseX, mouseY);
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

    protected void extractSlotTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.hoveringEmptyFilterSlot()) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.anvilcraft.slot.disable.tooltip"), x, y);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> components = super.getTooltipFromContainerItem(stack);
        if (this.hoveringNonEmptyFilterSlot()) {
            components.add(this.scrollToChangeTooltip);
            components.add(Component.translatable("screen.anvilcraft.filter.scroll_to_change")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            components.add(this.shiftToScrollFasterTooltip);
        }
        return components;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (slot instanceof FilterOnlySlot filterSlot) {
            ItemStack filterStack = this.menu.getCarried();
            if (filterStack.isEmpty() && !this.minecraft.hasShiftDown()) return;
            int id = slot.getContainerSlot();
            if (!filterStack.isEmpty() && buttonNum == InputConstants.MOUSE_BUTTON_RIGHT) {
                filterStack = filterStack.copyWithCount(1);
            } else {
                filterStack = filterStack.copy();
            }
            filterSlot.set(filterStack);
            ClientPacketDistributor.sendToServer(new SlotFilterChangePacket(id, filterStack, false));
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
    }

    private int getScrollSpeed() {
        return this.minecraft.hasShiftDown() ? 5 : 1;
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
            ClientPacketDistributor.sendToServer(new SlotFilterChangePacket(
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
