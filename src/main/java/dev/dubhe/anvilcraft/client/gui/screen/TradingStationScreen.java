package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.TradingStationMenu;
import dev.dubhe.anvilcraft.inventory.component.FilterOnlySlot;
import dev.dubhe.anvilcraft.item.FilterItem;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterMaxStackSizeChangePacket;
import dev.dubhe.anvilcraft.network.multiple.TradingStationPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.List;

public class TradingStationScreen extends AbstractContainerScreen<TradingStationMenu>
    implements IFilterScreen<TradingStationMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "trading_station");
    private static final ResourceLocation PLAYER_NOT_ALLOW = SharedTextures.textureGui("machine/trading_station/player_not_allow");
    private static final ResourceLocation PLAYER_ALLOW = SharedTextures.textureGui("machine/trading_station/player_allow");
    private static final ResourceLocation VILLAGER_NOT_ALLOW = SharedTextures.textureGui("machine/trading_station/villager_not_allow");
    private static final ResourceLocation VILLAGER_ALLOW = SharedTextures.textureGui("machine/trading_station/villager_allow");
    private static final ResourceLocation INPUT_NOT_ALLOW = SharedTextures.textureGui("machine/trading_station/input_not_allow");
    private static final ResourceLocation INPUT_ALLOW = SharedTextures.textureGui("machine/trading_station/input_allow");
    private static final ResourceLocation OUTPUT_NOT_ALLOW = SharedTextures.textureGui("machine/trading_station/output_not_allow");
    private static final ResourceLocation OUTPUT_ALLOW = SharedTextures.textureGui("machine/trading_station/output_allow");
    private final TradingStationMenu menu;
    private SwitchableButton playerAllowed;
    private SwitchableButton villagerAllowed;
    private SwitchableButton inputAllowed;
    private SwitchableButton outputAllowed;

    public TradingStationScreen(TradingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);
        IFilterScreen.super.renderSlot(guiGraphics, slot);
    }

    @Override
    protected void init() {
        super.init();

        this.playerAllowed = this.addRenderableWidget(new SwitchableButton(
            this.leftPos + 8,
            this.topPos + 50,
            16,
            16,
            List.of(
                PLAYER_NOT_ALLOW,
                PLAYER_ALLOW
            ),
            16,
            16,
            32,
            (btn, index) -> {
                TradingStationBlockEntity be = this.menu.getBe();
                be.setPlayerAllowed(index == 1);
                PacketDistributor.sendToServer(new TradingStationPackets.SyncAllowing(
                    be.getBlockPos(),
                    index == 1,
                    be.isVillagerAllowed(),
                    be.isInputAllowed(),
                    be.isOutputAllowed()
                ));
            }
        ));
        this.playerAllowed.setCurrent(this.menu.getBe().isPlayerAllowed() ? 1 : 0);
        this.villagerAllowed = this.addRenderableWidget(new SwitchableButton(
            this.leftPos + 26,
            this.topPos + 50,
            16,
            16,
            List.of(
                VILLAGER_NOT_ALLOW,
                VILLAGER_ALLOW
            ),
            16,
            16,
            32,
            (btn, index) -> {
                TradingStationBlockEntity be = this.menu.getBe();
                be.setVillagerAllowed(index == 1);
                PacketDistributor.sendToServer(new TradingStationPackets.SyncAllowing(
                    be.getBlockPos(),
                    be.isPlayerAllowed(),
                    index == 1,
                    be.isInputAllowed(),
                    be.isOutputAllowed()
                ));
            }
        ));
        this.villagerAllowed.setCurrent(this.menu.getBe().isVillagerAllowed() ? 1 : 0);
        this.inputAllowed = this.addRenderableWidget(new SwitchableButton(
            this.leftPos + 51,
            this.topPos + 50,
            16,
            16,
            List.of(
                INPUT_NOT_ALLOW,
                INPUT_ALLOW
            ),
            16,
            16,
            32,
            (btn, index) -> {
                TradingStationBlockEntity be = this.menu.getBe();
                be.setInputAllowed(index == 1);
                PacketDistributor.sendToServer(new TradingStationPackets.SyncAllowing(
                    be.getBlockPos(),
                    be.isPlayerAllowed(),
                    be.isVillagerAllowed(),
                    index == 1,
                    be.isOutputAllowed()
                ));
            }
        ));
        this.inputAllowed.setCurrent(this.menu.getBe().isInputAllowed() ? 1 : 0);
        this.outputAllowed = this.addRenderableWidget(new SwitchableButton(
            this.leftPos + 69,
            this.topPos + 50,
            16,
            16,
            List.of(
                OUTPUT_NOT_ALLOW,
                OUTPUT_ALLOW
            ),
            16,
            16,
            32,
            (btn, index) -> {
                TradingStationBlockEntity be = this.menu.getBe();
                be.setOutputAllowed(index == 1);
                PacketDistributor.sendToServer(new TradingStationPackets.SyncAllowing(
                    be.getBlockPos(),
                    be.isPlayerAllowed(),
                    be.isVillagerAllowed(),
                    be.isInputAllowed(),
                    index == 1
                ));
            }
        ));
        this.outputAllowed.setCurrent(this.menu.getBe().isOutputAllowed() ? 1 : 0);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 8, this.topPos + 25, this.leftPos + 24, this.topPos + 41)) {
            this.renderFilterSlotTooltip(
                guiGraphics,
                this.menu.getSlot(48),
                Component.translatable("screen.anvilcraft.trading_station.provide"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 26, this.topPos + 25, this.leftPos + 42, this.topPos + 41)) {
            this.renderFilterSlotTooltip(
                guiGraphics,
                this.menu.getSlot(49),
                Component.translatable("screen.anvilcraft.trading_station.provide"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 67, this.topPos + 25, this.leftPos + 83, this.topPos + 41)) {
            this.renderFilterSlotTooltip(
                guiGraphics,
                this.menu.getSlot(50),
                Component.translatable("screen.anvilcraft.trading_station.request"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 8, this.topPos + 50, this.leftPos + 24, this.topPos + 66)) {
            guiGraphics.renderTooltip(
                this.font,
                this.playerAllowed.getCurrent() == 0
                ? Component.translatable("screen.anvilcraft.trading_station.player_not_allow")
                : Component.translatable("screen.anvilcraft.trading_station.player_allow"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 26, this.topPos + 50, this.leftPos + 43, this.topPos + 66)) {
            guiGraphics.renderTooltip(
                this.font,
                this.villagerAllowed.getCurrent() == 0
                ? Component.translatable("screen.anvilcraft.trading_station.villager_not_allow")
                : Component.translatable("screen.anvilcraft.trading_station.villager_allow"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 51, this.topPos + 50, this.leftPos + 67, this.topPos + 66)) {
            guiGraphics.renderTooltip(
                this.font,
                this.inputAllowed.getCurrent() == 0
                ? Component.translatable("screen.anvilcraft.trading_station.input_not_allow")
                : Component.translatable("screen.anvilcraft.trading_station.input_allow"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 69, this.topPos + 50, this.leftPos + 85, this.topPos + 66)) {
            guiGraphics.renderTooltip(
                this.font,
                this.outputAllowed.getCurrent() == 0
                ? Component.translatable("screen.anvilcraft.trading_station.output_not_allow")
                : Component.translatable("screen.anvilcraft.trading_station.output_allow"),
                mouseX,
                mouseY
            );
        } else {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    protected void renderFilterSlotTooltip(GuiGraphics guiGraphics, Slot slot, Component tooltipEmpty, int mouseX, int mouseY) {
        if (!slot.hasItem()) {
            guiGraphics.renderTooltip(this.font, tooltipEmpty, mouseX, mouseY);
        } else {
            guiGraphics.renderTooltip(
                this.font,
                Lists.transform(this.getTooltipFromContainerItem(slot.getItem()), Component::getVisualOrderText),
                mouseX,
                mouseY
            );
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> components = super.getTooltipFromContainerItem(stack);
        if (this.hoveredSlot instanceof FilterOnlySlot slot && slot.hasItem()) {
            components.add(
                Component.translatable("screen.anvilcraft.filter.scroll_to_change")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)
            );
            components.add(
                Component.translatable("screen.anvilcraft.filter.shift_to_scroll_faster")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)
            );
        } else if (this.hoveredSlot instanceof SlotItemHandlerWithFilter filterSlot && filterSlot.isFilter()) {
            components.add(SCROLL_WHEEL_TO_CHANGE_STACK_LIMIT_TOOLTIP);
            components.add(SHIFT_TO_SCROLL_FASTER_TOOLTIP);
        }
        return components;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        if (slot instanceof SlotItemHandlerWithFilter filterSlot
            && filterSlot.isFilter()
            && !slot.hasItem()) {
            int storageSlot = slot.getContainerSlot();
            ItemStack carriedItem = this.menu.getCarried().copy();
            ItemStack previousFilter = this.menu.getFilter(storageSlot);
            if (!carriedItem.isEmpty()) {
                this.menu.setFilter(storageSlot, carriedItem);
                PacketDistributor.sendToServer(new SlotFilterChangePacket(storageSlot, carriedItem));
                if (carriedItem.is(ModItems.FILTER)
                    && (previousFilter.isEmpty() || !FilterItem.filter(previousFilter, carriedItem))) {
                    return;
                }
            } else if (Screen.hasShiftDown()
                       && button == InputConstants.MOUSE_BUTTON_LEFT
                       && !previousFilter.isEmpty()) {
                this.menu.setSlotDisabled(storageSlot, false);
                PacketDistributor.sendToServer(new SlotDisableChangePacket(storageSlot, false));
                return;
            }
        }
        if (slot instanceof FilterOnlySlot filterSlot) {
            ItemStack filterStack = this.menu.getCarried();
            if (filterStack.isEmpty() && !Screen.hasShiftDown()) return;
            if (!filterStack.isEmpty() && button == InputConstants.MOUSE_BUTTON_RIGHT) {
                filterStack = filterStack.copyWithCount(1);
            } else {
                filterStack = filterStack.copy();
            }
            filterSlot.set(filterStack);
            return;
        }
        super.slotClicked(slot, slotId, button, type);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Slot slot = this.hoveredSlot;
        if (slot instanceof SlotItemHandlerWithFilter filterSlot && filterSlot.isFilter() && scrollY != 0) {
            int storageSlot = slot.getContainerSlot();
            int limitBefore = this.menu.getSlotLimit(storageSlot);
            int limitAfter = limitBefore + this.getScrollSpeed() * (scrollY > 0 ? 1 : -1);
            limitAfter = Mth.clamp(limitAfter, 1, 64);
            if (limitAfter != limitBefore) {
                this.menu.setSlotLimit(storageSlot, limitAfter);
                PacketDistributor.sendToServer(new SlotFilterMaxStackSizeChangePacket(storageSlot, limitAfter));
            }
            return true;
        } else if (slot instanceof FilterOnlySlot filterSlot && scrollY != 0) {
            ItemStack item = filterSlot.getItem();
            int countBefore = item.getCount();
            int countAfter = countBefore + this.getScrollSpeed() * (scrollY > 0 ? 1 : -1);
            countAfter = Mth.clamp(countAfter, 1, item.getMaxStackSize());
            ItemStack newItem = item.copyWithCount(countAfter);
            filterSlot.set(newItem);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getScrollSpeed() {
        return Screen.hasShiftDown() ? 5 : 1;
    }

    @Override
    public TradingStationMenu getFilterMenu() {
        return this.menu;
    }

    @Override
    public Collection<Integer> getGhostSlots() {
        return IGhostIngredientScreen.range(36, 48, 1);
    }
}
