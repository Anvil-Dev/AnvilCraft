package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.rpc.StorageInput;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ItemDecoratorHandler;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class StorageScreen extends Screen {
    private static final Identifier BACKGROUND = SharedTextures.bg("misc", "storage_station");
    private static final Identifier CAPACITY = SharedTextures.textureGui("misc/storage_station/capacity");
    private static final Identifier SEARCH_CLEAR = SharedTextures.textureGui("misc/storage_station/search_clear");
    private static final Identifier PUT = SharedTextures.textureGui("misc/storage_station/put");
    private static final Identifier TAKE = SharedTextures.textureGui("misc/storage_station/take");
    private static final Identifier SEARCH_RETENTION = SharedTextures.textureGui("misc/storage_station/search_retention");
    private static final Identifier SORT_COUNT = SharedTextures.textureGui("misc/storage_station/sort_by_number");
    private static final Identifier SORT_MOD = SharedTextures.textureGui("misc/storage_station/sort_by_mod");
    private static final Identifier SORT_NAME = SharedTextures.textureGui("misc/storage_station/sort_by_name");
    private static final Identifier SORT_COUNT_REVERSED = SharedTextures.textureGui("misc/storage_station/sort_by_number_reverse");
    private static final Identifier SORT_NAME_REVERSED = SharedTextures.textureGui("misc/storage_station/sort_by_name_reverse");
    private static final Identifier ORDER_SEQUENTIAL = SharedTextures.textureGui("misc/storage_station/sequential_order");
    private static final Identifier ORDER_REVERSE = SharedTextures.textureGui("misc/storage_station/reverse_order");
    private static final Identifier NBT_UNFOLD = SharedTextures.textureGui("misc/storage_station/nbt_unfold");
    private static final Identifier NBT_FOLD = SharedTextures.textureGui("misc/storage_station/nbt_fold");
    private static final Identifier SLIDER = SharedTextures.textureGui("misc/storage_station/slider_big");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static final Identifier SMALL_FONT = Identifier.fromNamespaceAndPath("anvilcraft", "small");
    private static final int BG_WIDTH = 300;
    private static final int BG_HEIGHT = 222;
    private static final int STORAGE_COLUMNS = 9;
    private static final int STORAGE_ROWS = 6;
    private static final int VISIBLE_STORAGE_SLOTS = STORAGE_COLUMNS * STORAGE_ROWS;
    private static final int STORAGE_X = 114;
    private static final int STORAGE_Y = 18;
    private static final int SLOT_SIZE = 18;
    private static final int SLIDER_X = 280;
    private static final int SLIDER_Y = 18;
    private static final int SLIDER_WIDTH = 12;
    private static final int SLIDER_HEIGHT = 15;
    private static final int SLIDER_TRACK_HEIGHT = 106;
    private static final int METADATA_REFRESH_INTERVAL = 10;
    private final BlockPos sourcePos;
    private final Player player;

    private @Nullable EditBox search;
    private @Nullable CategoryList categories;

    private ItemStack carried = ItemStack.EMPTY;
    private IntList order = new IntArrayList();
    private final Int2ObjectMap<UnlimitedItemStack> contents = new Int2ObjectOpenHashMap<>();
    private double fullness;
    private StorageServerStub.@Nullable Capacity capacity;
    private long version = -1;
    private long orderVersion = -1;
    private int scrollRow;
    private int reorderRequest;
    private int syncRequest;
    private int interactionRequest;
    private int metadataCooldown;
    private boolean orderLoaded;
    private boolean metadataPending;
    private boolean interactionPending;
    private final IntSet quickCraftSlots = new IntOpenHashSet();
    private boolean quickCrafting;
    private int quickCraftingButton;
    private int lastClickedInventorySlot = -1;
    private int pickupAllSlot = -1;
    private int left;
    private int top;
    private int titleLabelX;

    public StorageScreen(BlockPos sourcePos) {
        super(Objects.requireNonNull(Minecraft.getInstance().level).getBlockState(sourcePos).getBlock().getName());
        this.sourcePos = sourcePos;
        this.player = Objects.requireNonNull(Minecraft.getInstance().player);
    }

    public static void openScreen(BlockPos sourcePos) {
        Minecraft.getInstance().setScreenAndShow(new StorageScreen(sourcePos));
    }

    @Override
    protected void init() {
        this.left = (this.width - StorageScreen.BG_WIDTH) / 2;
        this.top = (this.height - StorageScreen.BG_HEIGHT) / 2;
        this.titleLabelX = (StorageScreen.BG_WIDTH - 106 - this.font.width(this.title)) / 2 + 106;

        this.search = this.addRenderableWidget(new EditBox(
            this.font,
            this.left + 6,
            this.top + 7,
            94,
            9,
            Component.translatable("screen.anvilcraft.storage.search.edit")
        ));
        this.search.setValue(SettingClientStub.setting().storage().getSearchContent());
        this.search.setBordered(false);
        this.search.setResponder(content -> {
            SettingClientStub.update(content);
            this.reorder(false);
        });
        final SwitchableButton searchMode = this.addRenderableWidget(new SwitchableButton(
            this.left + 2,
            this.top + 23,
            24,
            20,
            ImmutableList.of(
                StorageScreen.SEARCH_CLEAR,
                StorageScreen.SEARCH_RETENTION
            ),
            20,
            24,
            40,
            (_, index) -> {
                SettingClientStub.update(SearchMode.values()[index]);
                this.reorder();
            }
        ));
        List<Identifier> sortTextures = Lists.newArrayList(
            StorageScreen.SORT_COUNT,
            StorageScreen.SORT_MOD,
            StorageScreen.SORT_NAME
        );
        final SwitchableButton sortMode = this.addRenderableWidget(new SwitchableButton(
            this.left + 28,
            this.top + 23,
            24,
            20,
            sortTextures,
            20,
            24,
            40,
            (_, index) -> {
                SettingClientStub.update(SortMode.values()[index]);
                this.reorder();
            }
        ));
        final SwitchableButton orderMode = this.addRenderableWidget(new SwitchableButton(
            this.left + 54,
            this.top + 23,
            24,
            20,
            ImmutableList.of(
                StorageScreen.ORDER_SEQUENTIAL,
                StorageScreen.ORDER_REVERSE
            ),
            20,
            24,
            40,
            (_, index) -> {
                OrderMode mode = OrderMode.values()[index];
                SettingClientStub.update(mode);
                if (mode == OrderMode.SEQUENTIAL) {
                    sortTextures.set(0, StorageScreen.SORT_COUNT);
                    sortTextures.set(2, StorageScreen.SORT_NAME);
                } else {
                    sortTextures.set(0, StorageScreen.SORT_COUNT_REVERSED);
                    sortTextures.set(2, StorageScreen.SORT_NAME_REVERSED);
                }
                this.reorder();
            }
        ));
        final SwitchableButton nbtMode = this.addRenderableWidget(new SwitchableButton(
            this.left + 80,
            this.top + 23,
            24,
            20,
            ImmutableList.of(
                StorageScreen.NBT_UNFOLD,
                StorageScreen.NBT_FOLD
            ),
            20,
            24,
            40,
            (_, index) -> SettingClientStub.update(NbtDisplayMode.values()[index])
        ));
        this.categories = this.addRenderableWidget(new CategoryList(
            this.left + 7,
            this.top + 49,
            SettingClientStub.setting(),
            _ -> SettingClientStub.update(SettingClientStub.listed().stream().toList())
                .thenRunAsync(this::reorder, this.screenExecutor),
            _ -> this.minecraft.setScreenAndShow(new CategorySettingsScreen(this.sourcePos))
        ));
        this.addRenderableWidget(new TexturedButton(
            this.left + 278,
            this.top + 139,
            18,
            20,
            StorageScreen.PUT,
            20,
            18,
            40,
            _ -> StorageClientStub.deposit(StorageScreen.this.sourcePos, this.minecraft.hasShiftDown()).thenAcceptAsync(
                result -> {
                    if (result.changed()) {
                        StorageScreen.this.reorder(false);
                    }
                },
                StorageScreen.this.screenExecutor
            )
        ));
        this.addRenderableWidget(new TexturedButton(
            this.left + 278,
            this.top + 161,
            18,
            20,
            StorageScreen.TAKE,
            20,
            18,
            40,
            _ -> StorageClientStub.take(StorageScreen.this.sourcePos).thenAcceptAsync(
                result -> {
                    if (result.changed()) {
                        StorageScreen.this.reorder(false);
                    }
                },
                StorageScreen.this.screenExecutor
            )
        ));

        SettingClientStub.load().thenAcceptAsync(
            setting -> {
                if (this.categories != null) {
                    this.categories.rebuild(setting);
                }
                StorageSetting storage = setting.storage();
                this.search.setValue(storage.getSearchContent());
                searchMode.setCurrent(storage.getSearch().ordinal());
                sortMode.setCurrent(storage.getSort().ordinal());
                orderMode.setCurrent(storage.getOrder().ordinal());
                nbtMode.setCurrent(storage.getNbtDisplay().ordinal());
                if (storage.getOrder() == OrderMode.SEQUENTIAL) {
                    sortTextures.set(0, StorageScreen.SORT_COUNT);
                    sortTextures.set(2, StorageScreen.SORT_NAME);
                } else {
                    sortTextures.set(0, StorageScreen.SORT_COUNT_REVERSED);
                    sortTextures.set(2, StorageScreen.SORT_NAME_REVERSED);
                }
                this.reorder();
            },
            this.screenExecutor
        );
        this.refreshMetadata();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.metadataCooldown > 0) {
            this.metadataCooldown--;
        } else {
            this.refreshMetadata();
        }
    }

    // region Extract(Render)
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            StorageScreen.BACKGROUND,
            this.left,
            this.top,
            0,
            0,
            StorageScreen.BG_WIDTH,
            StorageScreen.BG_HEIGHT,
            512,
            256
        );

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            StorageScreen.CAPACITY,
            this.left + 106,
            this.top,
            0,
            0,
            Mth.ceil(194 * this.fullness),
            13,
            194,
            13
        );
        graphics.text(
            this.font,
            this.title,
            this.left + this.titleLabelX,
            this.top + Constant.SCREEN_TITLE_Y,
            0xFF404040,
            false
        );
        this.extractStorageContents(graphics, mouseX, mouseY);
        this.extractPlayerInventory(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractCarriedItem(graphics, mouseX, mouseY);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    private void extractStorageContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        for (int displayIndex = 0; displayIndex < StorageScreen.VISIBLE_STORAGE_SLOTS; displayIndex++) {
            int orderIndex = firstOrderIndex + displayIndex;
            if (orderIndex >= this.order.size()) {
                break;
            }

            int x = this.left + StorageScreen.STORAGE_X
                + displayIndex % StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            int y = this.top + StorageScreen.STORAGE_Y
                + displayIndex / StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            boolean hovered = MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17);
            if (hovered) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, x - 4, y - 4, 24, 24);
            }

            UnlimitedItemStack stack = this.contents.getOrDefault(
                this.order.getInt(orderIndex),
                UnlimitedItemStack.EMPTY
            );
            if (!stack.isEmpty()) {
                ItemStack itemStack = stack.toStack();
                graphics.item(itemStack, x, y);
                StorageScreen.itemDecorations(graphics, this.minecraft, itemStack, x, y);
                if (hovered && this.carried.isEmpty()) {
                    graphics.setTooltipForNextFrame(this.font, itemStack, mouseX, mouseY);
                }
            }

            if (hovered) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, x - 4, y - 4, 24, 24);
            }
        }
        this.extractStorageSlider(graphics);
    }

    private void extractStorageSlider(GuiGraphicsExtractor graphics) {
        int maxScrollRow = this.getMaxScrollRow();
        int sliderOffset = maxScrollRow == 0
            ? 0
            : Math.round(
                (StorageScreen.SLIDER_TRACK_HEIGHT - StorageScreen.SLIDER_HEIGHT)
                    * (float) this.scrollRow / maxScrollRow
            );
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            StorageScreen.SLIDER,
            this.left + StorageScreen.SLIDER_X,
            this.top + StorageScreen.SLIDER_Y + sliderOffset,
            0,
            0,
            StorageScreen.SLIDER_WIDTH,
            StorageScreen.SLIDER_HEIGHT,
            StorageScreen.SLIDER_WIDTH,
            StorageScreen.SLIDER_HEIGHT
        );
    }

    private void extractPlayerInventory(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Inventory inv = this.player.getInventory();

        int y = this.top + 140 + 58;
        for (int column = 0; column < 9; column++) {
            int x = this.left + 114 + 18 * column;
            this.extractInventorySlot(graphics, inv, column, x, y, mouseX, mouseY);
        }

        for (int row = 0; row < 3; row++) {
            y = this.top + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.left + 114 + 18 * column;
                this.extractInventorySlot(graphics, inv, slot++, x, y, mouseX, mouseY);
            }
        }
    }

    private void extractInventorySlot(GuiGraphicsExtractor graphics, Inventory inv, int slot, int x, int y, int mouseX, int mouseY) {
        boolean hovered = MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17);
        if (hovered) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, x - 4, y - 4, 24, 24);
        }

        ItemStack stack = inv.getItem(slot);
        boolean quickCraftPreview = this.quickCrafting && this.quickCraftSlots.contains(this.getScreenSlot(slot));
        if (quickCraftPreview) {
            stack = this.getQuickCraftPreviewStack(slot);
            graphics.fill(x, y, x + 16, y + 16, -2130706433);
        }
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
            graphics.itemDecorations(this.font, stack, x, y);
            if (hovered && this.carried.isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }

        if (hovered) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, x - 4, y - 4, 24, 24);
        }
    }

    private void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (MathUtil.isInRange(mouseX, mouseY, this.left + 106, this.top, this.left + 300, this.top + 13)) {
            Component tooltip = this.getCapacityTooltip();
            if (tooltip != null) {
                graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
            }
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 2, this.top + 23, this.left + 26, this.top + 43)) {
            graphics.setTooltipForNextFrame(
                Component.translatable(
                    "screen.anvilcraft.storage.search",
                    SettingClientStub.setting().storage().getSearch().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 28, this.top + 23, this.left + 52, this.top + 43)) {
            graphics.setTooltipForNextFrame(
                Component.translatable(
                    "screen.anvilcraft.storage.sort",
                    SettingClientStub.setting().storage().getSort().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 54, this.top + 23, this.left + 78, this.top + 43)) {
            graphics.setTooltipForNextFrame(
                Component.translatable(
                    "screen.anvilcraft.storage.order",
                    SettingClientStub.setting().storage().getOrder().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 80, this.top + 23, this.left + 104, this.top + 43)) {
            graphics.setTooltipForNextFrame(
                Component.translatable(
                    "screen.anvilcraft.storage.nbt",
                    SettingClientStub.setting().storage().getNbtDisplay().getModeName()
                ),
                mouseX,
                mouseY
            );
        }
    }

    private @Nullable Component getCapacityTooltip() {
        StorageServerStub.Capacity capacity = this.capacity;
        if (capacity == null) {
            return null;
        }
        boolean unlimitedSpace = capacity.spaceSize() == Integer.MAX_VALUE;
        boolean unlimitedTypes = capacity.typeLimit() == Integer.MAX_VALUE;
        if (unlimitedSpace && unlimitedTypes) {
            return null;
        }
        if (unlimitedSpace) {
            return Component.translatable(
                "screen.anvilcraft.storage.capacity.types",
                capacity.typeCount(),
                capacity.typeLimit()
            );
        }
        if (unlimitedTypes) {
            return Component.translatable(
                "screen.anvilcraft.storage.capacity.space",
                capacity.space(),
                capacity.spaceSize()
            );
        }
        return Component.translatable(
            "screen.anvilcraft.storage.capacity",
            capacity.space(),
            capacity.spaceSize(),
            capacity.typeCount(),
            capacity.typeLimit()
        );
    }

    private void extractCarriedItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.carried.isEmpty()) {
            return;
        }
        ItemStack renderedCarried = this.carried;
        if (this.quickCrafting && !this.quickCraftSlots.isEmpty()) {
            int remaining = this.getQuickCraftRemaining();
            if (remaining == 0) {
                return;
            }
            renderedCarried = this.carried.copyWithCount(remaining);
        }
        graphics.nextStratum();
        graphics.item(renderedCarried, mouseX - 8, mouseY - 8);
        graphics.itemDecorations(this.font, renderedCarried, mouseX - 8, mouseY - 8);
    }

    private ItemStack getQuickCraftPreviewStack(int inventorySlot) {
        Slot slot = this.player.inventoryMenu.getSlot(this.getScreenSlot(inventorySlot));
        int currentCount = slot.hasItem() ? slot.getItem().getCount() : 0;
        int maxCount = Math.min(this.carried.getMaxStackSize(), slot.getMaxStackSize(this.carried));
        int placedCount = AbstractContainerMenu.getQuickCraftPlaceCount(
            this.quickCraftSlots.size(),
            this.quickCraftingButton,
            this.carried
        );
        return this.carried.copyWithCount(Math.min(currentCount + placedCount, maxCount));
    }

    private int getQuickCraftRemaining() {
        int remaining = this.carried.getCount();
        for (int screenSlot : this.quickCraftSlots) {
            Slot slot = this.player.inventoryMenu.getSlot(screenSlot);
            int currentCount = slot.hasItem() ? slot.getItem().getCount() : 0;
            int maxCount = Math.min(this.carried.getMaxStackSize(), slot.getMaxStackSize(this.carried));
            int placedCount = AbstractContainerMenu.getQuickCraftPlaceCount(
                this.quickCraftSlots.size(),
                this.quickCraftingButton,
                this.carried
            );
            remaining -= Math.min(placedCount, maxCount - currentCount);
        }
        return Math.max(0, remaining);
    }
    // endregion

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int lastClickedInventorySlot = this.lastClickedInventorySlot;
        this.lastClickedInventorySlot = -1;
        if (this.search != null && (event.button() == 0 || event.button() == 1)) {
            boolean hovered = MathUtil.isInRange(event.x(), event.y(), this.left + 6, this.top + 6, this.left + 100, this.top + 16);
            this.search.setFocused(hovered);
            this.setFocused(hovered ? this.search : null);
        }

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() == 0 || event.button() == 1) {
            Integer storageSlot = this.getStorageSlot(event.x(), event.y());
            if (storageSlot != null && this.minecraft.gameMode != null) {
                StorageInput action = event.hasShiftDown()
                                      ? StorageInput.QUICK_MOVE_FROM_STORAGE
                                      : StorageInput.PICKUP;
                this.interactWithStorage(storageSlot, event.button(), action);
                return true;
            }

            int slot = this.getInventorySlot(event.x(), event.y());
            if (slot == -1 || this.minecraft.gameMode == null) {
                return false;
            }
            this.lastClickedInventorySlot = slot;

            if (event.hasShiftDown()) {
                this.interactWithStorage(slot, event.button(), StorageInput.QUICK_MOVE_TO_STORAGE);
                return true;
            }

            if (!this.carried.isEmpty()) {
                if (event.button() == 0 && doubleClick && slot == lastClickedInventorySlot) {
                    this.pickupAllSlot = this.getScreenSlot(slot);
                    return true;
                }
                this.quickCrafting = true;
                this.quickCraftingButton = event.button();
                this.quickCraftSlots.clear();
                return true;
            }

            this.player.inventoryMenu.setCarried(this.carried);
            this.minecraft.gameMode.handleContainerInput(
                this.player.inventoryMenu.containerId,
                this.getScreenSlot(slot),
                event.button(),
                ContainerInput.PICKUP,
                this.player
            );
            this.carried = this.player.inventoryMenu.getCarried();
            return true;
        } else if (event.button() == 2) {
            Integer storageSlot = this.getStorageSlot(event.x(), event.y());
            if (
                storageSlot != null
                && this.player.hasInfiniteMaterials()
                && this.carried.isEmpty()
                && this.minecraft.options.keyPickItem.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.input()))
            ) {
                this.interactWithStorage(storageSlot, 0, StorageInput.CLONE);
                return true;
            }

            int slot = this.getScreenSlot();
            if (slot == -1 || this.minecraft.gameMode == null) {
                return false;
            }

            if (!this.minecraft.options.keyPickItem.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.input()))) {
                return false;
            }

            this.minecraft.gameMode.handleContainerInput(
                this.player.inventoryMenu.containerId,
                slot,
                0,
                ContainerInput.CLONE,
                this.player
            );
            this.carried = this.player.inventoryMenu.getCarried();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!this.quickCrafting || event.button() != this.quickCraftingButton || this.carried.isEmpty()) {
            return super.mouseDragged(event, dragX, dragY);
        }

        int inventorySlot = this.getInventorySlot(event.x(), event.y());
        if (inventorySlot != -1) {
            int screenSlot = this.getScreenSlot(inventorySlot);
            Slot slot = this.player.inventoryMenu.getSlot(screenSlot);
            if (
                this.carried.getCount() > this.quickCraftSlots.size()
                && AbstractContainerMenu.canItemQuickReplace(slot, this.carried, true)
                && slot.mayPlace(this.carried)
                && this.player.inventoryMenu.canDragTo(slot)
            ) {
                this.quickCraftSlots.add(screenSlot);
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        super.mouseReleased(event);
        if (this.pickupAllSlot != -1) {
            if (event.button() == 0 && this.minecraft.gameMode != null) {
                this.player.inventoryMenu.setCarried(this.carried);
                this.minecraft.gameMode.handleContainerInput(
                    this.player.inventoryMenu.containerId,
                    this.pickupAllSlot,
                    0,
                    ContainerInput.PICKUP_ALL,
                    this.player
                );
                this.carried = this.player.inventoryMenu.getCarried();
            }
            this.pickupAllSlot = -1;
            return true;
        }
        if (!this.quickCrafting) {
            return false;
        }

        if (event.button() == this.quickCraftingButton && this.minecraft.gameMode != null) {
            this.player.inventoryMenu.setCarried(this.carried);
            if (this.quickCraftSlots.isEmpty()) {
                int inventorySlot = this.getInventorySlot(event.x(), event.y());
                if (inventorySlot != -1) {
                    this.minecraft.gameMode.handleContainerInput(
                        this.player.inventoryMenu.containerId,
                        this.getScreenSlot(inventorySlot),
                        event.button(),
                        ContainerInput.PICKUP,
                        this.player
                    );
                }
            } else {
                this.quickCraftToSlots(event.button());
            }
            this.carried = this.player.inventoryMenu.getCarried();
        }

        this.quickCrafting = false;
        this.quickCraftSlots.clear();
        return true;
    }

    private void quickCraftToSlots(int button) {
        if (this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleContainerInput(
            this.player.inventoryMenu.containerId,
            -999,
            AbstractContainerMenu.getQuickcraftMask(0, button),
            ContainerInput.QUICK_CRAFT,
            this.player
        );
        for (int slot : this.quickCraftSlots) {
            this.minecraft.gameMode.handleContainerInput(
                this.player.inventoryMenu.containerId,
                slot,
                AbstractContainerMenu.getQuickcraftMask(1, button),
                ContainerInput.QUICK_CRAFT,
                this.player
            );
        }
        this.minecraft.gameMode.handleContainerInput(
            this.player.inventoryMenu.containerId,
            -999,
            AbstractContainerMenu.getQuickcraftMask(2, button),
            ContainerInput.QUICK_CRAFT,
            this.player
        );
    }

    private void interactWithStorage(int slot, int button, StorageInput action) {
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        StorageClientStub.interact(this.sourcePos, slot, button, action).whenCompleteAsync(
            (result, error) -> {
                this.interactionPending = false;
                if (request != this.interactionRequest || error != null) {
                    return;
                }
                this.carried = result.carried();
                this.player.inventoryMenu.setCarried(this.carried);
                if (result.changed()) {
                    this.reorder(false);
                }
            },
            this.screenExecutor
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (
            scrollY == 0
            || !MathUtil.isInRange(
                mouseX,
                mouseY,
                this.left + StorageScreen.STORAGE_X - 2,
                this.top + StorageScreen.SLIDER_Y,
                this.left + StorageScreen.SLIDER_X + StorageScreen.SLIDER_WIDTH,
                this.top + StorageScreen.STORAGE_Y + StorageScreen.STORAGE_ROWS * StorageScreen.SLOT_SIZE
            )
        ) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int nextScrollRow = Mth.clamp(
            this.scrollRow + (scrollY > 0 ? -1 : 1),
            0,
            this.getMaxScrollRow()
        );
        if (nextScrollRow != this.scrollRow) {
            this.scrollRow = nextScrollRow;
            this.syncVisible();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.search != null && this.search.isFocused()) {
            this.search.keyPressed(event);
            return true;
        }

        InputConstants.Key key = InputConstants.getKey(event);
        if (super.keyPressed(event)) {
            return true;
        } else if (this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            this.onClose();
            return true;
        } else {
            Integer storageSlot = this.getStorageSlot();
            if (storageSlot != null && this.minecraft.gameMode != null) {
                if (this.minecraft.options.keyPickItem.isActiveAndMatches(key)) {
                    this.interactWithStorage(storageSlot, 0, StorageInput.CLONE);
                    return true;
                } else if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
                    int dropMode = event.hasControlDown() ? event.hasShiftDown() ? 2 : 1 : 0;
                    this.interactWithStorage(storageSlot, dropMode, StorageInput.THROW);
                    return true;
                }
            }

            int hoveredSlot = this.getInventorySlot();
            if (hoveredSlot == -1 || this.minecraft.gameMode == null) {
                return false;
            }

            // Forge MC-146650: Needs to return true when the key is handled
            boolean handled = this.checkHotbarKeyPressed(event);
            if (!Objects.requireNonNull(this.minecraft.player).getInventory().getItem(hoveredSlot).isEmpty()) {
                hoveredSlot = this.getScreenSlot(hoveredSlot);
                if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
                    this.minecraft.gameMode.handleContainerInput(
                        this.player.inventoryMenu.containerId,
                        hoveredSlot,
                        event.hasControlDown() ? 1 : 0,
                        ContainerInput.THROW,
                        this.player
                    );
                    handled = true;
                }
            } else if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
                // Forge MC-146650: Emulate MC bug, so we don't drop from hotbar when pressing drop without hovering over an item.
                handled = true;
            }

            return handled;
        }
    }

    protected boolean checkHotbarKeyPressed(KeyEvent event) {
        int hoveredSlot = this.getScreenSlot();
        if (hoveredSlot == -1 || this.minecraft.gameMode == null) {
            return false;
        }

        InputConstants.Key key = InputConstants.getKey(event);
        if (this.carried.isEmpty()) {
            if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(key)) {
                this.minecraft.gameMode.handleContainerInput(
                    this.player.inventoryMenu.containerId,
                    hoveredSlot,
                    40,
                    ContainerInput.SWAP,
                    this.player
                );
                return true;
            }
            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].isActiveAndMatches(key)) {
                    this.minecraft.gameMode.handleContainerInput(
                        this.player.inventoryMenu.containerId,
                        hoveredSlot,
                        i,
                        ContainerInput.SWAP,
                        this.player
                    );
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void removed() {
        this.reorderRequest++;
        this.syncRequest++;
        this.metadataPending = false;
        if (!this.carried.isEmpty() && this.minecraft.gameMode != null) {
            this.player.inventoryMenu.setCarried(this.carried);
            Inventory inventory = this.player.getInventory();
            while (!this.carried.isEmpty()) {
                int slot = inventory.getSlotWithRemainingSpace(this.carried);
                if (slot == -1) {
                    slot = inventory.getFreeSlot();
                }
                if (slot == -1) {
                    this.minecraft.gameMode.handleContainerInput(
                        this.player.inventoryMenu.containerId,
                        -999,
                        0,
                        ContainerInput.PICKUP,
                        this.player
                    );
                    break;
                }

                this.minecraft.gameMode.handleContainerInput(
                    this.player.inventoryMenu.containerId,
                    slot < 9 ? slot + 36 : slot,
                    0,
                    ContainerInput.PICKUP,
                    this.player
                );
                this.carried = this.player.inventoryMenu.getCarried();
            }
            this.carried = ItemStack.EMPTY;
        }
        if (SettingClientStub.setting().storage().getSearch() == SearchMode.CLEAR) {
            SettingClientStub.update("");
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private @Nullable Integer getStorageSlot(double mouseX, double mouseY) {
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        for (int displayIndex = 0; displayIndex < StorageScreen.VISIBLE_STORAGE_SLOTS; displayIndex++) {
            int orderIndex = firstOrderIndex + displayIndex;
            int x = this.left + StorageScreen.STORAGE_X
                + displayIndex % StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            int y = this.top + StorageScreen.STORAGE_Y
                + displayIndex / StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                if (orderIndex < this.order.size()) {
                    return this.order.getInt(orderIndex);
                }
                return this.carried.isEmpty() ? null : -1;
            }
        }
        return null;
    }

    private @Nullable Integer getStorageSlot() {
        Window window = this.minecraft.getWindow();
        MouseHandler handler = this.minecraft.mouseHandler;
        return this.getStorageSlot(handler.getScaledXPos(window), handler.getScaledYPos(window));
    }

    private int getInventorySlot(double mouseX, double mouseY) {
        int y = this.top + 140 + 58;
        for (int column = 0; column < 9; column++) {
            int x = this.left + 114 + 18 * column;
            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                return column;
            }
        }

        for (int row = 0; row < 3; row++) {
            y = this.top + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.left + 114 + 18 * column;
                if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                    return slot;
                }
                slot++;
            }
        }
        return -1;
    }

    private int getInventorySlot() {
        Window window = this.minecraft.getWindow();
        MouseHandler handler = this.minecraft.mouseHandler;
        return this.getInventorySlot(handler.getScaledXPos(window), handler.getScaledYPos(window));
    }

    private int getScreenSlot(int invSlot) {
        return invSlot >= 0 && invSlot < 9 ? invSlot + 36 : invSlot;
    }

    private int getScreenSlot() {
        return this.getScreenSlot(this.getInventorySlot());
    }

    private void reorder() {
        this.reorder(true);
    }

    private void reorder(boolean resetScroll) {
        int request = ++this.reorderRequest;
        if (resetScroll) {
            this.scrollRow = 0;
        }
        StorageClientStub.reorder(this.sourcePos).whenCompleteAsync(
            (updatedOrder, error) -> {
                if (request != this.reorderRequest || error != null) {
                    return;
                }
                this.order = new IntArrayList(updatedOrder);
                this.orderLoaded = true;
                this.scrollRow = Mth.clamp(this.scrollRow, 0, this.getMaxScrollRow());
                this.syncVisible();
            },
            this.screenExecutor
        );
    }

    private void syncVisible() {
        int request = ++this.syncRequest;
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        int endOrderIndex = Math.min(firstOrderIndex + StorageScreen.VISIBLE_STORAGE_SLOTS, this.order.size());
        IntArrayList slots = new IntArrayList(endOrderIndex - firstOrderIndex);
        for (int orderIndex = firstOrderIndex; orderIndex < endOrderIndex; orderIndex++) {
            slots.add(this.order.getInt(orderIndex));
        }

        StorageClientStub.sync(this.sourcePos, slots).whenCompleteAsync(
            (result, error) -> {
                if (request != this.syncRequest || error != null || result.version() < this.version) {
                    return;
                }
                this.version = result.version();
                this.fullness = result.fullness();
                for (StorageServerStub.StackUpdate update : result.updates()) {
                    if (update.stack().isEmpty()) {
                        this.contents.remove(update.index());
                    } else {
                        this.contents.put(update.index(), update.stack());
                    }
                }
            },
            this.screenExecutor
        );
    }

    private void refreshMetadata() {
        if (this.metadataPending) {
            return;
        }
        this.metadataPending = true;
        this.metadataCooldown = StorageScreen.METADATA_REFRESH_INTERVAL;
        StorageClientStub.loadMetadata(this.sourcePos).whenCompleteAsync(
            (metadata, error) -> {
                this.metadataPending = false;
                if (error != null) {
                    return;
                }
                this.fullness = metadata.fullness();
                this.capacity = metadata.capacity();
                if (!this.orderLoaded || metadata.orderVersion() != this.orderVersion) {
                    this.orderVersion = metadata.orderVersion();
                    this.reorder(false);
                } else if (metadata.version() != this.version) {
                    this.syncVisible();
                }
            },
            this.screenExecutor
        );
    }

    private int getMaxScrollRow() {
        return Math.max(
            0,
            Math.ceilDiv(this.order.size(), StorageScreen.STORAGE_COLUMNS) - StorageScreen.STORAGE_ROWS
        );
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void itemDecorations(GuiGraphicsExtractor graphic, Minecraft minecraft, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }

        graphic.pose().pushMatrix();
        // region graphic.itemBar(stack, x, y);
        if (stack.isBarVisible()) {
            int left = x + 2;
            int top = y + 13;
            graphic.fill(RenderPipelines.GUI, left, top, left + 13, top + 2, -16777216);
            graphic.fill(RenderPipelines.GUI, left, top, left + stack.getBarWidth(), top + 1, ARGB.opaque(stack.getBarColor()));
        }
        // endregion
        // region graphic.itemCooldown(stack, x, y);
        LocalPlayer player = minecraft.player;
        float cooldown = player == null
                         ? 0.0F
                         : player.getCooldowns().getCooldownPercent(stack, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (cooldown > 0.0F) {
            int top = y + Mth.floor(16.0F * (1.0F - cooldown));
            int bottom = top + Mth.ceil(16.0F * cooldown);
            graphic.fill(RenderPipelines.GUI, x, top, x + 16, bottom, Integer.MAX_VALUE);
        }
        // endregion
        // region graphic.itemCount(minecraft.font, stack, x, y, null);
        if (stack.getCount() != 1) {
            Component amount = Component.literal(FormattingUtil.toAbbrNum(stack.getCount()))
                .withStyle(style -> style.withFont(new FontDescription.Resource(StorageScreen.SMALL_FONT)));
            graphic.text(minecraft.font, amount, x + 17 - minecraft.font.width(amount), y + 9, -1, true);
        }
        // endregion
        graphic.pose().popMatrix();
        ItemDecoratorHandler.of(stack).render(graphic, minecraft.font, stack, x, y);
    }
}
