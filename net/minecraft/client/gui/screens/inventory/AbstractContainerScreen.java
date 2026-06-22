package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    public static final Identifier INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    protected static final int BACKGROUND_TEXTURE_WIDTH = 256;
    protected static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    private static final float SNAPBACK_SPEED = 100.0F;
    private static final int QUICKDROP_DELAY = 500;
    protected static final int DEFAULT_IMAGE_WIDTH = 176;
    protected static final int DEFAULT_IMAGE_HEIGHT = 166;
    protected final int imageWidth;
    protected final int imageHeight;
    protected int titleLabelX;
    protected int titleLabelY;
    protected int inventoryLabelX;
    protected int inventoryLabelY;
    private final List<ItemSlotMouseAction> itemSlotMouseActions;
    protected final T menu;
    protected final Component playerInventoryTitle;
    protected @Nullable Slot hoveredSlot;
    private @Nullable Slot clickedSlot;
    private @Nullable Slot quickdropSlot;
    private @Nullable Slot lastClickSlot;
    private AbstractContainerScreen.@Nullable SnapbackData snapbackData;
    protected int leftPos;
    protected int topPos;
    private boolean isSplittingStack;
    private ItemStack draggingItem = ItemStack.EMPTY;
    private long quickdropTime;
    protected final Set<Slot> quickCraftSlots = Sets.newHashSet();
    protected boolean isQuickCrafting;
    private int quickCraftingType;
    @MouseButtonInfo.MouseButton
    private int quickCraftingButton;
    private boolean skipNextRelease;
    private int quickCraftingRemainder;
    private boolean doubleclick;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;

    public AbstractContainerScreen(T menu, Inventory inventory, Component title) {
        this(menu, inventory, title, 176, 166);
    }

    public AbstractContainerScreen(T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(title);
        this.menu = menu;
        this.playerInventoryTitle = inventory.getDisplayName();
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.skipNextRelease = true;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = imageHeight - 94;
        this.itemSlotMouseActions = new ArrayList<>();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.itemSlotMouseActions.clear();
        this.addItemSlotMouseAction(new BundleMouseActions(this.minecraft));
    }

    protected void addItemSlotMouseAction(ItemSlotMouseAction itemSlotMouseAction) {
        this.itemSlotMouseActions.add(itemSlotMouseAction);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractContents(graphics, mouseX, mouseY, a);
        this.extractCarriedItem(graphics, mouseX, mouseY);
        this.extractSnapbackItem(graphics);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int xo = this.leftPos;
        int yo = this.topPos;
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.pose().pushMatrix();
        graphics.pose().translate(xo, yo);
        this.extractLabels(graphics, mouseX, mouseY);
        Slot previouslyHoveredSlot = this.hoveredSlot;
        this.hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
        this.extractSlotHighlightBack(graphics);
        this.extractSlots(graphics, mouseX, mouseY);
        this.extractSlotHighlightFront(graphics);
        if (previouslyHoveredSlot != null && previouslyHoveredSlot != this.hoveredSlot) {
            this.onStopHovering(previouslyHoveredSlot);
        }

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ContainerScreenEvent.Render.Foreground(this, graphics, mouseX, mouseY));
        graphics.pose().popMatrix();
    }

    public void extractCarriedItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack carried = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if (!carried.isEmpty()) {
            int xOffset = 8;
            int yOffset = this.draggingItem.isEmpty() ? 8 : 16;
            String itemCount = null;
            if (!this.draggingItem.isEmpty() && this.isSplittingStack) {
                carried = carried.copyWithCount(Mth.ceil(carried.getCount() / 2.0F));
            } else if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
                carried = carried.copyWithCount(this.quickCraftingRemainder);
                if (carried.isEmpty()) {
                    itemCount = ChatFormatting.YELLOW + "0";
                }
            }

            graphics.nextStratum();
            this.extractFloatingItem(graphics, carried, mouseX - 8, mouseY - yOffset, itemCount);
        }
    }

    public void extractSnapbackItem(GuiGraphicsExtractor graphics) {
        if (this.snapbackData != null) {
            float snapbackProgress = Mth.clamp((float)(Util.getMillis() - this.snapbackData.time) / 100.0F, 0.0F, 1.0F);
            int xd = this.snapbackData.end.x - this.snapbackData.start.x;
            int yd = this.snapbackData.end.y - this.snapbackData.start.y;
            int x = this.snapbackData.start.x + (int)(xd * snapbackProgress);
            int y = this.snapbackData.start.y + (int)(yd * snapbackProgress);
            graphics.nextStratum();
            this.extractFloatingItem(graphics, this.snapbackData.item, x, y, null);
            if (snapbackProgress >= 1.0F) {
                this.snapbackData = null;
            }
        }
    }

    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                this.extractSlot(graphics, slot, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(this.hoveredSlot)
                    && itemMouseAction.onMouseScrolled(scrollX, scrollY, this.hoveredSlot.index, this.hoveredSlot.getItem())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void extractSlotHighlightBack(GuiGraphicsExtractor graphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    private void extractSlotHighlightFront(GuiGraphicsExtractor graphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack item = this.hoveredSlot.getItem();
            if (this.menu.getCarried().isEmpty() || this.showTooltipWithItemInHand(item)) {
                graphics.setTooltipForNextFrame(
                    this.font, this.getTooltipFromContainerItem(item), item.getTooltipImage(), item, mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE)
                );
            }
        }
    }

    private boolean showTooltipWithItemInHand(ItemStack item) {
        return item.getTooltipImage().map(ClientTooltipComponent::create).map(ClientTooltipComponent::showTooltipWithItemInHand).orElse(false);
    }

    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        return getTooltipFromItem(this.minecraft, itemStack);
    }

    private void extractFloatingItem(GuiGraphicsExtractor graphics, ItemStack carried, int x, int y, @Nullable String itemCount) {
        graphics.item(carried, x, y);
        var font = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(carried).getFont(carried, net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.FontContext.ITEM_COUNT);
        graphics.itemDecorations(font != null ? font : this.font, carried, x, y - (this.draggingItem.isEmpty() ? 0 : 8), itemCount);
    }

    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        int x = slot.x;
        int y = slot.y;
        ItemStack itemStack = slot.getItem();
        boolean quickCraftStack = false;
        boolean done = slot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack carried = this.menu.getCarried();
        String itemCount = null;
        if (slot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemStack.isEmpty()) {
            itemStack = itemStack.copyWithCount(itemStack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !carried.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }

            if (AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && this.menu.canDragTo(slot)) {
                quickCraftStack = true;
                int maxSize = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                int carry = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int newCount = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots.size(), this.quickCraftingType, carried) + carry;
                if (newCount > maxSize) {
                    newCount = maxSize;
                    itemCount = ChatFormatting.YELLOW.toString() + maxSize;
                }

                itemStack = carried.copyWithCount(newCount);
            } else {
                this.quickCraftSlots.remove(slot);
                this.recalculateQuickCraftRemaining();
            }
        }

        if (itemStack.isEmpty() && slot.isActive()) {
            Identifier icon = slot.getNoItemIcon();
            if (icon != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, 16, 16);
                done = true;
            }
        }

        if (!done) {
            if (quickCraftStack) {
                graphics.fill(x, y, x + 16, y + 16, -2130706433);
            }

            renderSlotContents(graphics, itemStack, slot, itemCount);
        }
    }

    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot, @Nullable String itemCount) {
        {
            int x = slot.x; int y = slot.y;
            int seed = slot.x + slot.y * this.imageWidth;
            if (slot.isFake()) {
                graphics.fakeItem(itemStack, x, y, seed);
            } else {
                graphics.item(itemStack, x, y, seed);
            }

            var font = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemStack).getFont(itemStack, net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.FontContext.ITEM_COUNT);
            graphics.itemDecorations(font != null ? font : this.font, itemStack, x, y, itemCount);
        }
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack carried = this.menu.getCarried();
        if (!carried.isEmpty() && this.isQuickCrafting) {
            if (this.quickCraftingType == 2) {
                this.quickCraftingRemainder = carried.getMaxStackSize();
            } else {
                this.quickCraftingRemainder = carried.getCount();

                for (Slot slot : this.quickCraftSlots) {
                    ItemStack slotItemStack = slot.getItem();
                    int carry = slotItemStack.isEmpty() ? 0 : slotItemStack.getCount();
                    int maxSize = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                    int newCount = Math.min(
                        AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots.size(), this.quickCraftingType, carried) + carry, maxSize
                    );
                    this.quickCraftingRemainder -= newCount - carry;
                }
            }
        }
    }

    private @Nullable Slot getHoveredSlot(double x, double y) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot, x, y)) {
                return slot;
            }
        }

        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        } else {
            var mouseKey = com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(event.button());
            boolean cloning = this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey) && this.minecraft.player.hasInfiniteMaterials();
            Slot slot = this.getHoveredSlot(event.x(), event.y());
            this.doubleclick = this.lastClickSlot == slot && doubleClick;
            this.skipNextRelease = false;
            if (event.button() != 0 && event.button() != 1 && !cloning) {
                this.checkHotbarMouseClicked(event);
            } else {
                int xo = this.leftPos;
                int yo = this.topPos;
                boolean clickedOutside = this.hasClickedOutside(event.x(), event.y(), xo, yo);
                if (slot != null) clickedOutside = false; // Forge, prevent dropping of items through slots outside of GUI boundaries
                int slotId = -1;
                if (slot != null) {
                    slotId = slot.index;
                }

                if (clickedOutside) {
                    slotId = -999;
                }

                if (this.minecraft.options.touchscreen().get() && clickedOutside && this.menu.getCarried().isEmpty()) {
                    this.onClose();
                    return true;
                }

                if (slotId != -1) {
                    if (this.minecraft.options.touchscreen().get()) {
                        if (slot != null && slot.hasItem()) {
                            this.clickedSlot = slot;
                            this.draggingItem = ItemStack.EMPTY;
                            this.isSplittingStack = event.button() == 1;
                        } else {
                            this.clickedSlot = null;
                        }
                    } else if (!this.isQuickCrafting) {
                        if (this.menu.getCarried().isEmpty()) {
                            if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                                this.slotClicked(slot, slotId, event.button(), ContainerInput.CLONE);
                            } else {
                                boolean quickKey = slotId != -999 && event.hasShiftDown();
                                ContainerInput containerInput = ContainerInput.PICKUP;
                                if (quickKey) {
                                    this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                                    containerInput = ContainerInput.QUICK_MOVE;
                                } else if (slotId == -999) {
                                    containerInput = ContainerInput.THROW;
                                }

                                this.slotClicked(slot, slotId, event.button(), containerInput);
                            }

                            this.skipNextRelease = true;
                        } else {
                            this.isQuickCrafting = true;
                            this.quickCraftingButton = event.button();
                            this.quickCraftSlots.clear();
                            if (event.button() == 0) {
                                this.quickCraftingType = 0;
                            } else if (event.button() == 1) {
                                this.quickCraftingType = 1;
                            } else if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                                this.quickCraftingType = 2;
                            }
                        }
                    }
                }
            }

            this.lastClickSlot = slot;
            return true;
        }
    }

    private void checkHotbarMouseClicked(MouseButtonEvent event) {
        if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
            if (this.minecraft.options.keySwapOffhand.matchesMouse(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
                return;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(event)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
                }
            }
        }
    }

    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return mx < xo || my < yo || mx >= xo + this.imageWidth || my >= yo + this.imageHeight;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        ItemStack carried = this.menu.getCarried();
        if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
            if (event.button() == 0 || event.button() == 1) {
                if (this.draggingItem.isEmpty()) {
                    if (slot != this.clickedSlot && !this.clickedSlot.getItem().isEmpty()) {
                        this.draggingItem = this.clickedSlot.getItem().copy();
                    }
                } else if (this.draggingItem.getCount() > 1 && slot != null && AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false)) {
                    long time = Util.getMillis();
                    if (this.quickdropSlot == slot) {
                        if (time - this.quickdropTime > 500L) {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ContainerInput.PICKUP);
                            this.slotClicked(slot, slot.index, 1, ContainerInput.PICKUP);
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ContainerInput.PICKUP);
                            this.quickdropTime = time + 750L;
                            this.draggingItem.shrink(1);
                        }
                    } else {
                        this.quickdropSlot = slot;
                        this.quickdropTime = time;
                    }
                }
            }

            return true;
        } else if (slot != null && this.shouldAddSlotToQuickCraft(slot, carried) && this.quickCraftSlots.add(slot)) {
            this.recalculateQuickCraftRemaining();
            return true;
        } else {
            return slot == null && this.menu.getCarried().isEmpty() ? super.mouseDragged(event, dx, dy) : true;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        super.mouseReleased(event); //Forge: call parent to release buttons
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        int xo = this.leftPos;
        int yo = this.topPos;
        boolean clickedOutside = this.hasClickedOutside(event.x(), event.y(), xo, yo);
        if (slot != null) clickedOutside = false; // Forge, prevent dropping of items through slots outside of GUI boundaries
        var mouseKey = com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(event.button());
        int slotId = -1;
        if (slot != null) {
            slotId = slot.index;
        }

        if (clickedOutside) {
            slotId = -999;
        }

        if (this.doubleclick && slot != null && event.button() == 0 && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (event.hasShiftDown()) {
                if (!this.lastQuickMoved.isEmpty()) {
                    for (Slot target : this.menu.slots) {
                        if (target != null
                            && target.mayPickup(this.minecraft.player)
                            && target.hasItem()
                            && target.isSameInventory(slot)
                            && AbstractContainerMenu.canItemQuickReplace(target, this.lastQuickMoved, true)) {
                            this.slotClicked(target, target.index, event.button(), ContainerInput.QUICK_MOVE);
                        }
                    }
                }
            } else {
                this.slotClicked(slot, slotId, event.button(), ContainerInput.PICKUP_ALL);
            }

            this.doubleclick = false;
        } else {
            if (this.isQuickCrafting && this.quickCraftingButton != event.button()) {
                this.isQuickCrafting = false;
                this.quickCraftSlots.clear();
                this.skipNextRelease = true;
                return true;
            }

            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            }

            if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
                if (event.button() == 0 || event.button() == 1) {
                    if (this.draggingItem.isEmpty() && slot != this.clickedSlot) {
                        this.draggingItem = this.clickedSlot.getItem();
                    }

                    boolean canReplace = AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false);
                    if (slotId != -1 && !this.draggingItem.isEmpty() && canReplace) {
                        this.slotClicked(this.clickedSlot, this.clickedSlot.index, event.button(), ContainerInput.PICKUP);
                        this.slotClicked(slot, slotId, 0, ContainerInput.PICKUP);
                        if (this.menu.getCarried().isEmpty()) {
                            this.snapbackData = null;
                        } else {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, event.button(), ContainerInput.PICKUP);
                            this.snapbackData = new AbstractContainerScreen.SnapbackData(
                                this.draggingItem,
                                new Vector2i((int)event.x(), (int)event.y()),
                                new Vector2i(this.clickedSlot.x + xo, this.clickedSlot.y + yo),
                                Util.getMillis()
                            );
                        }
                    } else if (!this.draggingItem.isEmpty()) {
                        this.snapbackData = new AbstractContainerScreen.SnapbackData(
                            this.draggingItem,
                            new Vector2i((int)event.x(), (int)event.y()),
                            new Vector2i(this.clickedSlot.x + xo, this.clickedSlot.y + yo),
                            Util.getMillis()
                        );
                    }

                    this.clearDraggingState();
                }
            } else if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
                this.quickCraftToSlots();
            } else if (!this.menu.getCarried().isEmpty()) {
                if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                    this.slotClicked(slot, slotId, event.button(), ContainerInput.CLONE);
                } else {
                    boolean quickKey = slotId != -999 && event.hasShiftDown();
                    if (quickKey) {
                        this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    }

                    this.slotClicked(slot, slotId, event.button(), quickKey ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
                }
            }
        }

        this.isQuickCrafting = false;
        return super.mouseReleased(event);
    }

    public void clearDraggingState() {
        this.draggingItem = ItemStack.EMPTY;
        this.clickedSlot = null;
    }

    private boolean isHovering(Slot slot, double xm, double ym) {
        return this.isHovering(slot.x, slot.y, 16, 16, xm, ym);
    }

    protected boolean isHovering(int left, int top, int w, int h, double xm, double ym) {
        int xo = this.leftPos;
        int yo = this.topPos;
        xm -= xo;
        ym -= yo;
        return xm >= left - 1 && xm < left + w + 1 && ym >= top - 1 && ym < top + h + 1;
    }

    private void onStopHovering(Slot slot) {
        if (slot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(slot)) {
                    itemMouseAction.onStopHovering(slot);
                }
            }
        }
    }

    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (slot != null) {
            slotId = slot.index;
        }

        this.onMouseClickAction(slot, containerInput);
        this.minecraft.gameMode.handleContainerInput(this.menu.containerId, slotId, buttonNum, containerInput, this.minecraft.player);
    }

    void onMouseClickAction(@Nullable Slot slot, ContainerInput containerInput) {
        if (slot != null && slot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(slot)) {
                    itemMouseAction.onSlotClicked(slot, containerInput);
                }
            }
        }
    }

    protected void handleSlotStateChanged(int slotId, int containerId, boolean newState) {
        this.minecraft.gameMode.handleSlotStateChanged(slotId, containerId, newState);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        var key = com.mojang.blaze3d.platform.InputConstants.getKey(event);
        if (super.keyPressed(event)) {
            return true;
        } else if (this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            this.onClose();
            return true;
        } else {
            boolean handled = this.checkHotbarKeyPressed(event); // Forge MC-146650: Needs to return true when the key is handled
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                if (this.minecraft.options.keyPickItem.isActiveAndMatches(key)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.CLONE);
                    handled = true;
                } else if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, event.hasControlDown() ? 1 : 0, ContainerInput.THROW);
                    handled = true;
                }
            } else if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
                handled = true; // Forge MC-146650: Emulate MC bug, so we don't drop from hotbar when pressing drop without hovering over a item.
            }

            return handled;
        }
    }

    protected boolean checkHotbarKeyPressed(KeyEvent event) {
        var key = com.mojang.blaze3d.platform.InputConstants.getKey(event);
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(key)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
                return true;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].isActiveAndMatches(key)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void removed() {
        if (this.minecraft.player != null) {
            this.menu.removed(this.minecraft.player);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public final void tick() {
        super.tick();
        if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
            this.containerTick();
        } else {
            this.minecraft.player.closeContainer();
        }
    }

    protected void containerTick() {
    }

    private boolean shouldAddSlotToQuickCraft(Slot slot, ItemStack carried) {
        return this.isQuickCrafting
            && !carried.isEmpty()
            && (carried.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2)
            && AbstractContainerMenu.canItemQuickReplace(slot, carried, true)
            && slot.mayPlace(carried)
            && this.menu.canDragTo(slot);
    }

    private void quickCraftToSlots() {
        this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ContainerInput.QUICK_CRAFT);

        for (Slot quickSlot : this.quickCraftSlots) {
            this.slotClicked(quickSlot, quickSlot.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
        }

        this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
    }

    @Override
    public T getMenu() {
        return this.menu;
    }

    // Neo: Add getters for non-public fields
    @org.jspecify.annotations.Nullable
    public Slot getHoveredSlot() {
        return this.hoveredSlot;
    }

    public int getLeftPos() {
        return leftPos;
    }

    public int getTopPos() {
        return topPos;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    /// @deprecated Neo: Use [#getHoveredSlot()] instead.
    @Deprecated(forRemoval = true, since = "26.1.2")
    @org.jspecify.annotations.Nullable
    public Slot getSlotUnderMouse() {
        return this.getHoveredSlot();
    }

    /// @deprecated Neo: Use [#getLeftPos()] instead.
    @Deprecated(forRemoval = true, since = "26.1.2")
    public int getGuiLeft() {
        return this.getLeftPos();
    }

    /// @deprecated Neo: Use [#getTopPos()] instead.
    @Deprecated(forRemoval = true, since = "26.1.2")
    public int getGuiTop() {
        return this.getTopPos();
    }

    /// @deprecated Neo: Use [#getImageHeight()] instead.
    @Deprecated(forRemoval = true, since = "26.1.2")
    public int getXSize() {
        return this.getImageWidth();
    }

    /// @deprecated Neo: Use [#getImageHeight()] instead.
    @Deprecated(forRemoval = true, since = "26.1.2")
    public int getYSize() {
        return this.getImageHeight();
    }

    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        if (this.hoveredSlot != null) {
            this.onStopHovering(this.hoveredSlot);
        }

        super.onClose();
    }

    @OnlyIn(Dist.CLIENT)
    private record SnapbackData(ItemStack item, Vector2i start, Vector2i end, long time) {
    }
}
