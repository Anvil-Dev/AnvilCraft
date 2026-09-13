package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.rpc.StorageInput;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.util.FluidAmountUtil;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ItemDecoratorHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
    private static final int VISIBLE_STORAGE_SLOTS = StorageScreen.STORAGE_COLUMNS * StorageScreen.STORAGE_ROWS;
    private static final int STORAGE_X = 114;
    private static final int STORAGE_Y = 18;
    private static final int SLOT_SIZE = 18;
    private static final int SLIDER_X = 280;
    private static final int SLIDER_Y = 18;
    private static final int SLIDER_WIDTH = 12;
    private static final int SLIDER_HEIGHT = 15;
    private static final int SLIDER_TRACK_HEIGHT = 106;
    private static final int METADATA_REFRESH_INTERVAL = 10;
    private static final int MAX_PRESERVED_SYNC_ATTEMPTS = 3;
    private static final int FLUID_SLOT_BASE = StoragePortManager.FLUID_SLOT_BASE;
    private static final int FLYOUT_FADE_IN_TICKS = 5;
    private static final int FLYOUT_HOLD_TICKS = 25;
    private static final int FLYOUT_FADE_OUT_TICKS = 5;
    private static final int FLYOUT_TOTAL_TICKS = FLYOUT_FADE_IN_TICKS + FLYOUT_HOLD_TICKS + FLYOUT_FADE_OUT_TICKS;
    private static final Identifier FLYOUT_BACK = AnvilCraft.of("flex_button/shaded_1px");
    private final BlockPos sourcePos;
    private final Player player;
    private final boolean tracksOpenState;

    private @Nullable EditBox search;
    private @Nullable CategoryList categories;

    private ItemStack carried = ItemStack.EMPTY;
    private IntList order = new IntArrayList();
    private IntList displayOrder = new IntArrayList();
    private final Int2ObjectMap<UnlimitedItemStack> contents = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<UnlimitedItemStack> foldedContents = new Int2ObjectOpenHashMap<>();
    private final Int2IntMap foldedCounts = new Int2IntOpenHashMap();
    private final Int2IntMap serverSlots = new Int2IntOpenHashMap();
    private final IntSet emptySlots = new IntOpenHashSet();
    private List<IntList> foldedGroups = List.of();
    private List<StorageServerStub.FluidEntry> fluids = List.of();
    private Component flyoutMessage = Component.empty();
    private int flyoutTimer = FLYOUT_TOTAL_TICKS;
    private int flyoutClickX;
    private int flyoutClickY;
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
    private boolean interactionSyncPending;
    private boolean nbtFolded;
    private boolean preservingOrder;
    private boolean remappedOrder;
    private int nextLogicalSlot;
    private final IntSet quickCraftSlots = new IntOpenHashSet();
    private boolean quickCrafting;
    private boolean quickMoveDragging;
    private final IntSet quickMoveSlots = new IntOpenHashSet();
    private final IntSet pendingQuickMoveSlots = new IntOpenHashSet();
    private final IntSet storageQuickMoveSlots = new IntOpenHashSet();
    private int quickCraftingButton;
    private int lastClickedInventorySlot = -1;
    private long lastInventoryClickTime;
    private int lastInventoryClickSlot = -1;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;
    private int pickupAllSlot = -1;
    private int left;
    private int top;
    private int titleLabelX;

    public StorageScreen(BlockPos sourcePos) {
        super(Objects.requireNonNull(Minecraft.getInstance().level).getBlockState(sourcePos).getBlock().getName());
        this.sourcePos = sourcePos;
        this.player = Objects.requireNonNull(Minecraft.getInstance().player);
        this.serverSlots.defaultReturnValue(-1);
        this.tracksOpenState = Minecraft.getInstance().level.getBlockState(sourcePos).getBlock()
            instanceof ShulkerContainerBlock;
    }

    public static void openScreen(BlockPos sourcePos) {
        Minecraft.getInstance().setScreenAndShow(new StorageScreen(sourcePos));
    }

    @Override
    protected void init() {
        if (this.tracksOpenState) {
            StorageClientStub.setOpen(this.sourcePos, true);
        }
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
            (_, index) -> {
                SettingClientStub.update(NbtDisplayMode.values()[index]);
                this.reorder();
            }
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
            _ -> this.deposit(true, this.minecraft.hasShiftDown())
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
                Objects.requireNonNull(this.search).setValue(storage.getSearchContent());
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
        this.flushQuickMoves();
        if (this.flyoutTimer < StorageScreen.FLYOUT_TOTAL_TICKS) this.flyoutTimer++;
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
        this.extractFlyout(graphics);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    private void extractStorageContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        for (int displayIndex = 0; displayIndex < StorageScreen.VISIBLE_STORAGE_SLOTS; displayIndex++) {
            int orderIndex = firstOrderIndex + displayIndex;
            if (orderIndex >= this.displayOrder.size()) {
                break;
            }

            int x = this.left + StorageScreen.STORAGE_X
                + displayIndex % StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            int y = this.top + StorageScreen.STORAGE_Y
                + displayIndex / StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            boolean hovered = MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17);
            if (hovered) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, StorageScreen.SLOT_HIGHLIGHT_BACK_SPRITE, x - 4, y - 4, 24, 24);
            }

            int slot = this.displayOrder.getInt(orderIndex);
            if (slot >= StorageScreen.FLUID_SLOT_BASE) {
                var entry = this.getFluidSlot(slot);
                if (entry != null) {
                    this.extractFluidIcon(graphics, entry, x, y);
                    if (hovered) {
                        graphics.setTooltipForNextFrame(this.font, List.of(entry.icon().getHoverName().getVisualOrderText(),
                            Component.translatable("screen.anvilcraft.storage.fluid_amount",
                                FluidAmountUtil.formatExactAmount(entry.amount())).getVisualOrderText()), mouseX, mouseY);
                    }
                }
                if (hovered) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, StorageScreen.SLOT_HIGHLIGHT_FRONT_SPRITE,
                        x - 4, y - 4, 24, 24);
                }
                continue;
            }
            UnlimitedItemStack stack = this.getDisplayedStack(slot);
            if (!stack.isEmpty()) {
                ItemStack itemStack = stack.toStack();
                graphics.item(itemStack, x, y);
                StorageScreen.itemDecorations(
                    graphics,
                    this.minecraft,
                    itemStack,
                    this.getDisplayedCount(slot, stack),
                    x,
                    y
                );
                if (hovered && this.carried.isEmpty()) {
                    graphics.setTooltipForNextFrame(this.font, itemStack, mouseX, mouseY);
                }
            }

            if (hovered) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, StorageScreen.SLOT_HIGHLIGHT_FRONT_SPRITE, x - 4, y - 4, 24, 24);
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
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, StorageScreen.SLOT_HIGHLIGHT_BACK_SPRITE, x - 4, y - 4, 24, 24);
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
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, StorageScreen.SLOT_HIGHLIGHT_FRONT_SPRITE, x - 4, y - 4, 24, 24);
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
        if (capacity.typeLimit() != Integer.MAX_VALUE) {
            return Component.translatable("screen.anvilcraft.storage.capacity.types", capacity.typeCount(), capacity.typeLimit());
        }
        return Component.translatable("screen.anvilcraft.storage.capacity.space", capacity.space(), capacity.spaceSize());
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

        if (event.button() == 1 && MathUtil.isInRange(event.x(), event.y(),
            this.left + 278, this.top + 139, this.left + 296, this.top + 159)) {
            this.deposit(false, event.hasShiftDown());
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() == 0 || event.button() == 1) {
            Integer fluidSlot = this.getFluidSlotAt(event.x(), event.y());
            if (fluidSlot != null && this.minecraft.gameMode != null) {
                if (event.button() == 1) {
                    if (!this.carried.isEmpty()) this.interactWithStorage(fluidSlot, event.button(), StorageInput.PICKUP);
                    return true;
                }
                this.flyoutClickX = (int) event.x();
                this.flyoutClickY = (int) event.y();
                this.interactWithStorage(fluidSlot, event.button(), event.hasShiftDown()
                    ? StorageInput.QUICK_MOVE_FROM_STORAGE : StorageInput.FLUID_BUCKET);
                return true;
            }
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
            ItemStack clickedItem = this.player.getInventory().getItem(slot);
            if (!clickedItem.isEmpty()) this.lastQuickMoved = clickedItem.copy();
            if (event.hasAltDown()) {
                this.moveSameToStorage(slot, event.button() == 0);
                return true;
            }
            boolean inventoryDoubleClick = event.button() == 0 && this.isInventoryDoubleClick(slot);

            if (event.hasShiftDown()) {
                if (inventoryDoubleClick) {
                    int target = this.findInventorySlotWith(this.lastQuickMoved);
                    if (target >= 0) this.moveSameToStorage(target, true);
                    return true;
                }
                if (event.button() == 0 && this.carried.isEmpty()) {
                    this.quickMoveDragging = true;
                    StorageClientStub.beginUndoGroup(this.sourcePos);
                    this.queueQuickMove(slot);
                } else {
                    this.interactWithStorage(slot, event.button(), StorageInput.QUICK_MOVE_TO_STORAGE);
                }
                return true;
            }

            if (!this.carried.isEmpty()) {
                if (inventoryDoubleClick && slot == lastClickedInventorySlot) {
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
        if (this.quickMoveDragging) {
            if (event.button() == 0 && event.hasShiftDown()) this.quickMoveDrag(event.x(), event.y());
            return true;
        }
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
        if (this.quickMoveDragging) {
            this.finishQuickMove();
            return true;
        }
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

    private void queueQuickMove(int slot) {
        if (slot >= 0 && this.quickMoveSlots.add(slot)) this.pendingQuickMoveSlots.add(slot);
    }

    public void quickMoveDrag(double mouseX, double mouseY) {
        Integer storageSlot = this.getStorageSlot(mouseX, mouseY);
        if (storageSlot != null && storageSlot >= 0) {
            int key = -1 - storageSlot;
            if (this.quickMoveSlots.add(key)) {
                this.storageQuickMoveSlots.add(storageSlot.intValue());
            } else {
                this.quickMoveSlots.remove(key);
                this.storageQuickMoveSlots.remove(storageSlot.intValue());
            }
            return;
        }
        this.queueQuickMove(this.getInventorySlot(mouseX, mouseY));
    }

    private boolean isInventoryDoubleClick(int slot) {
        long now = System.currentTimeMillis();
        boolean quick = slot == this.lastInventoryClickSlot && now - this.lastInventoryClickTime < 250L;
        this.lastInventoryClickSlot = slot;
        this.lastInventoryClickTime = now;
        return quick;
    }

    private int findInventorySlotWith(ItemStack sample) {
        if (sample.isEmpty()) return -1;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (ItemStack.isSameItemSameComponents(this.player.getInventory().getItem(slot), sample)) return slot;
        }
        return -1;
    }

    private void moveSameToStorage(int slot, boolean pour) {
        StorageClientStub.moveSameToStorage(this.sourcePos, slot, pour).thenAcceptAsync(changed -> {
            if (changed) this.refreshAfterQuickMove();
        }, this.screenExecutor);
    }

    private void flushQuickMoves() {
        if (!this.pendingQuickMoveSlots.isEmpty()) {
            IntList slots = new IntArrayList(this.pendingQuickMoveSlots);
            this.pendingQuickMoveSlots.clear();
            StorageClientStub.quickMoveToStorage(this.sourcePos, slots).thenAcceptAsync(changed -> {
                if (changed) this.refreshAfterQuickMove();
            }, this.screenExecutor);
        }
        if (!this.storageQuickMoveSlots.isEmpty()) {
            IntList slots = new IntArrayList(this.storageQuickMoveSlots.size());
            for (int logicalSlot : this.storageQuickMoveSlots) {
                int serverSlot = this.serverSlots.get(logicalSlot);
                if (serverSlot >= 0 && serverSlot < StorageScreen.FLUID_SLOT_BASE) slots.add(serverSlot);
            }
            this.storageQuickMoveSlots.clear();
            if (slots.isEmpty()) return;
            StorageClientStub.quickMoveFromStorage(this.sourcePos, slots).thenAcceptAsync(changed -> {
                if (changed) this.refreshAfterQuickMove();
            }, this.screenExecutor);
        }
    }

    private void finishQuickMove() {
        this.quickMoveDragging = false;
        this.flushQuickMoves();
        StorageClientStub.endUndoGroup(this.sourcePos);
        this.quickMoveSlots.clear();
    }

    private void refreshAfterQuickMove() {
        if (this.preservingOrder) {
            this.interactionSyncPending = true;
            this.syncPreservedOrder();
        } else {
            this.reorder(false);
        }
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
        int serverSlot = action == StorageInput.QUICK_MOVE_TO_STORAGE || slot >= StorageScreen.FLUID_SLOT_BASE
            ? slot : this.serverSlots.get(slot);
        var fluidEntry = slot >= StorageScreen.FLUID_SLOT_BASE ? this.getFluidSlot(slot) : null;
        FluidStack fluidIdentity = fluidEntry == null ? FluidStack.EMPTY : fluidEntry.icon().copyWithAmount(FluidType.BUCKET_VOLUME);
        StorageClientStub.interact(this.sourcePos, serverSlot, button, action, fluidIdentity).whenCompleteAsync(
            (result, error) -> {
                if (request != this.interactionRequest || error != null) {
                    this.interactionPending = false;
                    return;
                }
                if (result.notice() != StorageServerStub.FluidNotice.NONE) this.showNotice(result.notice().text());
                this.carried = result.carried();
                this.player.inventoryMenu.setCarried(this.carried);
                if (result.changed()) {
                    if (this.preservingOrder) {
                        this.interactionSyncPending = true;
                        this.syncPreservedOrder();
                        return;
                    }
                    this.reorder(false);
                }
                this.interactionPending = false;
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
            if (!this.nbtFolded) {
                this.syncVisible();
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (
            (event.key() == InputConstants.KEY_LSHIFT || event.key() == InputConstants.KEY_RSHIFT)
            && !this.preservingOrder
        ) {
            this.preservingOrder = true;
            this.reorderRequest++;
            this.syncRequest++;
        }

        if (this.search != null && this.search.isFocused()) {
            this.search.keyPressed(event);
            return true;
        }

        if (event.hasControlDown() && event.key() == InputConstants.KEY_Z) {
            StorageClientStub.undo(this.sourcePos).thenAcceptAsync(result -> {
                if (result.changed()) this.refreshAfterQuickMove();
            }, this.screenExecutor);
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

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (
            (event.key() == InputConstants.KEY_LSHIFT || event.key() == InputConstants.KEY_RSHIFT)
            && !event.hasShiftDown()
            && this.preservingOrder
        ) {
            this.preservingOrder = false;
            this.reorder(false);
            return true;
        }
        return super.keyReleased(event);
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
        if (this.quickMoveDragging && this.minecraft.player != null) this.finishQuickMove();
        this.reorderRequest++;
        this.syncRequest++;
        this.metadataPending = false;
        if (this.tracksOpenState && this.minecraft.player != null) {
            StorageClientStub.setOpen(this.sourcePos, false);
        }
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

    public int getLeftPos() {
        return this.left;
    }

    public int getTopPos() {
        return this.top;
    }

    public int getImageWidth() {
        return StorageScreen.BG_WIDTH;
    }

    public int getImageHeight() {
        return StorageScreen.BG_HEIGHT;
    }

    public @Nullable ItemStack getItemUnderMouse(double mouseX, double mouseY) {
        ItemArea itemArea = this.getItemAreaData(mouseX, mouseY);
        return itemArea == null ? null : itemArea.stack().copy();
    }

    public @Nullable Rect2i getItemArea(double mouseX, double mouseY) {
        ItemArea itemArea = this.getItemAreaData(mouseX, mouseY);
        return itemArea == null ? null : new Rect2i(itemArea.x(), itemArea.y(), 16, 16);
    }

    private @Nullable ItemArea getItemAreaData(double mouseX, double mouseY) {
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        for (int displayIndex = 0; displayIndex < StorageScreen.VISIBLE_STORAGE_SLOTS; displayIndex++) {
            int orderIndex = firstOrderIndex + displayIndex;
            int x = this.left + StorageScreen.STORAGE_X
                    + displayIndex % StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            int y = this.top + StorageScreen.STORAGE_Y
                    + displayIndex / StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                if (orderIndex >= this.displayOrder.size()) {
                    return null;
                }
                UnlimitedItemStack stack = this.getDisplayedStack(this.displayOrder.getInt(orderIndex));
                return stack.isEmpty() ? null : new ItemArea(stack.toStack(), x, y);
            }
        }

        int inventorySlot = this.getInventorySlot(mouseX, mouseY);
        if (inventorySlot == -1) {
            return null;
        }
        ItemStack stack = this.player.getInventory().getItem(inventorySlot);
        if (stack.isEmpty()) {
            return null;
        }
        int x = this.left + 114 + 18 * (inventorySlot % 9);
        int y = inventorySlot < 9
                ? this.top + 140 + 58
                : this.top + 140 + 18 * ((inventorySlot - 9) / 9);
        return new ItemArea(stack, x, y);
    }

    private record ItemArea(ItemStack stack, int x, int y) {
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
                if (orderIndex < this.displayOrder.size()) {
                    int slot = this.displayOrder.getInt(orderIndex);
                    return slot >= StorageScreen.FLUID_SLOT_BASE ? null : slot;
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

    private @Nullable Integer getFluidSlotAt(double mouseX, double mouseY) {
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        for (int displayIndex = 0; displayIndex < StorageScreen.VISIBLE_STORAGE_SLOTS; displayIndex++) {
            int orderIndex = firstOrderIndex + displayIndex;
            if (orderIndex >= this.displayOrder.size()) {
                break;
            }
            int x = this.left + StorageScreen.STORAGE_X
                + displayIndex % StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            int y = this.top + StorageScreen.STORAGE_Y
                + displayIndex / StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                int slot = this.displayOrder.getInt(orderIndex);
                return slot >= StorageScreen.FLUID_SLOT_BASE ? slot : null;
            }
        }
        return null;
    }

    private StorageServerStub.@Nullable FluidEntry getFluidSlot(int slot) {
        int index = slot - StorageScreen.FLUID_SLOT_BASE;
        return index >= 0 && index < this.fluids.size() ? this.fluids.get(index) : null;
    }

    private IntList appendFluidSlots(IntList itemsOnly) {
        if (this.fluids.isEmpty()) {
            return itemsOnly;
        }
        IntArrayList result = new IntArrayList(itemsOnly.size() + this.fluids.size());
        result.addAll(itemsOnly);
        for (int index = 0; index < this.fluids.size(); index++) {
            int slot = StorageScreen.FLUID_SLOT_BASE + index;
            if (this.order.contains(slot)) {
                result.add(slot);
            }
        }
        return result;
    }

    private IntList applySearchFilter(IntList order) {
        String search = SettingClientStub.setting().storage().getSearchContent().strip().toLowerCase(Locale.ROOT);
        if (search.isEmpty() || search.charAt(0) == '@' || search.charAt(0) == '#') {
            return order;
        }
        IntArrayList filtered = new IntArrayList(order.size());
        for (int slot : order) {
            // 流体伪槽位按流体名称与 id path 过滤，不能当作空物品丢弃
            if (slot >= StorageScreen.FLUID_SLOT_BASE) {
                StorageServerStub.FluidEntry entry = this.getFluidSlot(slot);
                if (entry == null) {
                    continue;
                }
                FluidStack icon = entry.icon();
                String fluidName = icon.getHoverName().getString().toLowerCase(Locale.ROOT);
                String fluidIdPath = BuiltInRegistries.FLUID.getKey(icon.getFluid()).getPath();
                if (fluidName.contains(search) || fluidIdPath.contains(search)) {
                    filtered.add(slot);
                }
                continue;
            }
            UnlimitedItemStack stack = this.getDisplayedStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.toStack().getHoverName().getString().toLowerCase(Locale.ROOT);
            String idPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (name.contains(search) || idPath.contains(search)) {
                filtered.add(slot);
            }
        }
        return filtered;
    }

    private void extractFluidIcon(GuiGraphicsExtractor graphics, StorageServerStub.FluidEntry entry, int x, int y) {
        var model = FluidRenderHelper.getModel(this.minecraft.getModelManager().getFluidStateModelSet(), entry.icon().getFluid());
        var tint = model.fluidTintSource();
        int color = tint == null ? -1 : tint.colorAsStack(entry.icon());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, model.stillMaterial().sprite(), x, y, 16, 16, ARGB.opaque(color));
        Component amount = Component.literal(FluidAmountUtil.formatAmount(entry.amount()))
            .withStyle(style -> style.withFont(new FontDescription.Resource(StorageScreen.SMALL_FONT)));
        StorageScreen.renderSlotCount(graphics, this.font, amount, entry.amount() == 0 ? 0xFFFFAA00 : -1, x, y);
    }

    private void showNotice(Component message) {
        if (message.getString().isEmpty()) return;
        this.flyoutMessage = message;
        this.flyoutTimer = 0;
    }

    private static void renderSlotCount(GuiGraphicsExtractor graphics, Font font, Component text, int color, int x, int y) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 17, y + 9);
        int width = font.width(text);
        if (width > 16) {
            graphics.pose().scale(0.75F, 0.75F);
            graphics.pose().translate(-1.0F, font.lineHeight * 0.25F - 0.25F);
        }
        graphics.text(font, text, -width, 0, color, true);
        graphics.pose().popMatrix();
    }

    private void extractFlyout(GuiGraphicsExtractor graphics) {
        int elapsed = this.flyoutTimer - StorageScreen.FLYOUT_FADE_IN_TICKS;
        float alpha = this.flyoutTimer < StorageScreen.FLYOUT_FADE_IN_TICKS
            ? this.flyoutTimer / (float) StorageScreen.FLYOUT_FADE_IN_TICKS
            : elapsed < StorageScreen.FLYOUT_HOLD_TICKS ? 1.0F
            : 1.0F - (elapsed - StorageScreen.FLYOUT_HOLD_TICKS) / (float) StorageScreen.FLYOUT_FADE_OUT_TICKS;
        if (alpha <= 0) return;
        int width = this.font.width(this.flyoutMessage) + 5;
        int height = this.font.lineHeight + 6;
        int x = Mth.clamp(this.flyoutClickX - width / 2, 4, Math.max(4, this.width - width - 4));
        int y = Math.max(4, this.flyoutClickY - 18 - height);
        int color = (int) (alpha * 255.0F) << 24 | 0xFFFFFF;
        graphics.nextStratum();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, StorageScreen.FLYOUT_BACK, x, y, width, height, color);
        graphics.text(this.font, this.flyoutMessage.copy().withColor(0xEE0000), x + 3, y + 3, color, false);
    }

    private void deposit(boolean pour, boolean all) {
        StorageClientStub.deposit(this.sourcePos, all, pour).thenAcceptAsync(result -> {
            if (result.changed()) this.reorder(false);
        }, this.screenExecutor);
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
                IntList reordered = new IntArrayList(updatedOrder);
                this.orderLoaded = true;
                this.syncReordered(reordered, this.scrollRow, request);
            },
            this.screenExecutor
        );
    }

    private void syncReordered(IntList reordered, int requestedScrollRow, int reorderRequest) {
        boolean foldNbt = SettingClientStub.setting().storage().getNbtDisplay() == NbtDisplayMode.FOLD;
        int reorderedScrollRow = foldNbt
                                 ? requestedScrollRow
                                 : Mth.clamp(requestedScrollRow, 0, this.getMaxScrollRow(reordered));
        int request = ++this.syncRequest;
        this.syncSlots(reordered).whenCompleteAsync(
            (results, error) -> {
                if (reorderRequest != this.reorderRequest) {
                    return;
                }
                if (error != null) {
                    this.orderLoaded = false;
                    return;
                }
                if (request != this.syncRequest) {
                    this.reorder(false);
                    return;
                }
                if (!this.applyReorderedSyncResults(results) || !this.hasContents(reordered)) {
                    this.reorder(false);
                    return;
                }
                this.order = reordered;
                this.resetServerSlots(reordered);
                this.rebuildDisplayOrder(foldNbt);
                this.scrollRow = Mth.clamp(reorderedScrollRow, 0, this.getMaxScrollRow());
                this.finishInteractionSync();
            },
            this.screenExecutor
        );
    }

    private void syncVisible() {
        if (this.preservingOrder) {
            this.syncPreservedOrder();
            return;
        }
        if (this.remappedOrder) {
            this.reorder(false);
            return;
        }
        boolean foldNbt = this.nbtFolded;
        IntList slots = foldNbt
                        ? this.order
                        : this.getVisibleSlots(this.displayOrder, this.scrollRow);
        int request = ++this.syncRequest;
        this.syncSlots(slots).whenCompleteAsync(
            (results, error) -> {
                if (request != this.syncRequest || error != null) {
                    return;
                }
                if (this.applySyncResults(results)) {
                    if (foldNbt) {
                        this.rebuildFoldedDisplay(true);
                        this.scrollRow = Mth.clamp(this.scrollRow, 0, this.getMaxScrollRow());
                    }
                } else {
                    this.reorder(false);
                }
            },
            this.screenExecutor
        );
    }

    private void syncPreservedOrder() {
        this.syncPreservedOrder(1);
    }

    private void syncPreservedOrder(int attempt) {
        int request = ++this.syncRequest;
        int reorderRequest = this.reorderRequest;
        StorageClientStub.reorder(this.sourcePos).whenCompleteAsync(
            (updatedOrder, reorderError) -> {
                if (
                    request != this.syncRequest
                    || reorderRequest != this.reorderRequest
                    || !this.preservingOrder
                    || reorderError != null
                ) {
                    return;
                }
                IntList currentOrder = new IntArrayList(updatedOrder);
                this.syncSlots(currentOrder).whenCompleteAsync(
                    (results, syncError) -> {
                        if (
                            request != this.syncRequest
                            || reorderRequest != this.reorderRequest
                            || !this.preservingOrder
                            || syncError != null
                        ) {
                            return;
                        }
                        StorageClientStub.reorder(this.sourcePos).whenCompleteAsync(
                            (confirmedOrder, confirmationError) -> {
                                if (
                                    request != this.syncRequest
                                    || reorderRequest != this.reorderRequest
                                    || !this.preservingOrder
                                    || confirmationError != null
                                ) {
                                    return;
                                }
                                if (!currentOrder.equals(confirmedOrder) || !this.applyPreservedSyncResults(results)) {
                                    if (attempt < StorageScreen.MAX_PRESERVED_SYNC_ATTEMPTS) {
                                        this.syncPreservedOrder(attempt + 1);
                                    } else {
                                        this.reorder(false);
                                    }
                                    return;
                                }
                                this.orderVersion = this.version;
                                this.scrollRow = Mth.clamp(this.scrollRow, 0, this.getMaxScrollRow());
                                this.finishInteractionSync();
                            },
                            this.screenExecutor
                        );
                    },
                    this.screenExecutor
                );
            },
            this.screenExecutor
        );
    }

    private CompletableFuture<List<StorageServerStub.SyncResult>> syncSlots(IntList slots) {
        List<CompletableFuture<StorageServerStub.SyncResult>> requests = new ArrayList<>();
        for (int start = 0; start < slots.size(); start += StorageScreen.VISIBLE_STORAGE_SLOTS) {
            int end = Math.min(start + StorageScreen.VISIBLE_STORAGE_SLOTS, slots.size());
            IntArrayList batch = new IntArrayList(end - start);
            for (int index = start; index < end; index++) {
                batch.add(slots.getInt(index));
            }
            requests.add(StorageClientStub.sync(this.sourcePos, batch));
        }
        if (requests.isEmpty()) {
            requests.add(StorageClientStub.sync(this.sourcePos, new IntArrayList()));
        }
        return CompletableFuture.allOf(requests.toArray(CompletableFuture<?>[]::new))
            .thenApply(_ -> requests.stream().map(CompletableFuture::join).toList());
    }

    private IntList getVisibleSlots(IntList order, int scrollRow) {
        int firstOrderIndex = scrollRow * StorageScreen.STORAGE_COLUMNS;
        int endOrderIndex = Math.min(firstOrderIndex + StorageScreen.VISIBLE_STORAGE_SLOTS, order.size());
        IntArrayList slots = new IntArrayList(endOrderIndex - firstOrderIndex);
        for (int orderIndex = firstOrderIndex; orderIndex < endOrderIndex; orderIndex++) {
            slots.add(order.getInt(orderIndex));
        }
        return slots;
    }

    private void applySyncResult(StorageServerStub.SyncResult result) {
        this.version = result.version();
        this.fullness = result.fullness();
        this.fluids = result.fluids();
        for (StorageServerStub.StackUpdate update : result.updates()) {
            if (update.stack().isEmpty()) {
                if (this.contents.containsKey(update.index())) {
                    // Keep the resource mapped to this logical slot while its current count is zero.
                    this.emptySlots.add(update.index());
                }
            } else {
                this.contents.put(update.index(), update.stack());
                this.emptySlots.remove(update.index());
            }
        }
    }

    private boolean applySyncResults(List<StorageServerStub.SyncResult> results) {
        if (this.hasInconsistentVersion(results)) {
            return false;
        }
        results.forEach(this::applySyncResult);
        return true;
    }

    private boolean applyReorderedSyncResults(List<StorageServerStub.SyncResult> results) {
        if (this.hasInconsistentVersion(results)) {
            return false;
        }
        this.contents.clear();
        this.emptySlots.clear();
        results.forEach(this::applySyncResult);
        return true;
    }

    private boolean applyPreservedSyncResults(List<StorageServerStub.SyncResult> results) {
        if (this.hasInconsistentVersion(results)) {
            return false;
        }

        Map<ItemResource, Integer> logicalSlots = new HashMap<>();
        for (int logicalSlot : this.order) {
            if (logicalSlot >= StorageScreen.FLUID_SLOT_BASE) continue;
            UnlimitedItemStack stack = this.contents.get(logicalSlot);
            logicalSlots.put(ItemResource.of(stack.toStack()), logicalSlot);
            this.emptySlots.add(logicalSlot);
        }
        this.serverSlots.clear();

        for (StorageServerStub.SyncResult result : results) {
            this.version = result.version();
            this.fullness = result.fullness();
            this.fluids = result.fluids();
            for (StorageServerStub.StackUpdate update : result.updates()) {
                if (update.stack().isEmpty()) {
                    continue;
                }
                ItemResource resource = ItemResource.of(update.stack().toStack());
                Integer logicalSlot = logicalSlots.get(resource);
                if (logicalSlot == null) {
                    logicalSlot = this.nextLogicalSlot++;
                    logicalSlots.put(resource, logicalSlot);
                    this.order.add(logicalSlot.intValue());
                }
                this.contents.put(logicalSlot.intValue(), update.stack());
                this.emptySlots.remove(logicalSlot.intValue());
                this.serverSlots.put(logicalSlot.intValue(), update.index());
            }
        }

        if (this.nbtFolded) {
            this.rebuildFoldedGroups(true);
        } else {
            this.displayOrder = this.applySearchFilter(new IntArrayList(this.order));
        }
        this.remappedOrder = true;
        return true;
    }

    private boolean hasInconsistentVersion(List<StorageServerStub.SyncResult> results) {
        long syncedVersion = results.getFirst().version();
        return syncedVersion < this.version
            || results.stream().anyMatch(result -> result.version() != syncedVersion);
    }

    private void resetServerSlots(IntList slots) {
        this.serverSlots.clear();
        this.nextLogicalSlot = 0;
        this.remappedOrder = false;
        for (int slot : slots) {
            if (slot >= StorageScreen.FLUID_SLOT_BASE) continue;
            this.serverSlots.put(slot, slot);
            this.nextLogicalSlot = Math.max(this.nextLogicalSlot, slot + 1);
        }
    }

    private boolean hasContents(IntList slots) {
        for (int slot : slots) {
            if (slot < StorageScreen.FLUID_SLOT_BASE && !this.contents.containsKey(slot)) {
                return false;
            }
        }
        return true;
    }

    private void finishInteractionSync() {
        if (this.interactionSyncPending) {
            this.interactionSyncPending = false;
            this.interactionPending = false;
        }
    }

    private void rebuildDisplayOrder(boolean foldNbt) {
        this.nbtFolded = foldNbt;
        this.foldedContents.clear();
        this.foldedCounts.clear();
        if (!foldNbt) {
            this.foldedGroups = List.of();
            this.displayOrder = this.applySearchFilter(new IntArrayList(this.order));
            return;
        }

        this.rebuildFoldedGroups(false);
    }

    private void rebuildFoldedGroups(boolean preserveRepresentatives) {
        List<IntList> groups = new ArrayList<>();
        Map<Item, IntList> groupsByItem = new HashMap<>();
        for (int slot : this.order) {
            UnlimitedItemStack stack = this.contents.getOrDefault(slot, UnlimitedItemStack.EMPTY);
            if (stack.isEmpty()) {
                continue;
            }
            IntList group = groupsByItem.get(stack.getItem());
            if (group == null) {
                group = new IntArrayList();
                groupsByItem.put(stack.getItem(), group);
                groups.add(group);
            }
            group.add(slot);
        }
        this.foldedGroups = groups;
        this.rebuildFoldedDisplay(preserveRepresentatives);
    }

    private void rebuildFoldedDisplay(boolean preserveRepresentatives) {
        IntArrayList foldedOrder = new IntArrayList(this.foldedGroups.size());
        this.foldedContents.clear();
        this.foldedCounts.clear();
        for (int groupIndex = 0; groupIndex < this.foldedGroups.size(); groupIndex++) {
            IntList group = this.foldedGroups.get(groupIndex);
            int representative = group.getInt(0);
            if (preserveRepresentatives && groupIndex < this.displayOrder.size()) {
                int previousRepresentative = this.displayOrder.getInt(groupIndex);
                if (group.contains(previousRepresentative)) {
                    representative = previousRepresentative;
                }
            }

            long count = 0;
            boolean foundNonEmpty = preserveRepresentatives && this.getStoredCount(representative) > 0;
            for (int slot : group) {
                int slotCount = this.getStoredCount(slot);
                count = Math.min(count + slotCount, Integer.MAX_VALUE);
                if (!foundNonEmpty && slotCount > 0) {
                    representative = slot;
                    foundNonEmpty = true;
                }
            }

            UnlimitedItemStack stack = Objects.requireNonNull(this.contents.get(representative));
            UnlimitedItemStack folded = stack.copy();
            int foldedCount = (int) count;
            folded.setCount(Math.max(foldedCount, 1));
            foldedOrder.add(representative);
            this.foldedContents.put(representative, folded);
            this.foldedCounts.put(representative, foldedCount);
        }
        this.displayOrder = this.applySearchFilter(this.appendFluidSlots(foldedOrder));
    }

    private UnlimitedItemStack getDisplayedStack(int slot) {
        Int2ObjectMap<UnlimitedItemStack> displayedContents = this.nbtFolded ? this.foldedContents : this.contents;
        return displayedContents.getOrDefault(slot, UnlimitedItemStack.EMPTY);
    }

    private int getDisplayedCount(int slot, UnlimitedItemStack stack) {
        return this.nbtFolded ? this.foldedCounts.get(slot) : this.emptySlots.contains(slot) ? 0 : stack.getCount();
    }

    private int getStoredCount(int slot) {
        UnlimitedItemStack stack = this.contents.getOrDefault(slot, UnlimitedItemStack.EMPTY);
        return this.emptySlots.contains(slot) ? 0 : stack.getCount();
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
                    if (this.preservingOrder) {
                        this.syncPreservedOrder();
                    } else {
                        this.reorder(false);
                    }
                } else if (metadata.version() != this.version) {
                    this.syncVisible();
                }
            },
            this.screenExecutor
        );
    }

    private int getMaxScrollRow() {
        return this.getMaxScrollRow(this.displayOrder);
    }

    private int getMaxScrollRow(IntList order) {
        return Math.max(
            0,
            Math.ceilDiv(order.size(), StorageScreen.STORAGE_COLUMNS) - StorageScreen.STORAGE_ROWS
        );
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void itemDecorations(
        GuiGraphicsExtractor graphic,
        Minecraft minecraft,
        ItemStack stack,
        int count,
        int x,
        int y
    ) {
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
        Component amount = Component.literal(FormattingUtil.toAbbrNum(count))
            .withStyle(style -> style.withFont(new FontDescription.Resource(StorageScreen.SMALL_FONT)));
        int color = count == 0 ? 0xFFFFAA00 : -1;
        StorageScreen.renderSlotCount(graphic, minecraft.font, amount, color, x, y);
        // endregion
        graphic.pose().popMatrix();
        ItemDecoratorHandler.of(stack).render(graphic, minecraft.font, stack, x, y);
    }
}
