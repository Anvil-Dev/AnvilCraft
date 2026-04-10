package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.BatchCutterMenu;
import dev.dubhe.anvilcraft.item.FilterItem;
import dev.dubhe.anvilcraft.network.BatchCutterSelectPacket;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterMaxStackSizeChangePacket;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.MathUtil;
import dev.dubhe.anvilcraft.util.Scrollable;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.BiFunction;

public class BatchCutterScreen extends BaseMachineScreen<BatchCutterMenu> implements IFilterScreen<BatchCutterMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "batch_cutter");
    private final BatchCutterMenu menu;
    private final BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier = this.getEnableFilterButtonSupplier(8, 20);
    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 2;
        }

        @Override
        public int column() {
            return 5;
        }

        @Override
        public int size() {
            List<RecipeHolder<StonecutterRecipe>> recipes = BatchCutterScreen.this.menu.getRecipes();
            return recipes == null ? 0 : recipes.size();
        }

        @Override
        public void setHead(int head) {
            BatchCutterScreen.this.head = head;
        }
    };
    private int head;
    private ItemStack renderingTooltip;
    @Getter
    private EnableFilterButton enableFilterButton = null;

    public BatchCutterScreen(BatchCutterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 152, 20);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        this.enableFilterButton = enableFilterButtonSupplier.apply(this.leftPos, this.topPos);
        this.addRenderableWidget(this.enableFilterButton);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.scrollable.calculateScroll(this.head / this.scrollable.column());

        this.init(minecraft, width, height);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.scrollable.canScroll()) {
            int left = this.leftPos + 132;
            int top = this.topPos + 23;
            int down = top + 36;
            graphics.blit(
                SharedTextures.SWITCH_TABLE_SLIDER,
                left,
                top + (int) ((down - top - 12) * this.scrollable.getScrollOffs()),
                0,
                0,
                4,
                12,
                8,
                12
            );
        }
    }

    @Override
    protected void renderBeforeTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderSelectingArea(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderSelectingArea(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltip = null;
        if (!this.scrollable.canScroll() && this.head != 0) this.head = 0;
        if (this.menu.getRecipes().isEmpty()) return;
        int maxSize = this.scrollable.row() * this.scrollable.column();
        for (int i = this.head; i < this.head + Math.min(this.menu.getRecipes().size() - this.head, maxSize); i++) {
            int x = this.leftPos + 39 + 18 * (i % this.scrollable.column());
            int y = this.topPos + 23 + 18 * ((i - this.head) / this.scrollable.column());

            RecipeHolder<StonecutterRecipe> recipe = ListUtil.safelyGet(this.menu.getRecipes(), i).orElse(null);
            if (recipe == null) continue;

            ItemStack willRender = recipe.value().getResultItem(this.menu.getLevel().registryAccess());

            int offsetV = 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                offsetV = 36;
                this.renderingTooltip = willRender;
            }

            boolean selected = false;
            if (this.menu.getEntity().getSelecting() == i) {
                offsetV = 18;
                selected = true;
            }

            graphics.blit(SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
            graphics.renderItem(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    @Override
    public void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        IFilterScreen.super.renderSlot(graphics, slot);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack itemstack = this.hoveredSlot.getItem();
            graphics.renderTooltip(this.font, this.getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, x, y);
            this.renderSlotTooltip(graphics, x, y);
        } else if (this.renderingTooltip != null) {
            graphics.renderTooltip(
                this.font,
                this.getTooltipFromContainerItem(this.renderingTooltip),
                this.renderingTooltip.getTooltipImage(),
                this.renderingTooltip,
                x,
                y
            );
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> components = super.getTooltipFromContainerItem(stack);
        if (
            this.hoveredSlot instanceof SlotItemHandlerWithFilter filterSlot
            && filterSlot.isFilter()
            && !filterSlot.getItem().isEmpty()
        ) {
            components.add(SCROLL_WHEEL_TO_CHANGE_STACK_LIMIT_TOOLTIP);
            components.add(SHIFT_TO_SCROLL_FASTER_TOOLTIP);
        }
        return components;
    }

    protected void renderSlotTooltip(GuiGraphics graphics, int x, int y) {
        if (this.hoveredSlot == null) return;
        if (!(this.hoveredSlot instanceof SlotItemHandlerWithFilter slot)) return;
        if (!slot.isFilter()) return;
        if (!this.isFilterEnabled()) return;
        if (!this.isSlotDisabled(this.hoveredSlot.getContainerSlot())) return;
        graphics.renderTooltip(this.font, Component.translatable("screen.anvilcraft.slot.disable.tooltip"), x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.insideScrollbar(mouseX, mouseY)) {
                this.scrollable.scrolling();
                return true;
            }
            int maxSize = this.scrollable.row() * this.scrollable.column();
            for (int i = this.head; i < this.head + Math.min(this.menu.getRecipes().size() - this.head, maxSize); i++) {
                int x = this.leftPos + 39 + 18 * (i % this.scrollable.column());
                int y = this.topPos + 23 + 18 * ((i - this.head) / this.scrollable.column());

                if (!MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) continue;
                if (this.menu.getEntity().getSelecting() == i) {
                    this.menu.getEntity().setSelecting(0);
                    this.menu.onChanged();
                    PacketDistributor.sendToServer(new BatchCutterSelectPacket(0, this.menu.getEntity().getPos()));
                } else {
                    this.menu.getEntity().setSelecting(i);
                    this.menu.onChanged();
                    PacketDistributor.sendToServer(new BatchCutterSelectPacket(i, this.menu.getEntity().getPos()));
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.scrollable.isScrolling()) {
            this.scrollable.notScrolling();
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrollable.isScrolling()) {
            int top = this.topPos + 23;
            this.scrollable.scrollOnDrag(12, mouseY, top, top + 36);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public BatchCutterMenu getFilterMenu() {
        return this.menu;
    }

    @Override
    public void flush() {
        this.enableFilterButton.flush();
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot instanceof SlotItemHandlerWithFilter && slot.getItem().isEmpty()) {
            ItemStack carriedItem = this.menu.getCarried().copy();
            int realSlotId = slot.getContainerSlot();
            if (!carriedItem.isEmpty() && this.menu.isFilterEnabled()) {
                if (this.menu.isSlotDisabled(realSlotId)) {
                    PacketDistributor.sendToServer(new SlotDisableChangePacket(realSlotId, false));
                    this.menu.setSlotDisabled(realSlotId, false);
                }
                ItemStack filter = this.menu.getFilter(realSlotId);
                PacketDistributor.sendToServer(new SlotFilterChangePacket(realSlotId, carriedItem));
                this.menu.setFilter(realSlotId, carriedItem);
                if (carriedItem.is(ModItems.FILTER) && (filter.isEmpty() || !FilterItem.filter(filter, carriedItem))) return;
                slot.set(carriedItem);
            } else if (Screen.hasShiftDown()) {
                PacketDistributor.sendToServer(new SlotDisableChangePacket(
                    realSlotId,
                    carriedItem.isEmpty() && !this.menu.isSlotDisabled(realSlotId)
                ));
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public int getOffsetX() {
        return this.leftPos = (this.width - this.imageWidth) / 2;
    }

    @Override
    public int getOffsetY() {
        return this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Slot slot = this.hoveredSlot;
        if (slot instanceof SlotItemHandlerWithFilter filterSlot && filterSlot.isFilter() && scrollY != 0) {
            int slotIndex = slot.getContainerSlot();
            int currentLimit = this.getSlotLimit(slotIndex);
            int scrollSpeed = Screen.hasShiftDown() ? 5 : 1;
            int newLimit = currentLimit + (scrollY > 0 ? scrollSpeed : -scrollSpeed);
            newLimit = Mth.clamp(newLimit, 1, 64);

            if (newLimit != currentLimit) {
                this.setSlotLimit(slotIndex, newLimit);
                PacketDistributor.sendToServer(new SlotFilterMaxStackSizeChangePacket(slotIndex, newLimit));
                return true;
            }
        }
        if (this.scrollable.canScroll()) {
            this.scrollable.scrollOnScroll(scrollY / 1.2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int left = this.leftPos + 132;
        int top = this.topPos + 23;
        int right = left + 4;
        int down = top + 36;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }
}
