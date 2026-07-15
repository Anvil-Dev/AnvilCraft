package dev.dubhe.anvilcraft.client.gui.screen;

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
import dev.dubhe.anvilcraft.item.utility.FilterItem;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterMaxStackSizeChangePacket;
import dev.dubhe.anvilcraft.network.multiple.TradingStationPackets;
import net.minecraft.ChatFormatting;
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

import java.util.Collection;
import java.util.List;

public class TradingStationScreen extends AbstractContainerScreen<TradingStationMenu>
    implements IFilterScreen<TradingStationMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "trading_station");
    private static final Identifier PLAYER_NOT_ALLOW = SharedTextures.textureGui("machine/trading_station/player_not_allow");
    private static final Identifier PLAYER_ALLOW = SharedTextures.textureGui("machine/trading_station/player_allow");
    private static final Identifier VILLAGER_NOT_ALLOW = SharedTextures.textureGui("machine/trading_station/villager_not_allow");
    private static final Identifier VILLAGER_ALLOW = SharedTextures.textureGui("machine/trading_station/villager_allow");
    private static final Identifier INPUT_NOT_ALLOW = SharedTextures.textureGui("machine/trading_station/input_not_allow");
    private static final Identifier INPUT_ALLOW = SharedTextures.textureGui("machine/trading_station/input_allow");
    private static final Identifier OUTPUT_NOT_ALLOW = SharedTextures.textureGui("machine/trading_station/output_not_allow");
    private static final Identifier OUTPUT_ALLOW = SharedTextures.textureGui("machine/trading_station/output_allow");
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.imageWidth,
            this.imageHeight,
            256,
            256
        );
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(graphics, slot, mouseX, mouseY);
        IFilterScreen.super.extractSlot(graphics, slot);
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
                ClientPacketDistributor.sendToServer(new TradingStationPackets.SyncAllowing(
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
                ClientPacketDistributor.sendToServer(new TradingStationPackets.SyncAllowing(
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
                ClientPacketDistributor.sendToServer(new TradingStationPackets.SyncAllowing(
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
                ClientPacketDistributor.sendToServer(new TradingStationPackets.SyncAllowing(
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
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 8, this.topPos + 25, this.leftPos + 24, this.topPos + 41)) {
            this.renderFilterSlotTooltip(
                graphics,
                this.menu.getSlot(48),
                Component.translatable("screen.anvilcraft.trading_station.provide"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 26, this.topPos + 25, this.leftPos + 42, this.topPos + 41)) {
            this.renderFilterSlotTooltip(
                graphics,
                this.menu.getSlot(49),
                Component.translatable("screen.anvilcraft.trading_station.provide"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 67, this.topPos + 25, this.leftPos + 83, this.topPos + 41)) {
            this.renderFilterSlotTooltip(
                graphics,
                this.menu.getSlot(50),
                Component.translatable("screen.anvilcraft.trading_station.request"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 8, this.topPos + 50, this.leftPos + 24, this.topPos + 66)) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.playerAllowed.getCurrent() == 0
                ? Component.translatable("screen.anvilcraft.trading_station.player_not_allow")
                : Component.translatable("screen.anvilcraft.trading_station.player_allow"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 26, this.topPos + 50, this.leftPos + 43, this.topPos + 66)) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.villagerAllowed.getCurrent() == 0
                ? Component.translatable("screen.anvilcraft.trading_station.villager_not_allow")
                : Component.translatable("screen.anvilcraft.trading_station.villager_allow"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 51, this.topPos + 50, this.leftPos + 67, this.topPos + 66)) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.inputAllowed.getCurrent() == 0
                ? Component.translatable("screen.anvilcraft.trading_station.input_not_allow")
                : Component.translatable("screen.anvilcraft.trading_station.input_allow"),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 69, this.topPos + 50, this.leftPos + 85, this.topPos + 66)) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.outputAllowed.getCurrent() == 0
                ? Component.translatable("screen.anvilcraft.trading_station.output_not_allow")
                : Component.translatable("screen.anvilcraft.trading_station.output_allow"),
                mouseX,
                mouseY
            );
        } else {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
    }

    protected void renderFilterSlotTooltip(
        GuiGraphicsExtractor graphics,
        Slot slot,
        Component tooltipEmpty,
        int mouseX,
        int mouseY
    ) {
        if (!slot.hasItem()) {
            graphics.setTooltipForNextFrame(this.font, tooltipEmpty, mouseX, mouseY);
        } else {
            ItemStack stack = slot.getItem();
            graphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(stack),
                stack.getTooltipImage(),
                stack,
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
    protected void slotClicked(Slot slot, int slotId, int button, ContainerInput input) {
        if (slot instanceof SlotItemHandlerWithFilter filterSlot
            && filterSlot.isFilter()
            && !slot.hasItem()) {
            int storageSlot = slot.getContainerSlot();
            ItemStack carriedItem = this.menu.getCarried().copy();
            ItemStack previousFilter = this.menu.getFilter(storageSlot);
            if (!carriedItem.isEmpty()) {
                this.menu.setFilter(storageSlot, carriedItem);
                ClientPacketDistributor.sendToServer(new SlotFilterChangePacket(storageSlot, carriedItem));
                if (carriedItem.is(ModItems.FILTER)
                    && (previousFilter.isEmpty() || !FilterItem.filter(previousFilter, carriedItem))) {
                    return;
                }
            } else if (this.minecraft.hasShiftDown()
                       && button == InputConstants.MOUSE_BUTTON_LEFT
                       && !previousFilter.isEmpty()) {
                this.menu.setSlotDisabled(storageSlot, false);
                ClientPacketDistributor.sendToServer(new SlotDisableChangePacket(storageSlot, false));
                return;
            }
        }
        if (slot instanceof FilterOnlySlot filterSlot) {
            ItemStack filterStack = this.menu.getCarried();
            if (filterStack.isEmpty() && !this.minecraft.hasShiftDown()) return;
            if (!filterStack.isEmpty() && button == InputConstants.MOUSE_BUTTON_RIGHT) {
                filterStack = filterStack.copyWithCount(1);
            } else {
                filterStack = filterStack.copy();
            }
            filterSlot.set(filterStack);
            return;
        }
        super.slotClicked(slot, slotId, button, input);
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
                ClientPacketDistributor.sendToServer(new SlotFilterMaxStackSizeChangePacket(storageSlot, limitAfter));
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
        return this.minecraft.hasShiftDown() ? 5 : 1;
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
