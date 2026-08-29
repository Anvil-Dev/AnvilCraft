package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
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
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ItemDecoratorHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class StorageScreen extends Screen {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("misc", "storage_station");
    private static final ResourceLocation CAPACITY = SharedTextures.textureGui("misc/storage_station/capacity");
    private static final ResourceLocation SEARCH_CLEAR = SharedTextures.textureGui("misc/storage_station/search_clear");
    private static final ResourceLocation PUT = SharedTextures.textureGui("misc/storage_station/put");
    private static final ResourceLocation TAKE = SharedTextures.textureGui("misc/storage_station/take");
    private static final ResourceLocation SEARCH_RETENTION = SharedTextures.textureGui("misc/storage_station/search_retention");
    private static final ResourceLocation SORT_COUNT = SharedTextures.textureGui("misc/storage_station/sort_by_number");
    private static final ResourceLocation SORT_MOD = SharedTextures.textureGui("misc/storage_station/sort_by_mod");
    private static final ResourceLocation SORT_NAME = SharedTextures.textureGui("misc/storage_station/sort_by_name");
    private static final ResourceLocation SORT_COUNT_REVERSED = SharedTextures.textureGui("misc/storage_station/sort_by_number_reverse");
    private static final ResourceLocation SORT_NAME_REVERSED = SharedTextures.textureGui("misc/storage_station/sort_by_name_reverse");
    private static final ResourceLocation ORDER_SEQUENTIAL = SharedTextures.textureGui("misc/storage_station/sequential_order");
    private static final ResourceLocation ORDER_REVERSE = SharedTextures.textureGui("misc/storage_station/reverse_order");
    private static final ResourceLocation NBT_UNFOLD = SharedTextures.textureGui("misc/storage_station/nbt_unfold");
    private static final ResourceLocation NBT_FOLD = SharedTextures.textureGui("misc/storage_station/nbt_fold");
    private static final ResourceLocation SLIDER = SharedTextures.textureGui("misc/storage_station/slider_big");
    private static final ResourceLocation SMALL_FONT = ResourceLocation.fromNamespaceAndPath("anvilcraft", "small");
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
    private static final int MAX_PRESERVED_SYNC_ATTEMPTS = 3;
    private final Minecraft minecraft;
    private final BlockPos sourcePos;
    private final Player player;
    private final boolean tracksOpenState;

    private @Nullable EditBox search;
    private @Nullable CategoryList categories;

    private ItemStack carried = ItemStack.EMPTY;
    private IntList order = new IntArrayList();
    private IntList displayOrder = new IntArrayList();
    private final Int2ObjectMap<UnlimitedItemStack> contents = new Int2ObjectOpenHashMap<>();
    private final Int2LongMap counts = new Int2LongOpenHashMap();
    private final Int2ObjectMap<UnlimitedItemStack> foldedContents = new Int2ObjectOpenHashMap<>();
    private final Int2LongMap foldedCounts = new Int2LongOpenHashMap();
    private final Int2IntMap serverSlots = new Int2IntOpenHashMap();
    private final IntSet emptySlots = new IntOpenHashSet();
    private List<IntList> foldedGroups = List.of();
    private double fullness;
    private @Nullable StorageServerStub.Capacity capacity;
    private long version = -1;
    private long orderVersion = -1;
    private int scrollRow;
    private boolean draggingSlider;
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
    private final IntSet quickCraftStorageSlots = new IntOpenHashSet();
    private boolean quickCrafting;
    private int quickCraftingButton;
    private int lastClickedInventorySlot = -1;
    private boolean quickMoveDragging;
    private final IntSet quickMoveSlots = new IntOpenHashSet();
    private final IntSet pendingQuickMoveSlots = new IntOpenHashSet();
    private int left;
    private int top;
    private int titleLabelX;
    private @Nullable List<Component> renderingTooltips;

    public StorageScreen(BlockPos sourcePos) {
        this(
            sourcePos,
            Objects.requireNonNull(Minecraft.getInstance().level).getBlockState(sourcePos).getBlock().getName()
        );
    }

    public StorageScreen(BlockPos sourcePos, Component title) {
        super(title);
        this.minecraft = Minecraft.getInstance();
        this.sourcePos = sourcePos;
        this.player = Objects.requireNonNull(Minecraft.getInstance().player);
        this.serverSlots.defaultReturnValue(-1);
        this.tracksOpenState = Objects.requireNonNull(Minecraft.getInstance().level).getBlockState(sourcePos)
            .getBlock() instanceof ShulkerContainerBlock;
    }

    public static void openScreen(BlockPos sourcePos) {
        Minecraft.getInstance().setScreen(new StorageScreen(sourcePos));
    }

    public static void openScreen(BlockPos sourcePos, Component title) {
        Minecraft.getInstance().setScreen(new StorageScreen(sourcePos, title));
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
            (button, index) -> {
                SettingClientStub.update(SearchMode.values()[index]);
                this.reorder();
            }
        ));
        List<ResourceLocation> sortTextures = Lists.newArrayList(
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
            (button, index) -> {
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
            (button, index) -> {
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
            (button, index) -> {
                SettingClientStub.update(NbtDisplayMode.values()[index]);
                this.reorder();
            }
        ));
        this.categories = this.addRenderableWidget(new CategoryList(
            this.left + 7,
            this.top + 49,
            SettingClientStub.setting(),
            button -> SettingClientStub.update(SettingClientStub.listed().stream().toList())
                .thenRunAsync(this::reorder, this.screenExecutor),
            button -> this.minecraft.setScreen(new CategorySettingsScreen(this.sourcePos, this.title))
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
            button -> StorageClientStub.deposit(StorageScreen.this.sourcePos, Screen.hasShiftDown()).thenAcceptAsync(
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
            button -> StorageClientStub.take(StorageScreen.this.sourcePos).thenAcceptAsync(
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
        this.carried = this.player.inventoryMenu.getCarried();
        this.flushQuickMoves();
        if (this.metadataCooldown > 0) {
            this.metadataCooldown--;
        } else {
            this.refreshMetadata();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 仅画透明渐暗背景，跳过默认的高斯模糊（renderBlurredBackground），避免仓储界面背景模糊
        this.renderTransparentBackground(graphics);
        graphics.blit(
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
        this.renderStorageSlider(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltips = null;
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
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
        graphics.drawString(
            this.font,
            this.title,
            this.left + this.titleLabelX,
            this.top + Constant.SCREEN_TITLE_Y,
            0xFF404040,
            false
        );
        this.renderStorageContents(graphics, mouseX, mouseY);
        this.renderPlayerInventory(graphics, mouseX, mouseY);
        // 背景纹理必须先于 widgets 绘制，而 Screen.render 会二次调用 renderBackground
        // （半透明渐变会盖住纹理），故手动遍历 renderables 渲染 widgets。
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        this.renderCarriedItem(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderStorageContents(GuiGraphics graphics, int mouseX, int mouseY) {
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

            int slot = this.displayOrder.getInt(orderIndex);
            UnlimitedItemStack stack = this.getDisplayedStack(slot);

            if (!stack.isEmpty()) {
                ItemStack itemStack = stack.toStack();
                graphics.renderItem(itemStack, x, y);
                StorageScreen.renderItemDecorations(
                    graphics,
                    this.minecraft.font,
                    itemStack,
                    this.getDisplayedCount(slot),
                    x,
                    y
                );
            }
            if (hovered) {
                AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
            }
            if (hovered && this.carried.isEmpty() && !stack.isEmpty()) {
                List<Component> tooltipLines = new ArrayList<>(stack.toStack().getTooltipLines(
                    Item.TooltipContext.of(this.minecraft.level),
                    this.player,
                    this.minecraft.options.advancedItemTooltips
                        ? TooltipFlag.Default.ADVANCED
                        : TooltipFlag.Default.NORMAL
                ));
                long displayedCount = this.getDisplayedCount(slot);
                // 图标上的缩写仅在 >= 1000 时出现，此时 tooltip 才额外渲染精确数量
                if (displayedCount >= 1000) {
                    tooltipLines.add(Component.translatable("screen.anvilcraft.storage.count", displayedCount));
                }
                this.renderingTooltips = tooltipLines;
            }
        }
    }

    private void renderStorageSlider(GuiGraphics graphics) {
        int maxScrollRow = this.getMaxScrollRow();
        int sliderOffset = maxScrollRow == 0
            ? 0
            : Math.round(
                (StorageScreen.SLIDER_TRACK_HEIGHT - StorageScreen.SLIDER_HEIGHT)
                    * (float) this.scrollRow / maxScrollRow
            );
        graphics.blit(
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

    /** 鼠标是否位于滚动条轨道（含滑块）区域内。 */
    private boolean isOverSliderTrack(double mouseX, double mouseY) {
        int maxScrollRow = this.getMaxScrollRow();
        return maxScrollRow > 0
               && MathUtil.isInRange(
                   mouseX,
                   mouseY,
                   this.left + StorageScreen.SLIDER_X - 2,
                   this.top + StorageScreen.SLIDER_Y,
                   this.left + StorageScreen.SLIDER_X + StorageScreen.SLIDER_WIDTH + 2,
                   this.top + StorageScreen.SLIDER_Y + StorageScreen.SLIDER_TRACK_HEIGHT
               );
    }

    /** 按鼠标纵坐标把滚动条定位到对应行，并刷新可视内容。 */
    private void scrollSliderTo(double mouseY) {
        float trackTop = (float) (this.top + StorageScreen.SLIDER_Y);
        float usable = StorageScreen.SLIDER_TRACK_HEIGHT - StorageScreen.SLIDER_HEIGHT;
        float fraction = Mth.clamp((float) (mouseY - trackTop - StorageScreen.SLIDER_HEIGHT / 2.0F) / usable, 0.0F, 1.0F);
        int next = Math.round(fraction * this.getMaxScrollRow());
        if (next != this.scrollRow) {
            this.scrollRow = next;
            if (!this.nbtFolded) {
                this.syncVisible();
            }
        }
    }

    private void renderPlayerInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        Inventory inv = this.player.getInventory();

        int y = this.top + 140 + 58;
        for (int column = 0; column < 9; column++) {
            int x = this.left + 114 + 18 * column;
            this.renderInventorySlot(graphics, inv, column, x, y, mouseX, mouseY);
        }

        for (int row = 0; row < 3; row++) {
            y = this.top + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.left + 114 + 18 * column;
                this.renderInventorySlot(graphics, inv, slot++, x, y, mouseX, mouseY);
            }
        }
    }

    private void renderInventorySlot(GuiGraphics graphics, Inventory inv, int slot, int x, int y, int mouseX, int mouseY) {
        boolean hovered = MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17);

        ItemStack stack = inv.getItem(slot);
        boolean quickCraftPreview = this.quickCrafting && this.quickCraftSlots.contains(this.getScreenSlot(slot));
        if (quickCraftPreview) {
            stack = this.getQuickCraftPreviewStack(slot);
            graphics.fill(x, y, x + 16, y + 16, -2130706433);
        }
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(this.font, stack, x, y);
        }
        if (hovered) {
            AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
        }
        if (hovered && this.carried.isEmpty() && !stack.isEmpty()) {
            this.renderingTooltips = stack.getTooltipLines(
                Item.TooltipContext.of(this.minecraft.level),
                this.player,
                this.minecraft.options.advancedItemTooltips
                ? TooltipFlag.Default.ADVANCED
                : TooltipFlag.Default.NORMAL
            );
        }
    }

    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.renderingTooltips != null) {
            graphics.renderTooltip(this.font, this.renderingTooltips, Optional.empty(), mouseX, mouseY);
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 106, this.top, this.left + 300, this.top + 13)) {
            Component tooltip = this.getCapacityTooltip();
            if (tooltip != null) {
                graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            }
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 2, this.top + 23, this.left + 26, this.top + 43)) {
            graphics.renderTooltip(
                this.font,
                Component.translatable(
                    "screen.anvilcraft.storage.search",
                    SettingClientStub.setting().storage().getSearch().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 28, this.top + 23, this.left + 52, this.top + 43)) {
            graphics.renderTooltip(
                this.font,
                Component.translatable(
                    "screen.anvilcraft.storage.sort",
                    SettingClientStub.setting().storage().getSort().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 54, this.top + 23, this.left + 78, this.top + 43)) {
            graphics.renderTooltip(
                this.font,
                Component.translatable(
                    "screen.anvilcraft.storage.order",
                    SettingClientStub.setting().storage().getOrder().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.left + 80, this.top + 23, this.left + 104, this.top + 43)) {
            graphics.renderTooltip(
                this.font,
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
        // 空间与类型都无限（超维存储站）显示 Infinity Storage
        if (capacity.spaceSize() == Integer.MAX_VALUE && capacity.typeLimit() == Integer.MAX_VALUE) {
            return Component.translatable("screen.anvilcraft.storage.capacity.infinity");
        }
        // 有类型上限（潜影集装箱）按类型数显示
        if (capacity.typeLimit() != Integer.MAX_VALUE) {
            return Component.translatable("screen.anvilcraft.storage.capacity.types", capacity.typeCount(), capacity.typeLimit());
        }
        // 有限空间的板条箱按空间显示
        return Component.translatable("screen.anvilcraft.storage.capacity.space", capacity.space(), capacity.spaceSize());
    }

    private void renderCarriedItem(GuiGraphics graphics, int mouseX, int mouseY) {
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
        // 鼠标物品输出在所有槽位内容之上（高亮/槽位物品/数量文字），仍低于tooltip
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        graphics.renderItem(renderedCarried, mouseX - 8, mouseY - 8);
        graphics.renderItemDecorations(this.font, renderedCarried, mouseX - 8, mouseY - 8);
        graphics.pose().popPose();
    }

    private ItemStack getQuickCraftPreviewStack(int inventorySlot) {
        Slot slot = this.player.inventoryMenu.getSlot(this.getScreenSlot(inventorySlot));
        int currentCount = slot.hasItem() ? slot.getItem().getCount() : 0;
        int maxCount = Math.min(this.carried.getMaxStackSize(), slot.getMaxStackSize(this.carried));
        int placedCount = AbstractContainerMenu.getQuickCraftPlaceCount(
            this.getQuickCraftSlotSet(),
            this.quickCraftingButton,
            this.carried
        );
        return this.carried.copyWithCount(Math.min(currentCount + placedCount, maxCount));
    }

    private int getQuickCraftRemaining() {
        if (this.quickCraftingButton == 2) {
            return this.carried.getCount();
        }
        int remaining = this.carried.getCount();
        for (int screenSlot : this.quickCraftSlots) {
            Slot slot = this.player.inventoryMenu.getSlot(screenSlot);
            int currentCount = slot.hasItem() ? slot.getItem().getCount() : 0;
            int maxCount = Math.min(this.carried.getMaxStackSize(), slot.getMaxStackSize(this.carried));
            int placedCount = AbstractContainerMenu.getQuickCraftPlaceCount(
                this.getQuickCraftSlotSet(),
                this.quickCraftingButton,
                this.carried
            );
            remaining -= Math.min(placedCount, maxCount - currentCount);
        }
        return Math.max(0, remaining);
    }

    private Set<Slot> getQuickCraftSlotSet() {
        Set<Slot> slots = new HashSet<>();
        for (int screenSlot : this.quickCraftSlots) {
            slots.add(this.player.inventoryMenu.getSlot(screenSlot));
        }
        return slots;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int lastClickedInventorySlot = this.lastClickedInventorySlot;
        this.lastClickedInventorySlot = -1;
        if (this.search != null && (button == 0 || button == 1)) {
            boolean hovered = MathUtil.isInRange(mouseX, mouseY, this.left + 6, this.top + 6, this.left + 100, this.top + 16);
            this.search.setFocused(hovered);
            this.setFocused(hovered ? this.search : null);
        }

        // 左键按住滚动条：进入拖动状态，并按点击位置立即定位
        if (button == 0 && this.isOverSliderTrack(mouseX, mouseY)) {
            this.draggingSlider = true;
            this.scrollSliderTo(mouseY);
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 || button == 1) {
            Integer storageSlot = this.getStorageSlot(mouseX, mouseY);
            if (storageSlot != null && this.minecraft.gameMode != null) {
                StorageInput action = Screen.hasShiftDown()
                                      ? StorageInput.QUICK_MOVE_FROM_STORAGE
                                      : StorageInput.PICKUP;
                this.interactWithStorage(storageSlot, button, action);
                return true;
            }

            int slot = this.getInventorySlot(mouseX, mouseY);
            if (slot == -1) {
                if (this.minecraft.gameMode != null && !this.carried.isEmpty()) {
                    this.player.inventoryMenu.setCarried(this.carried);
                    this.minecraft.gameMode.handleInventoryMouseClick(
                        this.player.inventoryMenu.containerId,
                        -999,
                        button,
                        ClickType.PICKUP,
                        this.player
                    );
                    this.carried = this.player.inventoryMenu.getCarried();
                    return true;
                }
                return false;
            }
            if (this.minecraft.gameMode == null) {
                return false;
            }
            this.lastClickedInventorySlot = slot;

            if (Screen.hasAltDown()) {
                this.moveSameToStorage(slot);
                return true;
            }

            if (Screen.hasShiftDown()) {
                if (this.carried.isEmpty()) {
                    this.quickMoveDragging = true;
                    StorageClientStub.beginUndoGroup(this.sourcePos);
                    this.queueQuickMove(slot);
                } else {
                    this.interactWithStorage(slot, button, StorageInput.QUICK_MOVE_TO_STORAGE);
                }
                return true;
            }

            this.carried = this.player.inventoryMenu.getCarried();
            if (button == 0 && this.isDoubleClick(slot, button) && slot == lastClickedInventorySlot) {
                if (this.carried.isEmpty()) {
                    this.player.inventoryMenu.setCarried(this.carried);
                    this.minecraft.gameMode.handleInventoryMouseClick(
                        this.player.inventoryMenu.containerId,
                        this.getScreenSlot(slot),
                        button,
                        ClickType.PICKUP,
                        this.player
                    );
                    this.carried = this.player.inventoryMenu.getCarried();
                }
                this.doubleclick = true;
                return true;
            }
            if (!this.carried.isEmpty()) {
                this.startQuickCraft(button);
                return true;
            }

            this.player.inventoryMenu.setCarried(this.carried);
            this.minecraft.gameMode.handleInventoryMouseClick(
                this.player.inventoryMenu.containerId,
                this.getScreenSlot(slot),
                button,
                ClickType.PICKUP,
                this.player
            );
            this.carried = this.player.inventoryMenu.getCarried();
            return true;
        } else if (button == 2) {
            Integer storageSlot = this.getStorageSlot(mouseX, mouseY);
            if (
                storageSlot != null
                && this.player.hasInfiniteMaterials()
                && this.minecraft.options.keyPickItem.matchesMouse(2)
            ) {
                if (this.carried.isEmpty()) {
                    this.interactWithStorage(storageSlot, 0, StorageInput.CLONE);
                } else {
                    this.startQuickCraft(button);
                }
                return true;
            }

            int slot = this.getScreenSlot();
            if (slot == -1 || this.minecraft.gameMode == null) {
                return false;
            }

            if (!this.minecraft.options.keyPickItem.matchesMouse(2)) {
                return false;
            }

            if (this.carried.isEmpty()) {
                this.minecraft.gameMode.handleInventoryMouseClick(
                    this.player.inventoryMenu.containerId,
                    slot,
                    0,
                    ClickType.CLONE,
                    this.player
                );
                this.carried = this.player.inventoryMenu.getCarried();
            } else {
                this.startQuickCraft(button);
            }
            return true;
        }

        return false;
    }

    private long lastClickTime;
    private int lastClickSlot = -1;
    private int lastClickButton = -1;
    private boolean doubleclick;

    private boolean isDoubleClick(int slot, int button) {
        final boolean quick = slot == this.lastClickSlot
            && System.currentTimeMillis() - this.lastClickTime < 250L
            && button == this.lastClickButton;
        this.lastClickSlot = slot;
        this.lastClickTime = System.currentTimeMillis();
        this.lastClickButton = button;
        return quick;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingSlider) {
            if (button == 0) {
                this.scrollSliderTo(mouseY);
            }
            return true;
        }
        if (this.quickMoveDragging) {
            if (button == 0 && Screen.hasShiftDown()) {
                int inventorySlot = this.getInventorySlot(mouseX, mouseY);
                if (inventorySlot != -1) {
                    this.queueQuickMove(inventorySlot);
                }
            }
            return true;
        }
        if (!this.quickCrafting || button != this.quickCraftingButton || this.carried.isEmpty()) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        Integer storageSlot = this.getStorageSlot(mouseX, mouseY);
        if (storageSlot != null) {
            if (button == 2 && this.player.hasInfiniteMaterials()) {
                this.quickCraftStorageSlots.add(storageSlot.intValue());
            }
            return true;
        }

        int inventorySlot = this.getInventorySlot(mouseX, mouseY);
        if (inventorySlot != -1) {
            int screenSlot = this.getScreenSlot(inventorySlot);
            Slot slot = this.player.inventoryMenu.getSlot(screenSlot);
            if (
                (this.carried.getCount() > this.quickCraftSlots.size() || button == 2)
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (this.draggingSlider) {
            this.draggingSlider = false;
            return true;
        }
        if (this.quickMoveDragging) {
            this.quickMoveDragging = false;
            this.quickMoveSlots.clear();
            this.flushQuickMoves();
            StorageClientStub.endUndoGroup(this.sourcePos);
            return true;
        }
        if (this.doubleclick) {
            this.doubleclick = false;
            this.lastClickTime = 0L;
            if (button == 0 && this.minecraft.gameMode != null) {
                int inventorySlot = this.getInventorySlot(mouseX, mouseY);
                if (inventorySlot == this.lastClickedInventorySlot) {
                    this.player.inventoryMenu.setCarried(this.carried);
                    this.minecraft.gameMode.handleInventoryMouseClick(
                        this.player.inventoryMenu.containerId,
                        this.getScreenSlot(inventorySlot),
                        0,
                        ClickType.PICKUP_ALL,
                        this.player
                    );
                    this.carried = this.player.inventoryMenu.getCarried();
                }
            }
            return true;
        }
        if (!this.quickCrafting) {
            return false;
        }

        if (button == this.quickCraftingButton && this.minecraft.gameMode != null) {
            this.player.inventoryMenu.setCarried(this.carried);
            if (this.quickCraftSlots.isEmpty() && this.quickCraftStorageSlots.isEmpty()) {
                int inventorySlot = this.getInventorySlot(mouseX, mouseY);
                if (inventorySlot != -1) {
                    this.minecraft.gameMode.handleInventoryMouseClick(
                        this.player.inventoryMenu.containerId,
                        this.getScreenSlot(inventorySlot),
                        button,
                        ClickType.PICKUP,
                        this.player
                    );
                }
            } else {
                if (!this.quickCraftStorageSlots.isEmpty()) {
                    this.clonePutToStorage();
                }
                if (!this.quickCraftSlots.isEmpty()) {
                    this.quickCraftToSlots(button);
                }
            }
            this.carried = this.player.inventoryMenu.getCarried();
            if (this.carried.isEmpty()) {
                this.lastClickTime = 0L;
            }
        }

        this.quickCrafting = false;
        this.quickCraftSlots.clear();
        this.quickCraftStorageSlots.clear();
        return true;
    }

    private void startQuickCraft(int button) {
        this.quickCrafting = true;
        this.quickCraftingButton = button;
        this.quickCraftSlots.clear();
        this.quickCraftStorageSlots.clear();
    }

    private void clonePutToStorage() {
        if (this.quickCraftStorageSlots.isEmpty()) {
            return;
        }
        IntList slots = new IntArrayList(this.quickCraftStorageSlots);
        StorageClientStub.clonePut(this.sourcePos, slots).whenCompleteAsync(
            (changed, error) -> {
                if (error != null || !changed) {
                    return;
                }
                if (this.preservingOrder) {
                    this.interactionSyncPending = true;
                    this.syncPreservedOrder();
                    return;
                }
                this.reorder(false);
            },
            this.screenExecutor
        );
    }

    private void queueQuickMove(int slot) {
        if (this.quickMoveSlots.add(slot)) {
            this.pendingQuickMoveSlots.add(slot);
        }
    }

    private void flushQuickMoves() {
        if (this.pendingQuickMoveSlots.isEmpty()) {
            return;
        }
        IntList slots = new IntArrayList(this.pendingQuickMoveSlots);
        this.pendingQuickMoveSlots.clear();
        StorageClientStub.quickMoveToStorage(this.sourcePos, slots).whenCompleteAsync(
            (changed, error) -> {
                if (error != null || !changed) {
                    return;
                }
                if (this.preservingOrder) {
                    this.interactionSyncPending = true;
                    this.syncPreservedOrder();
                    return;
                }
                this.reorder(false);
            },
            this.screenExecutor
        );
    }

    private void moveSameToStorage(int slot) {
        StorageClientStub.moveSameToStorage(this.sourcePos, slot).whenCompleteAsync(
            (changed, error) -> {
                if (error != null || !changed) {
                    return;
                }
                if (this.preservingOrder) {
                    this.interactionSyncPending = true;
                    this.syncPreservedOrder();
                    return;
                }
                this.reorder(false);
            },
            this.screenExecutor
        );
    }

    private void undoLastMove() {
        StorageClientStub.undo(this.sourcePos).whenCompleteAsync(
            (result, error) -> {
                if (error != null || !result.changed()) {
                    return;
                }
                if (this.preservingOrder) {
                    this.interactionSyncPending = true;
                    this.syncPreservedOrder();
                    return;
                }
                this.reorder(false);
            },
            this.screenExecutor
        );
    }

    private void quickCraftToSlots(int button) {
        if (this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleInventoryMouseClick(
            this.player.inventoryMenu.containerId,
            -999,
            AbstractContainerMenu.getQuickcraftMask(0, button),
            ClickType.QUICK_CRAFT,
            this.player
        );
        for (int slot : this.quickCraftSlots) {
            this.minecraft.gameMode.handleInventoryMouseClick(
                this.player.inventoryMenu.containerId,
                slot,
                AbstractContainerMenu.getQuickcraftMask(1, button),
                ClickType.QUICK_CRAFT,
                this.player
            );
        }
        this.minecraft.gameMode.handleInventoryMouseClick(
            this.player.inventoryMenu.containerId,
            -999,
            AbstractContainerMenu.getQuickcraftMask(2, button),
            ClickType.QUICK_CRAFT,
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
        int serverSlot = action == StorageInput.QUICK_MOVE_TO_STORAGE ? slot : this.serverSlots.get(slot);
        StorageClientStub.interact(this.sourcePos, serverSlot, button, action).whenCompleteAsync(
            (result, error) -> {
                if (request != this.interactionRequest || error != null) {
                    this.interactionPending = false;
                    return;
                }
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (
            (keyCode == InputConstants.KEY_LSHIFT || keyCode == InputConstants.KEY_RSHIFT)
            && !this.preservingOrder
        ) {
            this.preservingOrder = true;
            this.reorderRequest++;
            this.syncRequest++;
        }

        if (this.search != null && this.search.isFocused()) {
            this.search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (Screen.hasControlDown() && keyCode == InputConstants.KEY_Z) {
            this.undoLastMove();
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
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
                    int dropMode = Screen.hasControlDown() ? Screen.hasShiftDown() ? 2 : 1 : 0;
                    this.interactWithStorage(storageSlot, dropMode, StorageInput.THROW);
                    return true;
                }
            }

            int hoveredSlot = this.getInventorySlot();
            if (hoveredSlot == -1 || this.minecraft.gameMode == null) {
                return false;
            }

            // Forge MC-146650: Needs to return true when the key is handled
            boolean handled = this.checkHotbarKeyPressed(keyCode, scanCode);
            if (!Objects.requireNonNull(this.minecraft.player).getInventory().getItem(hoveredSlot).isEmpty()) {
                hoveredSlot = this.getScreenSlot(hoveredSlot);
                if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
                    this.minecraft.gameMode.handleInventoryMouseClick(
                        this.player.inventoryMenu.containerId,
                        hoveredSlot,
                        Screen.hasControlDown() ? 1 : 0,
                        ClickType.THROW,
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
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (
            (keyCode == InputConstants.KEY_LSHIFT || keyCode == InputConstants.KEY_RSHIFT)
            && !Screen.hasShiftDown()
            && this.preservingOrder
        ) {
            this.preservingOrder = false;
            this.reorder(false);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        int hoveredSlot = this.getScreenSlot();
        if (hoveredSlot == -1 || this.minecraft.gameMode == null) {
            return false;
        }

        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (this.carried.isEmpty()) {
            if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(key)) {
                this.minecraft.gameMode.handleInventoryMouseClick(
                    this.player.inventoryMenu.containerId,
                    hoveredSlot,
                    40,
                    ClickType.SWAP,
                    this.player
                );
                return true;
            }
            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].isActiveAndMatches(key)) {
                    this.minecraft.gameMode.handleInventoryMouseClick(
                        this.player.inventoryMenu.containerId,
                        hoveredSlot,
                        i,
                        ClickType.SWAP,
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
                    this.minecraft.gameMode.handleInventoryMouseClick(
                        this.player.inventoryMenu.containerId,
                        -999,
                        0,
                        ClickType.PICKUP,
                        this.player
                    );
                    break;
                }

                this.minecraft.gameMode.handleInventoryMouseClick(
                    this.player.inventoryMenu.containerId,
                    slot < 9 ? slot + 36 : slot,
                    0,
                    ClickType.PICKUP,
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
                    return this.displayOrder.getInt(orderIndex);
                }
                return this.carried.isEmpty() ? null : -1;
            }
        }
        return null;
    }

    private @Nullable Integer getStorageSlot() {
        return this.getStorageSlot(this.getMouseScaledX(), this.getMouseScaledY());
    }

    private double getMouseScaledX() {
        return this.minecraft.mouseHandler.xpos() / this.minecraft.getWindow().getGuiScale();
    }

    private double getMouseScaledY() {
        return this.minecraft.mouseHandler.ypos() / this.minecraft.getWindow().getGuiScale();
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
        return this.getInventorySlot(this.getMouseScaledX(), this.getMouseScaledY());
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
            .thenApply(ignored -> requests.stream().map(CompletableFuture::join).toList());
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
        for (StorageServerStub.StackUpdate update : result.updates()) {
            if (update.stack().isEmpty()) {
                if (this.contents.containsKey(update.index())) {
                    // Keep the resource mapped to this logical slot while its current count is zero.
                    this.emptySlots.add(update.index());
                }
            } else {
                this.contents.put(update.index(), update.stack());
                this.counts.put(update.index(), update.count());
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
        this.counts.clear();
        this.emptySlots.clear();
        results.forEach(this::applySyncResult);
        return true;
    }

    private boolean applyPreservedSyncResults(List<StorageServerStub.SyncResult> results) {
        if (this.hasInconsistentVersion(results)) {
            return false;
        }

        // 以物品+数据组件为键把服务端最新槽位重映射回已锁定的逻辑槽位（忽略数量），
        // 避免同物品的不同组件堆相互覆盖：数量、渲染与 serverSlots 各自保持独立。
        Map<UnlimitedItemStacksResourceHandler.ResourceKey, Integer> logicalSlots = new HashMap<>();
        for (int logicalSlot : this.order) {
            UnlimitedItemStack stack = this.contents.get(logicalSlot);
            logicalSlots.put(
                UnlimitedItemStacksResourceHandler.ResourceKey.of(stack.toStack()),
                logicalSlot
            );
            this.emptySlots.add(logicalSlot);
        }
        this.serverSlots.clear();

        for (StorageServerStub.SyncResult result : results) {
            this.version = result.version();
            this.fullness = result.fullness();
            for (StorageServerStub.StackUpdate update : result.updates()) {
                if (update.stack().isEmpty()) {
                    continue;
                }
                UnlimitedItemStacksResourceHandler.ResourceKey key =
                    UnlimitedItemStacksResourceHandler.ResourceKey.of(update.stack().toStack());
                Integer logicalSlot = logicalSlots.get(key);
                if (logicalSlot == null) {
                    logicalSlot = this.allocateLogicalSlot();
                    logicalSlots.put(key, logicalSlot);
                    this.order.add(logicalSlot.intValue());
                }
                this.contents.put(logicalSlot.intValue(), update.stack());
                this.counts.put(logicalSlot.intValue(), update.count());
                this.emptySlots.remove(logicalSlot.intValue());
                this.serverSlots.put(logicalSlot.intValue(), update.index());
            }
        }

        if (this.nbtFolded) {
            this.rebuildFoldedGroups(true);
        } else {
            this.displayOrder = new IntArrayList(this.order);
        }
        this.remappedOrder = true;
        return true;
    }

    private int allocateLogicalSlot() {
        int logicalSlot;
        do {
            logicalSlot = this.nextLogicalSlot++;
        } while (this.order.contains(logicalSlot) || this.contents.containsKey(logicalSlot));
        return logicalSlot;
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
            this.serverSlots.put(slot, slot);
            this.nextLogicalSlot = Math.max(this.nextLogicalSlot, slot + 1);
        }
    }

    private boolean hasContents(IntList slots) {
        for (int slot : slots) {
            if (!this.contents.containsKey(slot)) {
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
            this.displayOrder = new IntArrayList(this.order);
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
                long slotCount = this.getStoredCount(slot);
                count += slotCount;
                if (!foundNonEmpty && slotCount > 0) {
                    representative = slot;
                    foundNonEmpty = true;
                }
            }

            UnlimitedItemStack stack = Objects.requireNonNull(this.contents.get(representative));
            UnlimitedItemStack folded = stack.copy();
            // 图标栈仅用于渲染物品与判定非空，数量截断到 int 上限；真实总量存于 foldedCounts
            folded.setCount(Math.clamp(count, 1, Integer.MAX_VALUE));
            foldedOrder.add(representative);
            this.foldedContents.put(representative, folded);
            this.foldedCounts.put(representative, count);
        }
        this.displayOrder = foldedOrder;
    }

    private UnlimitedItemStack getDisplayedStack(int slot) {
        Int2ObjectMap<UnlimitedItemStack> displayedContents = this.nbtFolded ? this.foldedContents : this.contents;
        return displayedContents.getOrDefault(slot, UnlimitedItemStack.EMPTY);
    }

    private long getDisplayedCount(int slot) {
        return this.nbtFolded ? this.foldedCounts.get(slot) : this.getStoredCount(slot);
    }

    private long getStoredCount(int slot) {
        UnlimitedItemStack stack = this.contents.getOrDefault(slot, UnlimitedItemStack.EMPTY);
        if (stack.isEmpty() || this.emptySlots.contains(slot)) {
            return 0;
        }
        return this.counts.getOrDefault(slot, 0);
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

    public static void renderItemDecorations(
        GuiGraphics graphics,
        Font font,
        ItemStack stack,
        long count,
        int x,
        int y
    ) {
        if (stack.isEmpty()) {
            return;
        }

        PoseStack pose = graphics.pose();
        // 抬高 z 使耐久条与数量数字绘制在物品图标之上
        pose.pushPose();
        pose.translate(0, 0, 200);

        // 耐久条
        if (stack.isBarVisible()) {
            int left = x + 2;
            int top = y + 13;
            graphics.fill(RenderType.guiOverlay(), left, top, left + 13, top + 2, 0xFF000000);
            graphics.fill(RenderType.guiOverlay(), left, top, left + stack.getBarWidth(), top + 1, stack.getBarColor() | 0xFF000000);
        }

        // 数量（使用缩写格式，可超过 999）
        pose.translate(x + 17, y + 9, 0);
        Component amount = Component.literal(FormattingUtil.toAbbrNum(count))
            .withStyle(style -> style.withFont(StorageScreen.SMALL_FONT));
        int color = count == 0 ? 0xFFFFAA00 : -1;
        int width = font.width(amount);
        if (width > 16) {
            pose.scale(0.75F, 0.75F, 1);
            pose.translate(-1F, font.lineHeight * 0.25F - 0.25F, 0);
        }
        graphics.drawString(font, amount, -width, 0, color, true);

        pose.popPose();

        // noinspection UnstableApiUsage
        ItemDecoratorHandler.of(stack).render(graphics, font, stack, x, y);
    }
}
