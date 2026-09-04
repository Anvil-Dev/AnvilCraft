package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.client.support.GuiRenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.integration.StorageJeiBridge;
import dev.dubhe.anvilcraft.rpc.StorageInput;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
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
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ItemDecoratorHandler;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final ResourceLocation CAPACITY = StorageScreen.texture("capacity");
    private static final ResourceLocation SEARCH_CLEAR = StorageScreen.texture("search_clear");
    private static final ResourceLocation PUT = StorageScreen.texture("put");
    private static final ResourceLocation TAKE = StorageScreen.texture("take");
    private static final ResourceLocation CRAFTING = StorageScreen.texture("crafting");
    private static final ResourceLocation CRAFTING_AUTO_FILL_OFF = StorageScreen.texture("crafting_auto_fill_off");
    private static final ResourceLocation CRAFTING_AUTO_FILL_ON = StorageScreen.texture("crafting_auto_fill_on");
    private static final ResourceLocation CRAFTING_CLEAR = StorageScreen.texture("crafting_clear");
    private static final ResourceLocation CRAFTING_TO_PLAYER = StorageScreen.texture("crafting_to_player");
    private static final ResourceLocation CRAFTING_TO_STORAGE = StorageScreen.texture("crafting_to_storage");
    private static final ResourceLocation SEARCH_RETENTION = StorageScreen.texture("search_retention");
    private static final ResourceLocation SORT_COUNT = StorageScreen.texture("sort_by_number");
    private static final ResourceLocation SORT_MOD = StorageScreen.texture("sort_by_mod");
    private static final ResourceLocation SORT_NAME = StorageScreen.texture("sort_by_name");
    private static final ResourceLocation SORT_COUNT_REVERSED = StorageScreen.texture("sort_by_number_reverse");
    private static final ResourceLocation SORT_NAME_REVERSED = StorageScreen.texture("sort_by_name_reverse");
    private static final ResourceLocation ORDER_SEQUENTIAL = StorageScreen.texture("sequential_order");
    private static final ResourceLocation ORDER_REVERSE = StorageScreen.texture("reverse_order");
    private static final ResourceLocation NBT_UNFOLD = StorageScreen.texture("nbt_unfold");
    private static final ResourceLocation NBT_FOLD = StorageScreen.texture("nbt_fold");
    private static final ResourceLocation SLIDER = StorageScreen.texture("slider_big");
    private static final ResourceLocation FLYOUT_BACK = AnvilCraft.of("flex_button/shaded_1px");
    private static final ResourceLocation FLYOUT_POINTER = AnvilCraft.of("flex_button/pointer");
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
    /** ① 切石机输入槽（单槽）的左上角。 */
    private static final int CRAFTING_STONECUTTER_X = 7;
    private static final int CRAFTING_STONECUTTER_Y = 130;
    /** ② 合成输入 9 宫格的左上角（3×3，18px 间距）。 */
    private static final int CRAFTING_GRID_X = 7;
    private static final int CRAFTING_GRID_Y = 162;
    /** ③ 切石机结果槽。 */
    private static final int CRAFTING_RESULT_STONECUTTER_X = 83;
    private static final int CRAFTING_RESULT_STONECUTTER_Y = 162;
    /** ④ 合成结果槽。 */
    private static final int CRAFTING_RESULT_CRAFTING_X = 83;
    private static final int CRAFTING_RESULT_CRAFTING_Y = 198;
    /** 切石机配方选择面板：第一个按钮左上角，与批量切割机一致的行列排布（3 列 × 2 行，18px 间距）。 */
    private static final int CRAFTING_RECIPE_X = 39;
    private static final int CRAFTING_RECIPE_Y = 120;
    private static final int CRAFTING_RECIPE_COLUMNS = 3;
    private static final int CRAFTING_RECIPE_ROWS = 2;
    private static final int CRAFTING_SLOT_SIZE = 18;
    /**
     * 连续合成（Shift 点击结果槽）客户端最多请求的分块数。
     * 每个分块最多合成 {@code CRAFTING_TAKE_ALL_CHUNK} 次，总上限为
     * 64 × 64 = 4096 次；仅作为异常配方下的防御性兜底。
     */
    private static final int MAX_TAKE_ALL_CHUNKS = 64;
    /** 缺失工作台/切石机提示浮窗：0.25s 淡入 + 1.25s 停留 + 0.25s 淡出。 */
    private static final int FLYOUT_FADE_IN_TICKS = 5;
    private static final int FLYOUT_HOLD_TICKS = 25;
    private static final int FLYOUT_FADE_OUT_TICKS = 5;
    private final Minecraft minecraft;
    @Getter
    private final BlockPos sourcePos;
    private final Player player;
    private final boolean tracksOpenState;

    private @Nullable EditBox search;
    private @Nullable CategoryList categories;

    private ScreenMode mode = ScreenMode.NORMAL;
    private ItemStack carried = ItemStack.EMPTY;
    private IntList order = new IntArrayList();
    private IntList displayOrder = new IntArrayList();
    @Getter
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
    /** 存储列表滚动（与分类栏/配方滑条一致的连续 0..1 偏移）。 */
    private final Scrollable storageScrollable = new Scrollable() {
        @Override
        public int row() {
            return StorageScreen.STORAGE_ROWS;
        }

        @Override
        public int column() {
            return StorageScreen.STORAGE_COLUMNS;
        }

        @Override
        public int size() {
            return StorageScreen.this.displayOrder.size();
        }

        @Override
        public void setHead(int head) {
            int next = Mth.clamp(head / StorageScreen.STORAGE_COLUMNS, 0, StorageScreen.this.getMaxScrollRow());
            if (next != StorageScreen.this.scrollRow) {
                StorageScreen.this.scrollRow = next;
                if (!StorageScreen.this.nbtFolded) {
                    StorageScreen.this.syncVisible();
                }
            }
        }
    };
    private boolean draggingSlider;
    private int reorderRequest;
    private int syncRequest;
    private int interactionRequest;
    private int metadataCooldown;
    private boolean orderLoaded;
    private boolean metadataPending;
    private boolean interactionPending;
    private boolean interactionSyncPending;
    /** 上次播放切石机取走音效的游戏 tick（与方块侧一致，同一 tick 只播一次）。 */
    private long lastStonecutterTakeSoundTick = -1;
    private boolean closed;
    private boolean nbtFolded;
    private boolean preservingOrder;
    private boolean remappedOrder;
    private int nextLogicalSlot;
    private final IntSet quickCraftSlots = new IntOpenHashSet();
    private final IntSet quickCraftStorageSlots = new IntOpenHashSet();
    /** 拖拽分配目标：①/② 合成输入槽（0 为①，1~9 为②）。 */
    private final IntSet quickCraftCraftingSlots = new IntOpenHashSet();
    /** 拖拽分配目标：玩家背包槽（inventory index 0~35，与 quickCraftSlots 同步）。 */
    private final IntSet quickCraftInventorySlots = new IntOpenHashSet();
    private boolean quickCrafting;
    private int quickCraftingButton;
    private int lastClickedInventorySlot = -1;
    @Getter
    private boolean quickMoveDragging;
    private final IntSet quickMoveSlots = new IntOpenHashSet();
    private final IntSet pendingQuickMoveSlots = new IntOpenHashSet();
    private final IntSet storageQuickMoveSlots = new IntOpenHashSet();
    private final Int2ObjectMap<IntList> quickMoveMovedBySlot = new Int2ObjectOpenHashMap<>();
    private @Nullable List<Component> renderingTooltips;
    private boolean craftingAvailable;
    private boolean craftingLoaded;
    @Getter
    private CraftingStorage crafting = CraftingStorage.EMPTY;
    private @Nullable SwitchableButton craftingAutoFillButton;
    private @Nullable SwitchableButton craftingToStorageButton;
    private @Nullable TexturedButton craftingClearButton;
    private List<ItemStack> stonecutterRecipes = List.of();
    /** 切石机配方列表当前页首项索引（3 列 × 2 行，超出可滚动）。 */
    private int recipeHead;
    private final Scrollable recipeScrollable = new Scrollable() {
        @Override
        public int row() {
            return StorageScreen.CRAFTING_RECIPE_ROWS;
        }

        @Override
        public int column() {
            return StorageScreen.CRAFTING_RECIPE_COLUMNS;
        }

        @Override
        public int size() {
            return StorageScreen.this.stonecutterRecipes.size();
        }

        @Override
        public void setHead(int head) {
            StorageScreen.this.recipeHead = head;
        }
    };
    private int flyoutTimer;
    private boolean flyoutVisible;

    public StorageScreen(BlockPos sourcePos) {
        this(
            sourcePos,
            Objects.requireNonNull(Minecraft.getInstance().level).getBlockState(sourcePos).getBlock().getName()
        );
    }

    public StorageScreen(BlockPos sourcePos, Component title) {
        super(
            StorageMenu.create(Objects.requireNonNull(Minecraft.getInstance().player), sourcePos),
            Objects.requireNonNull(Minecraft.getInstance().player).getInventory(),
            title
        );
        this.imageWidth = StorageScreen.BG_WIDTH;
        this.imageHeight = StorageScreen.BG_HEIGHT;
        this.minecraft = Minecraft.getInstance();
        this.sourcePos = sourcePos;
        this.player = Objects.requireNonNull(Minecraft.getInstance().player);
        this.serverSlots.defaultReturnValue(-1);
        this.tracksOpenState = Objects.requireNonNull(Minecraft.getInstance().level).getBlockState(sourcePos)
            .getBlock() instanceof ShulkerContainerBlock;
    }

    public static void openScreen(BlockPos sourcePos) {
        StorageScreen.openScreen(sourcePos, null);
    }

    public static void openScreen(BlockPos sourcePos, @Nullable Component title) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        StorageScreen screen = title == null
            ? new StorageScreen(sourcePos)
            : new StorageScreen(sourcePos, title);
        // 纯客户端菜单：安装为活动菜单（服务端容器仍为 inventoryMenu，RPC 指针读写不受影响）
        minecraft.player.containerMenu = screen.getMenu();
        minecraft.setScreen(screen);
    }

    @Override
    protected void init() {
        if (this.tracksOpenState) {
            StorageClientStub.setOpen(this.sourcePos, true);
        }
        this.leftPos = (this.width - StorageScreen.BG_WIDTH) / 2;
        this.topPos = (this.height - StorageScreen.BG_HEIGHT) / 2;
        this.titleLabelX = (StorageScreen.BG_WIDTH - 106 - this.font.width(this.title)) / 2 + 106;

        this.search = this.addRenderableWidget(new EditBox(
            this.font,
            this.leftPos + 6,
            this.topPos + 7,
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
            this.leftPos + 2,
            this.topPos + 23,
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
            this.leftPos + 28,
            this.topPos + 23,
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
            this.leftPos + 54,
            this.topPos + 23,
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
            this.leftPos + 80,
            this.topPos + 23,
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
        CategoryList.ButtonInfo info = switch (this.mode) {
            case NORMAL -> CategoryList.ButtonInfo.normal();
            case CRAFTING -> CategoryList.ButtonInfo.small();
        };
        this.categories = this.addRenderableWidget(new CategoryList(
            this.leftPos + 7,
            this.topPos + 49,
            info,
            SettingClientStub.setting(),
            button -> SettingClientStub.update(SettingClientStub.listed().stream().toList())
                .thenRunAsync(this::reorder, this.screenExecutor),
            button -> this.minecraft.setScreen(new CategorySettingsScreen(this.sourcePos, this.title))
        ));
        this.addRenderableWidget(new TexturedButton(
            this.leftPos + 278,
            this.topPos + 139,
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
            this.leftPos + 278,
            this.topPos + 161,
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
        this.addRenderableWidget(new TexturedButton(
            this.leftPos + 278,
            this.topPos + 195,
            18,
            20,
            StorageScreen.CRAFTING,
            20,
            18,
            40,
            button -> this.toggleCraftingMode()
        ));

        // CRAFTING 面板选项按钮：自动补料 / 清空 / 产物去向
        this.craftingAutoFillButton = this.addRenderableWidget(new SwitchableButton(
            this.leftPos + 75,
            this.topPos + 182,
            12,
            12,
            ImmutableList.of(
                StorageScreen.CRAFTING_AUTO_FILL_OFF,
                StorageScreen.CRAFTING_AUTO_FILL_ON
            ),
            12,
            12,
            24,
            (button, index) -> {
                boolean autoFill = index == 1;
                StorageClientStub.craftingSetOptions(
                    StorageScreen.this.sourcePos,
                    autoFill,
                    StorageScreen.this.crafting.toStorage()
                );
                StorageScreen.this.crafting = StorageScreen.this.crafting.withAutoFill(autoFill);
            }
        ));
        this.craftingClearButton = this.addRenderableWidget(new TexturedButton(
            this.leftPos + 62,
            this.topPos + 182,
            12,
            12,
            StorageScreen.CRAFTING_CLEAR,
            12,
            12,
            24,
            button -> StorageClientStub.craftingClearToStorage(StorageScreen.this.sourcePos).thenAcceptAsync(
                ignored -> StorageScreen.this.loadCrafting(true),
                StorageScreen.this.screenExecutor
            )
        ));
        this.craftingToStorageButton = this.addRenderableWidget(new SwitchableButton(
            this.leftPos + 88,
            this.topPos + 182,
            12,
            12,
            ImmutableList.of(
                StorageScreen.CRAFTING_TO_PLAYER,
                StorageScreen.CRAFTING_TO_STORAGE
            ),
            12,
            12,
            24,
            (button, index) -> {
                boolean toStorage = index == 1;
                StorageClientStub.craftingSetOptions(
                    StorageScreen.this.sourcePos,
                    StorageScreen.this.crafting.autoFill(),
                    toStorage
                );
                StorageScreen.this.crafting = StorageScreen.this.crafting.withToStorage(toStorage);
            }
        ));
        if (this.craftingAutoFillButton != null) {
            this.craftingAutoFillButton.visible = false;
        }
        this.craftingToStorageButton.visible = false;
        if (this.craftingClearButton != null) {
            this.craftingClearButton.visible = false;
        }

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
        this.checkCraftingAvailable();
        this.restoreCraftingMode();
        this.refreshMetadata();
    }

    /**
     * 打开界面时恢复上次关闭时的合成模式：读取持久化的 {@code lastOpened}，
     * 为 {@code true} 则运行一次合成模式切换（含可用性检查）。
     */
    private void restoreCraftingMode() {
        StorageClientStub.craftingGet(this.sourcePos).whenCompleteAsync(
            (data, error) -> {
                if (error == null && data.lastOpened() && this.mode == ScreenMode.NORMAL) {
                    this.toggleCraftingMode();
                }
            },
            this.screenExecutor
        );
    }

    /** 异步检查仓储内是否同时存在工作台与切石机（决定合成模式是否可用）。 */
    private void checkCraftingAvailable() {
        StorageClientStub.craftingAvailable(this.sourcePos).whenCompleteAsync(
            (available, error) -> {
                this.craftingAvailable = error == null && available;
                if (error == null) {
                    if (!this.craftingAvailable && this.mode == ScreenMode.CRAFTING) {
                        // 进入时检查失败：回退 NORMAL 并提示
                        this.setMode(ScreenMode.NORMAL);
                        this.showFlyout();
                    } else if (this.mode == ScreenMode.CRAFTING) {
                        this.loadCrafting(true);
                    }
                }
            },
            this.screenExecutor
        );
    }

    /** 点击合成模式切换按钮：检查通过才切换，失败显示缺失提示。 */
    private void toggleCraftingMode() {
        if (this.mode == ScreenMode.CRAFTING) {
            this.setMode(ScreenMode.NORMAL);
            return;
        }
        StorageClientStub.craftingAvailable(this.sourcePos).whenCompleteAsync(
            (available, error) -> {
                if (error != null) {
                    this.showFlyout();
                    return;
                }
                if (available) {
                    this.craftingAvailable = true;
                    this.setMode(ScreenMode.CRAFTING);
                    this.loadCrafting(true);
                } else {
                    this.showFlyout();
                }
            },
            this.screenExecutor
        );
    }

    /** 切换合成/普通模式并同步分类列表布局。 */
    private void setMode(ScreenMode mode) {
        if (this.mode == mode) {
            return;
        }
        this.mode = mode;
        if (this.categories != null) {
            this.categories.rebuild(
                switch (mode) {
                    case NORMAL -> CategoryList.ButtonInfo.normal();
                    case CRAFTING -> CategoryList.ButtonInfo.small();
                },
                SettingClientStub.setting()
            );
        }
        boolean craftingMode = mode == ScreenMode.CRAFTING;
        if (this.craftingAutoFillButton != null) {
            this.craftingAutoFillButton.visible = craftingMode;
        }
        if (this.craftingToStorageButton != null) {
            this.craftingToStorageButton.visible = craftingMode;
        }
        if (this.craftingClearButton != null) {
            this.craftingClearButton.visible = craftingMode;
        }
    }

    /**
     * 加载合成面板数据。① 输入物品类型变化（或 {@code refreshRecipes} 为真）时
     * 重载切石机候选配方；仅数量变化时不重载配方面板。
     */
    public void loadCrafting(boolean refreshRecipes) {
        ItemStack oldInput = this.crafting.stonecutterInput();
        this.craftingLoaded = false;
        StorageClientStub.craftingGet(this.sourcePos).whenCompleteAsync(
            (data, getError) -> {
                if (getError != null) {
                    return;
                }
                this.crafting = data;
                this.craftingLoaded = true;
                if (this.craftingAutoFillButton != null) {
                    this.craftingAutoFillButton.setCurrent(data.autoFill() ? 1 : 0);
                }
                if (this.craftingToStorageButton != null) {
                    this.craftingToStorageButton.setCurrent(data.toStorage() ? 1 : 0);
                }
                boolean inputChanged = !ItemStack.isSameItemSameComponents(oldInput, data.stonecutterInput());
                if (refreshRecipes || inputChanged) {
                    StorageClientStub.craftingStonecutterRecipes(this.sourcePos).whenCompleteAsync(
                        (recipes, recipeError) -> {
                            if (recipeError == null) {
                                this.stonecutterRecipes = recipes;
                                // 配方列表变化后校正滚动位置（不越界）
                                this.recipeScrollable.calculateScroll(
                                    this.recipeHead / StorageScreen.CRAFTING_RECIPE_COLUMNS
                                );
                                this.recipeScrollable.scrollTo();
                            }
                        },
                        this.screenExecutor
                    );
                }
            },
            this.screenExecutor
        );
    }

    /** 显示「仓储内缺失工作台或切石机」浮窗（淡入 + 停留 + 淡出）。 */
    private void showFlyout() {
        this.flyoutTimer = 0;
        this.flyoutVisible = true;
    }

    /** 浮窗当前透明度（0~1）。 */
    private float getFlyoutAlpha() {
        if (!this.flyoutVisible) {
            return 0.0F;
        }
        if (this.flyoutTimer < StorageScreen.FLYOUT_FADE_IN_TICKS) {
            return this.flyoutTimer / (float) StorageScreen.FLYOUT_FADE_IN_TICKS;
        }
        int elapsed = this.flyoutTimer - StorageScreen.FLYOUT_FADE_IN_TICKS;
        if (elapsed < StorageScreen.FLYOUT_HOLD_TICKS) {
            return 1.0F;
        }
        elapsed -= StorageScreen.FLYOUT_HOLD_TICKS;
        if (elapsed < StorageScreen.FLYOUT_FADE_OUT_TICKS) {
            return 1.0F - elapsed / (float) StorageScreen.FLYOUT_FADE_OUT_TICKS;
        }
        return 0.0F;
    }

    @Override
    protected void containerTick() {
        this.carried = this.player.inventoryMenu.getCarried();
        this.flushQuickMoves();
        this.refreshTitle();
        if (
            this.flyoutVisible
            && this.flyoutTimer < StorageScreen.FLYOUT_FADE_IN_TICKS
            + StorageScreen.FLYOUT_HOLD_TICKS
            + StorageScreen.FLYOUT_FADE_OUT_TICKS
        ) {
            this.flyoutTimer++;
        } else if (this.flyoutVisible) {
            this.flyoutVisible = false;
        }
        if (this.metadataCooldown > 0) {
            this.metadataCooldown--;
        } else {
            this.refreshMetadata();
        }
    }

    /**
     * 打开界面期间若板条箱的 dispose 状态变化（相邻虚空物质被放置/移除），
     * 同步更新界面标题（普通「板条箱」↔「溢出销毁板条箱」）。
     */
    private void refreshTitle() {
        if (this.minecraft.level == null) {
            return;
        }
        BlockState state = this.minecraft.level.getBlockState(this.sourcePos);
        if (!(state.getBlock() instanceof CrateBlock)) {
            return;
        }
        Component displayName = CrateBlock.displayName(state);
        // MutableComponent 未按内容重写 equals（引用比较），改比较 contents
        // （TranslatableContents 等为 record，按 key/参数做内容相等），避免每 tick 本地化格式化
        if (this.title.getContents().equals(displayName.getContents())) {
            return;
        }
        this.title = displayName;
        this.titleLabelX = (StorageScreen.BG_WIDTH - 106 - this.font.width(this.title)) / 2 + 106;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 仅画透明渐暗背景，跳过默认的高斯模糊（renderBlurredBackground），避免仓储界面背景模糊
        this.renderTransparentBackground(graphics);
        graphics.blit(
            this.mode.getBackground(),
            this.leftPos,
            this.topPos,
            0,
            0,
            StorageScreen.BG_WIDTH,
            StorageScreen.BG_HEIGHT,
            512,
            256
        );
        this.renderStorageSlider(graphics);
    }

    /**
     * 仓储界面由 {@link #render} 全量自绘（不走 {@code AbstractContainerScreen.render}），
     * 此抽象方法无需绘制任何内容。
     */
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltips = null;
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Background(this, graphics, mouseX, mouseY));
        graphics.blit(
            StorageScreen.CAPACITY,
            this.leftPos + 106,
            this.topPos,
            0,
            0,
            Mth.clamp(Mth.ceil(194 * this.fullness), 0, 194),
            13,
            194,
            13
        );
        graphics.drawString(
            this.font,
            this.title,
            this.leftPos + this.titleLabelX,
            this.topPos + Constant.SCREEN_TITLE_Y,
            0xFF404040,
            false
        );
        this.renderStorageContents(graphics, mouseX, mouseY);
        this.renderPlayerInventory(graphics, mouseX, mouseY);
        if (this.mode == ScreenMode.CRAFTING) {
            this.renderCraftingPanel(graphics, mouseX, mouseY);
        }
        // 背景纹理必须先于 widgets 绘制，而 Screen.render 会二次调用 renderBackground
        // （半透明渐变会盖住纹理），故手动遍历 renderables 渲染 widgets。
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(this, graphics, mouseX, mouseY));
        this.renderCarriedItem(graphics, mouseX, mouseY);
        this.renderFlyout(graphics);
        this.renderStorageTooltip(graphics, mouseX, mouseY);
    }

    private void renderStorageContents(GuiGraphics graphics, int mouseX, int mouseY) {
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        for (int displayIndex = 0; displayIndex < StorageScreen.VISIBLE_STORAGE_SLOTS; displayIndex++) {
            int orderIndex = firstOrderIndex + displayIndex;
            if (orderIndex >= this.displayOrder.size()) {
                break;
            }

            int x = this.leftPos + StorageScreen.STORAGE_X
                + displayIndex % StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            int y = this.topPos + StorageScreen.STORAGE_Y
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
        int sliderOffset = Math.round(
            (StorageScreen.SLIDER_TRACK_HEIGHT - StorageScreen.SLIDER_HEIGHT)
                * this.storageScrollable.getScrollOffs()
        );
        graphics.blit(
            StorageScreen.SLIDER,
            this.leftPos + StorageScreen.SLIDER_X,
            this.topPos + StorageScreen.SLIDER_Y + sliderOffset,
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
                   this.leftPos + StorageScreen.SLIDER_X - 2,
                   this.topPos + StorageScreen.SLIDER_Y,
                   this.leftPos + StorageScreen.SLIDER_X + StorageScreen.SLIDER_WIDTH + 2,
                   this.topPos + StorageScreen.SLIDER_Y + StorageScreen.SLIDER_TRACK_HEIGHT
               );
    }

    /** 配方滚动条轨道命中检测（仅在配方可滚动时）。 */
    private boolean isOverRecipeSliderTrack(double mouseX, double mouseY) {
        if (!this.recipeScrollable.canScroll()) {
            return false;
        }
        int left = this.leftPos + StorageScreen.CRAFTING_RECIPE_X
            + StorageScreen.CRAFTING_RECIPE_COLUMNS * StorageScreen.CRAFTING_SLOT_SIZE + 2;
        int top = this.topPos + StorageScreen.CRAFTING_RECIPE_Y;
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            left,
            top,
            left + 4,
            top + StorageScreen.CRAFTING_RECIPE_ROWS * StorageScreen.CRAFTING_SLOT_SIZE
        );
    }

    /** 按鼠标纵坐标定位存储滚动（连续偏移），并刷新可视内容。 */
    private void scrollSliderTo(double mouseY) {
        this.storageScrollable.scrollOnDrag(
            StorageScreen.SLIDER_HEIGHT,
            mouseY,
            this.topPos + StorageScreen.SLIDER_Y,
            this.topPos + StorageScreen.SLIDER_Y + StorageScreen.SLIDER_TRACK_HEIGHT
        );
    }

    /** 渲染合成面板：① 切石机输入、② 合成 9 宫格、③④ 结果槽、切石机配方选择。 */
    private void renderCraftingPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        // ① 切石机输入（单槽）
        int stonecutterX = this.leftPos + StorageScreen.CRAFTING_STONECUTTER_X;
        int stonecutterY = this.topPos + StorageScreen.CRAFTING_STONECUTTER_Y;
        this.renderCraftingSlot(
            graphics,
            this.crafting.stonecutterInput(),
            stonecutterX,
            stonecutterY,
            mouseX,
            mouseY,
            0
        );

        // ② 合成输入 9 宫格
        for (int i = 0; i < this.crafting.craftingInput().size(); i++) {
            int x = this.leftPos + StorageScreen.CRAFTING_GRID_X + i % 3 * StorageScreen.CRAFTING_SLOT_SIZE;
            int y = this.topPos + StorageScreen.CRAFTING_GRID_Y + i / 3 * StorageScreen.CRAFTING_SLOT_SIZE;
            this.renderCraftingSlot(
                graphics,
                this.crafting.craftingInput().get(i),
                x,
                y,
                mouseX,
                mouseY,
                i + 1
            );
        }

        // ③ 切石机结果、④ 合成结果（仅展示，点击取出）
        this.renderCraftingSlot(
            graphics,
            this.getStonecutterResult(),
            this.leftPos + StorageScreen.CRAFTING_RESULT_STONECUTTER_X,
            this.topPos + StorageScreen.CRAFTING_RESULT_STONECUTTER_Y,
            mouseX,
            mouseY,
            -1
        );
        this.renderCraftingSlot(
            graphics,
            this.getCraftingResult(),
            this.leftPos + StorageScreen.CRAFTING_RESULT_CRAFTING_X,
            this.topPos + StorageScreen.CRAFTING_RESULT_CRAFTING_Y,
            mouseX,
            mouseY,
            -1
        );

        // 切石机配方选择（3 列 × 2 行，超出可滚动；与批量切割机一致）
        if (this.stonecutterRecipes.isEmpty()) {
            return;
        }
        int maxSize = StorageScreen.CRAFTING_RECIPE_COLUMNS * StorageScreen.CRAFTING_RECIPE_ROWS;
        for (int i = this.recipeHead; i < this.recipeHead + Math.min(this.stonecutterRecipes.size() - this.recipeHead, maxSize); i++) {
            int x = this.getCraftingRecipeX(i - this.recipeHead);
            int y = this.getCraftingRecipeY(i - this.recipeHead);
            ItemStack recipe = this.stonecutterRecipes.get(i);
            boolean selected = i == this.crafting.stonecutterSelected();
            boolean hovered = MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18);
            int offsetV = selected ? 18 : hovered ? 36 : 0;
            graphics.blit(
                SharedTextures.SWITCH_TABLE_BUTTON,
                x,
                y,
                0,
                offsetV,
                18,
                18,
                18,
                54
            );
            graphics.renderItem(recipe, x + 1, y + (selected ? 1 : 0));
            if (hovered && this.carried.isEmpty()) {
                this.renderingTooltips = recipe.getTooltipLines(
                    Item.TooltipContext.of(this.minecraft.level),
                    this.player,
                    this.minecraft.options.advancedItemTooltips
                    ? TooltipFlag.Default.ADVANCED
                    : TooltipFlag.Default.NORMAL
                );
            }
        }
        // 配方区右侧滚动条（可滚动时显示）
        if (this.recipeScrollable.canScroll()) {
            int left = this.leftPos + StorageScreen.CRAFTING_RECIPE_X
                + maxSize / StorageScreen.CRAFTING_RECIPE_ROWS * StorageScreen.CRAFTING_SLOT_SIZE + 2;
            int top = this.topPos + StorageScreen.CRAFTING_RECIPE_Y;
            int down = top + StorageScreen.CRAFTING_RECIPE_ROWS * StorageScreen.CRAFTING_SLOT_SIZE;
            graphics.blit(
                SharedTextures.SWITCH_TABLE_SLIDER,
                left,
                top + (int) ((down - top - 12) * this.recipeScrollable.getScrollOffs()),
                0,
                0,
                4,
                12,
                8,
                12
            );
        }
    }

    /**
     * 渲染一个合成面板槽位（物品 + 高亮 + 拖拽分配预览）。
     * {@code craftingSlotId}：0 为①，1~9 为②，-1 为③④ 结果槽（不参与拖拽）。
     */
    private void renderCraftingSlot(
        GuiGraphics graphics,
        ItemStack stack,
        int x,
        int y,
        int mouseX,
        int mouseY,
        int craftingSlotId
    ) {
        boolean hovered = MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17);
        boolean quickCraftPreview = craftingSlotId >= 0
            && this.quickCrafting
            && this.quickCraftCraftingSlots.contains(craftingSlotId);
        if (quickCraftPreview) {
            stack = this.getCraftingQuickCraftPreviewStack(craftingSlotId);
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

    /** 拖拽分配预览：该输入槽将显示的结果栈（当前内容 + 预计放入量）。 */
    private ItemStack getCraftingQuickCraftPreviewStack(int craftingSlotId) {
        ItemStack current = craftingSlotId == 0
            ? this.crafting.stonecutterInput()
            : this.crafting.craftingInput().get(craftingSlotId - 1);
        // 异种槽不参与分配：预览保持原物品
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, this.carried)) {
            return current;
        }
        // ① 仅接受切石机配方输入：非配方物品不会放入，预览保持原样（含空槽）
        if (craftingSlotId == 0 && !this.isStonecutterRecipeInput(this.carried)) {
            return current;
        }
        int currentCount = current.getCount();
        int maxCount = this.carried.getMaxStackSize();
        int placedCount = this.getCraftingQuickCraftPlaceCount();
        int previewCount = Math.min(currentCount + placedCount, maxCount);
        if (previewCount == 0) {
            return ItemStack.EMPTY;
        }
        return this.carried.copyWithCount(previewCount);
    }

    /** 该物品是否可作为① 切石机输入（客户端配方预览用）。 */
    private boolean isStonecutterRecipeInput(ItemStack stack) {
        if (stack.isEmpty() || this.minecraft.level == null) {
            return false;
        }
        return !this.minecraft.level.getRecipeManager()
            .getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(stack), this.minecraft.level)
            .isEmpty();
    }

    /**
     * 拖拽分配中每个目标槽预计放入的数量（与服务端 craftingQuickCraft 一致）：
     * 左键 floor 均分（余数留在指针）、右键每槽 1 个、中键每槽放满。
     * 目标数包含 ①/② 输入槽与背包槽。
     */
    private int getCraftingQuickCraftPlaceCount() {
        int total = this.quickCraftCraftingSlots.size() + this.quickCraftSlots.size();
        if (total == 0) {
            return 0;
        }
        if (this.quickCraftingButton == 1) {
            return 1;
        }
        if (this.quickCraftingButton == 2) {
            return this.carried.getMaxStackSize();
        }
        return Math.floorDiv(this.carried.getCount(), total);
    }

    /** ③ 切石机结果：当前选中配方对①的产物；无配方时为空。 */
    private ItemStack getStonecutterResult() {
        if (!this.craftingLoaded || this.crafting.stonecutterInput().isEmpty()) {
            return ItemStack.EMPTY;
        }
        int selected = this.crafting.stonecutterSelected();
        if (selected < 0 || selected >= this.stonecutterRecipes.size()) {
            return ItemStack.EMPTY;
        }
        return this.stonecutterRecipes.get(selected);
    }

    /** ④ 合成结果：② 9 宫格匹配的第一个合成配方产物（客户端本地计算预览）。 */
    private ItemStack getCraftingResult() {
        if (!this.craftingLoaded) {
            return ItemStack.EMPTY;
        }
        boolean empty = true;
        for (ItemStack stack : this.crafting.craftingInput()) {
            if (!stack.isEmpty()) {
                empty = false;
                break;
            }
        }
        if (empty) {
            return ItemStack.EMPTY;
        }
        if (this.minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        CraftingInput input = CraftingInput.of(
            3,
            3,
            this.crafting.craftingInput()
        );
        List<RecipeHolder<CraftingRecipe>> recipes = this.minecraft.level.getRecipeManager()
            .getRecipesFor(RecipeType.CRAFTING, input, this.minecraft.level);
        if (recipes.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return recipes.getFirst().value().assemble(input, this.minecraft.level.registryAccess());
    }

    /** 渲染「仓储内缺失工作台或切石机」浮窗（flat 九宫格底 + pointer 箭头）。 */
    private void renderFlyout(GuiGraphics graphics) {
        float alpha = this.getFlyoutAlpha();
        if (alpha <= 0.0F) {
            return;
        }
        MutableComponent message = Component.translatable("tooltip.anvilcraft.storage.missing_workbench");
        int textWidth = this.font.width(message);
        int textHeight = this.font.lineHeight;
        int flyoutWidth = textWidth + 5;
        int flyoutHeight = textHeight + 6;
        int flyoutX = this.leftPos + 296 - flyoutWidth;
        int flyoutY = this.topPos + 219;
        int color = (int) (alpha * 255.0F) << 24 | 0xFFFFFF;
        GuiRenderSupport.blitSprite(graphics, StorageScreen.FLYOUT_BACK, flyoutX, flyoutY, flyoutWidth, flyoutHeight, color);
        GuiRenderSupport.blitSprite(graphics, StorageScreen.FLYOUT_POINTER, this.leftPos + 284, this.topPos + 216, 6, 5, color);
        graphics.drawString(this.font, message.withColor(0xEE0000), flyoutX + 3, flyoutY + 3, color, false);
    }

    private void renderPlayerInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        Inventory inv = this.player.getInventory();

        int y = this.topPos + 140 + 58;
        for (int column = 0; column < 9; column++) {
            int x = this.leftPos + 114 + 18 * column;
            this.renderInventorySlot(graphics, inv, column, x, y, mouseX, mouseY);
        }

        for (int row = 0; row < 3; row++) {
            y = this.topPos + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.leftPos + 114 + 18 * column;
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

    private void renderStorageTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.renderingTooltips != null) {
            graphics.renderTooltip(this.font, this.renderingTooltips, Optional.empty(), mouseX, mouseY);
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 106, this.topPos, this.leftPos + 300, this.topPos + 13)) {
            Component tooltip = this.getCapacityTooltip();
            if (tooltip != null) {
                graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            }
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 2, this.topPos + 23, this.leftPos + 26, this.topPos + 43)) {
            graphics.renderTooltip(
                this.font,
                Component.translatable(
                    "screen.anvilcraft.storage.search",
                    SettingClientStub.setting().storage().getSearch().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 28, this.topPos + 23, this.leftPos + 52, this.topPos + 43)) {
            graphics.renderTooltip(
                this.font,
                Component.translatable(
                    "screen.anvilcraft.storage.sort",
                    SettingClientStub.setting().storage().getSort().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 54, this.topPos + 23, this.leftPos + 78, this.topPos + 43)) {
            graphics.renderTooltip(
                this.font,
                Component.translatable(
                    "screen.anvilcraft.storage.order",
                    SettingClientStub.setting().storage().getOrder().getModeName()
                ),
                mouseX,
                mouseY
            );
        } else if (MathUtil.isInRange(mouseX, mouseY, this.leftPos + 80, this.topPos + 23, this.leftPos + 104, this.topPos + 43)) {
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
        int placedCount;
        if (!this.quickCraftCraftingSlots.isEmpty()) {
            // 混合拖拽（输入槽 + 背包槽）：统一 floor 均分
            placedCount = this.getCraftingQuickCraftPlaceCount();
        } else {
            placedCount = AbstractContainerMenu.getQuickCraftPlaceCount(
                this.getQuickCraftSlotSet(),
                this.quickCraftingButton,
                this.carried
            );
        }
        return this.carried.copyWithCount(Math.min(currentCount + placedCount, maxCount));
    }

    private int getQuickCraftRemaining() {
        if (this.quickCraftingButton == 2) {
            return this.carried.getCount();
        }
        if (!this.quickCraftCraftingSlots.isEmpty()) {
            // 混合拖拽：统一 floor 均分，剩余 = 总数 - 每槽配额 × 目标数
            int total = this.quickCraftCraftingSlots.size() + this.quickCraftSlots.size();
            int perSlot = this.quickCraftingButton == 1 ? 1 : Math.floorDiv(this.carried.getCount(), total);
            return Math.max(0, this.carried.getCount() - perSlot * total);
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
            boolean hovered = MathUtil.isInRange(mouseX, mouseY, this.leftPos + 6, this.topPos + 6, this.leftPos + 100, this.topPos + 16);
            if (hovered && button == 1) {
                // 右键搜索框：清空搜索内容并聚焦输入。
                // setValue 触发 responder → 同步服务端设置并重新排序（与手动删除文本一致）
                if (!this.search.getValue().isEmpty()) {
                    this.search.setValue("");
                }
                this.search.setFocused(true);
                this.setFocused(this.search);
                return true;
            }
            this.search.setFocused(hovered);
            this.setFocused(hovered ? this.search : null);
        }

        // 左键按住滚动条：进入拖动状态，并按点击位置立即定位
        if (button == 0 && this.isOverSliderTrack(mouseX, mouseY)) {
            this.draggingSlider = true;
            this.scrollSliderTo(mouseY);
            return true;
        }

        // 配方滚动条：按住拖动
        if (button == 0 && this.isOverRecipeSliderTrack(mouseX, mouseY)) {
            this.recipeScrollable.scrolling();
            this.recipeScrollable.scrollOnDrag(
                12,
                mouseY,
                this.topPos + StorageScreen.CRAFTING_RECIPE_Y,
                this.topPos + StorageScreen.CRAFTING_RECIPE_Y
                    + StorageScreen.CRAFTING_RECIPE_ROWS * StorageScreen.CRAFTING_SLOT_SIZE
            );
            return true;
        }

        // 只分发到子组件（搜索框/按钮），绝不调用 AbstractContainerScreen 的容器点击逻辑
        if (this.dispatchMouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (this.mode == ScreenMode.CRAFTING && (button == 0 || button == 1)) {
            if (this.clickJeiRecipeArea(mouseX, mouseY)) {
                return true;
            }
            if (this.clickCraftingRecipe(mouseX, mouseY)) {
                return true;
            }
            Integer craftingSlot = this.getCraftingSlot(mouseX, mouseY);
            if (craftingSlot != null) {
                if (Screen.hasShiftDown()) {
                    // Shift 点击：把槽内物品移出到背包 → 仓储（放不下留在槽内，不拿指针）
                    this.quickMoveCraftingSlotOut(craftingSlot);
                    return true;
                }
                if (button == 0 && this.isDoubleClick(-(craftingSlot + 1), button)) {
                    this.doubleclick = true;
                    this.doubleClickCraftingSlot = craftingSlot;
                    return true;
                }
                if (!this.carried.isEmpty()) {
                    // 指针有物：进入拖拽准备，释放时若无拖拽则按单次点击放置
                    this.startQuickCraft(button);
                    return true;
                }
                // 空指针：延迟到鼠标释放时执行（区分单击取物与双击收集，避免异步竞争）
                this.pendingCraftingSlot = craftingSlot;
                this.pendingCraftingButton = button;
                return true;
            }
            if (this.clickCraftingResult(mouseX, mouseY, true)) {
                return true;
            }
            if (this.clickCraftingResult(mouseX, mouseY, false)) {
                return true;
            }
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
                // 仅点击界面矩形之外才丢出指针物品；界面内空白处不丢出
                boolean insideGui = MathUtil.isInRange(
                    mouseX,
                    mouseY,
                    this.leftPos,
                    this.topPos,
                    this.leftPos + StorageScreen.BG_WIDTH,
                    this.topPos + StorageScreen.BG_HEIGHT
                );
                if (!insideGui && this.minecraft.gameMode != null && !this.carried.isEmpty()) {
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
                if (button == 1) {
                    // Shift+右键：把该背包槽物品直接放入仓储（不经过指针）
                    this.interactWithStorage(slot, button, StorageInput.QUICK_MOVE_TO_STORAGE);
                } else if (this.carried.isEmpty()) {
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
    /** 双击目标为 ①/② 槽时的槽号（0 为①，1~9 为②），否则 -1。 */
    private int doubleClickCraftingSlot = -1;
    /** 输入槽单击（空指针）延迟到鼠标释放时执行的槽号 / 按钮，-1 表示无。 */
    private int pendingCraftingSlot = -1;
    private int pendingCraftingButton = 0;

    /** 双击检测：crafting 槽用负值标识避免与背包槽冲突。 */
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
        if (this.recipeScrollable.isScrolling()) {
            if (button == 0) {
                this.recipeScrollable.scrollOnDrag(
                    12,
                    mouseY,
                    this.topPos + StorageScreen.CRAFTING_RECIPE_Y,
                    this.topPos + StorageScreen.CRAFTING_RECIPE_Y
                        + StorageScreen.CRAFTING_RECIPE_ROWS * StorageScreen.CRAFTING_SLOT_SIZE
                );
            }
            return true;
        }
        if (this.quickMoveDragging) {
            if (button == 0 && Screen.hasShiftDown()) {
                this.quickMoveDrag(mouseX, mouseY);
            }
            return true;
        }
        if (!this.quickCrafting || button != this.quickCraftingButton || this.carried.isEmpty()) {
            return this.dispatchMouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        Integer craftingSlot = this.getCraftingSlot(mouseX, mouseY);
        if (craftingSlot != null && this.mode == ScreenMode.CRAFTING) {
            int total = this.quickCraftCraftingSlots.size() + this.quickCraftSlots.size();
            if (button == 2 || this.carried.getCount() > total) {
                this.quickCraftCraftingSlots.add(craftingSlot.intValue());
            }
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
                (this.carried.getCount() > this.quickCraftSlots.size() + this.quickCraftCraftingSlots.size()
                    || button == 2)
                && AbstractContainerMenu.canItemQuickReplace(slot, this.carried, true)
                && slot.mayPlace(this.carried)
                && this.player.inventoryMenu.canDragTo(slot)
            ) {
                this.quickCraftSlots.add(screenSlot);
                this.quickCraftInventorySlots.add(inventorySlot);
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    public void quickMoveDrag(double mouseX, double mouseY) {
        Integer storageSlot = this.getStorageSlot(mouseX, mouseY);
        if (storageSlot != null) {
            int key = -1 - storageSlot;
            if (this.quickMoveSlots.add(key)) {
                this.storageQuickMoveSlots.add(storageSlot);
            } else {
                this.quickMoveSlots.remove(key);
                this.storageQuickMoveSlots.remove(storageSlot);
                this.pendingQuickMoveSlots.remove(storageSlot);
            }
            return;
        }
        int inventorySlot = this.getInventorySlot(mouseX, mouseY);
        if (inventorySlot != -1) {
            this.queueQuickMove(inventorySlot);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dispatchMouseReleased(mouseX, mouseY, button);
        if (this.draggingSlider) {
            this.draggingSlider = false;
            return true;
        }
        if (this.recipeScrollable.isScrolling()) {
            this.recipeScrollable.notScrolling();
            return true;
        }
        if (this.quickMoveDragging) {
            this.quickMoveDragging = false;
            this.recordQuickMoveMovedFromSelection();
            this.quickMoveSlots.clear();

            this.flushQuickMoves();
            StorageClientStub.endUndoGroup(this.sourcePos);
            return true;
        }
        if (this.doubleclick) {
            this.doubleclick = false;
            this.lastClickTime = 0L;
            this.pendingCraftingSlot = -1;
            if (button == 0) {
                if (this.doubleClickCraftingSlot >= 0) {
                    // 双击 ①/② 槽：拿起槽内物品并收集背包同种
                    int slot = this.doubleClickCraftingSlot;
                    this.doubleClickCraftingSlot = -1;
                    this.pickupAllCraftingSlot(slot);
                    return true;
                }
                this.doubleClickCraftingSlot = -1;
                if (this.minecraft.gameMode != null) {
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
                        // 背包双击：补充收集 ①/② 输入槽中的同种物品
                        if (this.mode == ScreenMode.CRAFTING && !this.carried.isEmpty()) {
                            this.pickupAllInputsIntoCarried();
                        }
                    }
                }
            } else {
                this.doubleClickCraftingSlot = -1;
            }
            return true;
        }
        if (this.pendingCraftingSlot >= 0) {
            // 输入槽单击（空指针）：取出物品
            int slot = this.pendingCraftingSlot;
            int clickButton = this.pendingCraftingButton;
            this.pendingCraftingSlot = -1;
            this.interactWithCraftingSlot(slot, clickButton);
            return true;
        }
        if (!this.quickCrafting) {
            return false;
        }

        if (button == this.quickCraftingButton && this.minecraft.gameMode != null) {
            this.player.inventoryMenu.setCarried(this.carried);
            boolean hasCrafting = !this.quickCraftCraftingSlots.isEmpty();
            if (this.quickCraftSlots.isEmpty() && this.quickCraftStorageSlots.isEmpty() && !hasCrafting) {
                // 未拖拽：单次点击语义
                Integer craftingSlot = this.mode == ScreenMode.CRAFTING
                    ? this.getCraftingSlot(mouseX, mouseY)
                    : null;
                if (craftingSlot != null && !this.carried.isEmpty()) {
                    this.interactWithCraftingSlot(craftingSlot, button);
                } else {
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
                }
            } else {
                if (!this.quickCraftStorageSlots.isEmpty()) {
                    this.clonePutToStorage();
                }
                if (hasCrafting) {
                    // 输入槽 + 背包槽统一按一组均分
                    this.quickCraftToCraftingSlots(button);
                } else if (!this.quickCraftSlots.isEmpty()) {
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
        this.quickCraftInventorySlots.clear();
        this.quickCraftStorageSlots.clear();
        this.quickCraftCraftingSlots.clear();
        return true;
    }

    private void startQuickCraft(int button) {
        this.quickCrafting = true;
        this.quickCraftingButton = button;
        this.quickCraftSlots.clear();
        this.quickCraftInventorySlots.clear();
        this.quickCraftStorageSlots.clear();
        this.quickCraftCraftingSlots.clear();
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
        if (!this.quickMoveSlots.add(slot)) {
            return;
        }
        if (slot < 0) {
            this.storageQuickMoveSlots.add(-1 - slot);
            return;
        }
        this.pendingQuickMoveSlots.add(slot);
    }

    private void recordQuickMoveMovedFromSelection() {
        for (int slot : this.pendingQuickMoveSlots) {
            this.recordQuickMoveMoved(slot, this.player.getInventory().getItem(slot).getCount());
        }
    }

    private void recordQuickMoveMoved(int slot, int count) {
        if (count <= 0 || slot < 0) {
            return;
        }
        this.quickMoveMovedBySlot.computeIfAbsent(slot, key -> new IntArrayList()).add(count);
    }

    private void flushQuickMoves() {
        this.quickMoveMovedBySlot.clear();
        if (this.pendingQuickMoveSlots.isEmpty() && this.storageQuickMoveSlots.isEmpty()) {
            return;
        }
        IntList slots = new IntArrayList(this.pendingQuickMoveSlots);
        this.pendingQuickMoveSlots.clear();
        if (!slots.isEmpty()) {

            StorageClientStub.quickMoveToStorage(this.sourcePos, slots).whenCompleteAsync(
                (moved, error) -> {
                    if (error != null) {
                        return;
                    }
                    this.applyQuickMoveMoved(slots, moved);
                    if (!moved) {
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
        if (this.storageQuickMoveSlots.isEmpty()) {
            return;
        }
        IntList storageSlots = new IntArrayList(this.storageQuickMoveSlots);
        this.storageQuickMoveSlots.clear();
        StorageClientStub.quickMoveFromStorage(this.sourcePos, storageSlots).whenCompleteAsync(
            (moved, error) -> {
                if (error != null) {
                    return;
                }
                this.applyQuickMoveMoved(storageSlots, moved);
                if (!moved) {
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

    private void applyQuickMoveMoved(IntList slots, boolean moved) {
        if (!moved || slots.isEmpty() || this.quickMoveMovedBySlot.isEmpty()) {
            return;
        }
        for (int slot : slots) {
            IntList counts = this.quickMoveMovedBySlot.remove(slot);
            if (counts != null) {
                for (int count : counts) {
                    StorageClientStub.quickMoveUndo(this.sourcePos, slot, count);
                }
            }
        }
        this.quickMoveMovedBySlot.clear();
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
                if (this.closed) {
                    // 界面已关闭：把 RPC 返回的指针物品放回背包，避免鼠标上残留物品

                    this.returnCarriedToInventory();
                    return;
                }
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

    /**
     * 点击③/④ 结果槽区域的 JEI 打开区域（切石机 (24,132) 14×15 → 打开切石机配方；
     * 合成 (65,200) 14×15 → 打开合成配方）。未安装 JEI 时无操作。返回是否命中。
     */
    private boolean clickJeiRecipeArea(double mouseX, double mouseY) {
        if (
            MathUtil.isInRange(
                mouseX,
                mouseY,
                this.leftPos + 24,
                this.topPos + 132,
                this.leftPos + 24 + 14,
                this.topPos + 132 + 15
            )
        ) {
            StorageJeiBridge.openStonecutterRecipes();
            return true;
        }
        if (
            MathUtil.isInRange(
                mouseX,
                mouseY,
                this.leftPos + 65,
                this.topPos + 200,
                this.leftPos + 65 + 14,
                this.topPos + 200 + 15
            )
        ) {
            StorageJeiBridge.openCraftingRecipes();
            return true;
        }
        return false;
    }

    /** 点击切石机配方按钮：切换选中配方。返回是否命中。 */
    private boolean clickCraftingRecipe(double mouseX, double mouseY) {
        int maxSize = StorageScreen.CRAFTING_RECIPE_COLUMNS * StorageScreen.CRAFTING_RECIPE_ROWS;
        for (int i = this.recipeHead; i < this.recipeHead + Math.min(this.stonecutterRecipes.size() - this.recipeHead, maxSize); i++) {
            int x = this.getCraftingRecipeX(i - this.recipeHead);
            int y = this.getCraftingRecipeY(i - this.recipeHead);
            if (!MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                continue;
            }
            int next = this.crafting.stonecutterSelected() == i ? 0 : i;
            this.crafting = this.crafting.withStonecutterSelected(next);
            StorageClientStub.craftingSelect(this.sourcePos, next);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
            return true;
        }
        return false;
    }

    /** 切石机配方按钮第 i 个的 X 坐标（与批量切割机一致）。 */
    private int getCraftingRecipeX(int i) {
        return this.leftPos + StorageScreen.CRAFTING_RECIPE_X
            + i % StorageScreen.CRAFTING_RECIPE_COLUMNS * StorageScreen.CRAFTING_SLOT_SIZE;
    }

    /** 切石机配方按钮第 i 个的 Y 坐标（与批量切割机一致）。 */
    private int getCraftingRecipeY(int i) {
        return this.topPos + StorageScreen.CRAFTING_RECIPE_Y
            + i / StorageScreen.CRAFTING_RECIPE_COLUMNS * StorageScreen.CRAFTING_SLOT_SIZE;
    }

    /**
     * ① 切石机输入 / ② 合成 9 宫格的槽位命中检测。
     * 返回 -1 表示未命中；0 表示①；1~9 表示②的 9 个槽。
     */
    private @Nullable Integer getCraftingSlot(double mouseX, double mouseY) {
        int stonecutterX = this.leftPos + StorageScreen.CRAFTING_STONECUTTER_X;
        int stonecutterY = this.topPos + StorageScreen.CRAFTING_STONECUTTER_Y;
        if (MathUtil.isInRange(mouseX, mouseY, stonecutterX - 2, stonecutterY - 2, stonecutterX + 17, stonecutterY + 17)) {
            return 0;
        }
        for (int i = 0; i < this.crafting.craftingInput().size(); i++) {
            int x = this.leftPos + StorageScreen.CRAFTING_GRID_X + i % 3 * StorageScreen.CRAFTING_SLOT_SIZE;
            int y = this.topPos + StorageScreen.CRAFTING_GRID_Y + i / 3 * StorageScreen.CRAFTING_SLOT_SIZE;
            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                return i + 1;
            }
        }
        return null;
    }

    /** 与①/②槽交互（按玩家物品栏点击语义）。slot=0 为①，1~9 为②；button=0 左键 / 1 右键。 */
    private void interactWithCraftingSlot(int slot, int button) {
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        CompletableFuture<StorageServerStub.InteractionResult> future;
        if (slot == 0) {
            future = StorageClientStub.craftingPutStonecutterInput(this.sourcePos, button, this.carried);
        } else {
            future = StorageClientStub.craftingPutCraftingSlot(this.sourcePos, slot - 1, button, this.carried);
        }
        future.whenCompleteAsync(
            (result, error) -> {
                if (request != this.interactionRequest || error != null) {
                    this.interactionPending = false;
                    return;
                }
                this.carried = result.carried();
                this.player.inventoryMenu.setCarried(this.carried);
                if (this.closed) {
                    // 界面已关闭：把 RPC 返回的指针物品放回背包，避免鼠标上残留物品

                    this.returnCarriedToInventory();
                    return;
                }
                if (result.changed()) {
                    this.loadCrafting(false);
                }
                this.interactionPending = false;
            },
            this.screenExecutor
        );
    }

    /** 双击 ①/② 槽：拿起槽内物品并从背包收集同种到指针。 */
    private void pickupAllCraftingSlot(int slot) {
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        StorageClientStub.craftingPickupAll(this.sourcePos, slot, this.carried).whenCompleteAsync(
            (result, error) -> {
                if (request != this.interactionRequest || error != null) {
                    this.interactionPending = false;
                    return;
                }
                this.carried = result.carried();
                this.player.inventoryMenu.setCarried(this.carried);
                if (this.closed) {
                    // 界面已关闭：把 RPC 返回的指针物品放回背包，避免鼠标上残留物品

                    this.returnCarriedToInventory();
                    return;
                }
                if (result.changed()) {
                    this.loadCrafting(false);
                }
                this.interactionPending = false;
            },
            this.screenExecutor
        );
    }

    /** 背包槽双击补充：把 ①/② 输入槽中与指针同种的物品收集到指针。 */
    private void pickupAllInputsIntoCarried() {
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        StorageClientStub.craftingPickupIntoCarried(this.sourcePos, this.carried).whenCompleteAsync(
            (result, error) -> {
                if (request != this.interactionRequest || error != null) {
                    this.interactionPending = false;
                    return;
                }
                this.carried = result.carried();
                this.player.inventoryMenu.setCarried(this.carried);
                if (this.closed) {
                    // 界面已关闭：把 RPC 返回的指针物品放回背包，避免鼠标上残留物品

                    this.returnCarriedToInventory();
                    return;
                }
                if (result.changed()) {
                    this.loadCrafting(false);
                }
                this.interactionPending = false;
            },
            this.screenExecutor
        );
    }

    /** 输入槽 Shift 点击：把槽内物品移出到背包 → 仓储（放不下留在槽内，不拿指针）。 */
    private void quickMoveCraftingSlotOut(int slot) {
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        StorageClientStub.craftingQuickMoveOut(this.sourcePos, slot).whenCompleteAsync(
            (changed, error) -> {
                if (request != this.interactionRequest || error != null) {
                    this.interactionPending = false;
                    return;
                }
                if (changed) {
                    this.loadCrafting(false);
                }
                this.interactionPending = false;
            },
            this.screenExecutor
        );
    }

    /**
     * 拖拽分配结束：把指针物品按原版规则（左键 floor 均分 / 右键每槽 1 个 / 中键填满）
     * 放入 ①/② 输入槽与（如有）玩家背包槽，所有目标作为一组统一计算。
     */
    private void quickCraftToCraftingSlots(int button) {
        if (this.quickCraftCraftingSlots.isEmpty()) {
            return;
        }
        final IntList craftingSlots = new IntArrayList(this.quickCraftCraftingSlots);
        final IntList inventorySlots = new IntArrayList(this.quickCraftInventorySlots);
        this.quickCraftCraftingSlots.clear();
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        StorageClientStub.craftingQuickCraft(
            this.sourcePos,
            button,
            craftingSlots,
            inventorySlots,
            this.carried
        ).whenCompleteAsync(
            (result, error) -> {
                if (request != this.interactionRequest || error != null) {
                    this.interactionPending = false;
                    return;
                }
                this.carried = result.carried();
                this.player.inventoryMenu.setCarried(this.carried);
                if (this.closed) {
                    // 界面已关闭：把 RPC 返回的指针物品放回背包，避免鼠标上残留物品

                    this.returnCarriedToInventory();
                    return;
                }
                if (result.changed()) {
                    this.loadCrafting(false);
                }
                this.interactionPending = false;
            },
            this.screenExecutor
        );
    }

    /** 播放原版切石机取走音效：与方块侧一致，同一游戏 tick 内最多播放一次。 */
    private void playStonecutterTakeSound() {
        if (this.minecraft.level == null) {
            return;
        }
        long tick = this.minecraft.level.getGameTime();
        if (tick == this.lastStonecutterTakeSoundTick) {
            return;
        }
        this.lastStonecutterTakeSoundTick = tick;
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 1.0F));
    }

    /**
     * 点击③/④ 结果槽：取出结果并消耗输入。stonecutter=true 为③。
     * Shift 点击时连续合成：服务端每次 RPC 最多合成一个分块，客户端循环调用
     * 直至 done（材料耗尽 / 无处可放 / 不消耗型配方），避免单次 RPC 长时间阻塞
     * 服务端线程。
     */
    private boolean clickCraftingResult(double mouseX, double mouseY, boolean stonecutter) {
        int x = stonecutter
                ? this.leftPos + StorageScreen.CRAFTING_RESULT_STONECUTTER_X
                : this.leftPos + StorageScreen.CRAFTING_RESULT_CRAFTING_X;
        int y = stonecutter
                ? this.topPos + StorageScreen.CRAFTING_RESULT_STONECUTTER_Y
                : this.topPos + StorageScreen.CRAFTING_RESULT_CRAFTING_Y;
        if (!MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
            return false;
        }
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return true;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        boolean shift = Screen.hasShiftDown();
        if (shift) {
            this.takeAllChunk(request, stonecutter, 0);
        } else {
            StorageClientStub.craftingTakeResult(this.sourcePos, stonecutter, false).whenCompleteAsync(
                (result, error) -> {
                    if (request != this.interactionRequest || error != null) {
                        this.interactionPending = false;
                        return;
                    }
                    this.carried = result.carried();
                    this.player.inventoryMenu.setCarried(this.carried);
                    if (result.changed()) {
                        if (stonecutter) {
                            this.playStonecutterTakeSound();
                        }
                        this.loadCrafting(false);
                    }
                    this.interactionPending = false;
                },
                this.screenExecutor
            );
        }
        return true;
    }

    /**
     * 连续合成的一个分块：调用一次服务端 {@code craftingTakeAll}，未完成
     * （done=false）时递归调用下一个分块，直至自然终止或达到总块数上限。
     */
    private void takeAllChunk(int request, boolean stonecutter, int chunkIndex) {
        StorageClientStub.craftingTakeAll(this.sourcePos, stonecutter).whenCompleteAsync(
            (result, error) -> {
                if (request != this.interactionRequest || error != null) {
                    this.interactionPending = false;
                    return;
                }
                this.carried = result.carried();
                this.player.inventoryMenu.setCarried(this.carried);
                if (this.closed) {
                    // 界面已关闭：把 RPC 返回的指针物品放回背包，避免鼠标上残留物品

                    this.returnCarriedToInventory();
                    return;
                }
                if (result.changed()) {
                    if (stonecutter) {
                        this.playStonecutterTakeSound();
                    }
                    this.loadCrafting(false);
                }
                if (result.done()) {
                    this.interactionPending = false;
                    return;
                }
                // 未完成：继续下一个分块（上限兜底，防止异常配方导致客户端无限循环）
                if (chunkIndex + 1 >= StorageScreen.MAX_TAKE_ALL_CHUNKS) {
                    this.interactionPending = false;
                    return;
                }
                this.takeAllChunk(request, stonecutter, chunkIndex + 1);
            },
            this.screenExecutor
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return this.dispatchMouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        // 悬停在切石机配方选择区：滚动配方列表
        if (this.mode == ScreenMode.CRAFTING && !this.stonecutterRecipes.isEmpty()) {
            int recipeRight = this.leftPos + StorageScreen.CRAFTING_RECIPE_X
                + StorageScreen.CRAFTING_RECIPE_COLUMNS * StorageScreen.CRAFTING_SLOT_SIZE + 6;
            int recipeBottom = this.topPos + StorageScreen.CRAFTING_RECIPE_Y
                + StorageScreen.CRAFTING_RECIPE_ROWS * StorageScreen.CRAFTING_SLOT_SIZE;
            if (MathUtil.isInRange(
                mouseX,
                mouseY,
                this.leftPos + StorageScreen.CRAFTING_RECIPE_X,
                this.topPos + StorageScreen.CRAFTING_RECIPE_Y,
                recipeRight,
                recipeBottom
            )) {
                if (this.recipeScrollable.canScroll()) {
                    this.recipeScrollable.scrollOnScroll(scrollY / 1.2);
                }
                return true;
            }
        }
        if (
            !MathUtil.isInRange(
                mouseX,
                mouseY,
                this.leftPos + StorageScreen.STORAGE_X - 2,
                this.topPos + StorageScreen.SLIDER_Y,
                this.leftPos + StorageScreen.SLIDER_X + StorageScreen.SLIDER_WIDTH,
                this.topPos + StorageScreen.STORAGE_Y + StorageScreen.STORAGE_ROWS * StorageScreen.SLOT_SIZE
            )
        ) {
            return this.dispatchMouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (this.storageScrollable.canScroll()) {
            this.storageScrollable.scrollOnScroll(scrollY / 1.2);
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
        if (keyCode == InputConstants.KEY_ESCAPE && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        }
        if (Screen.hasControlDown() && keyCode == InputConstants.KEY_Z) {
            this.undoLastMove();
            return true;
        }
        if (this.dispatchKeyPressed(keyCode, scanCode, modifiers)) {
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
        return this.dispatchKeyReleased(keyCode, scanCode, modifiers);
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

    /**
     * 屏幕被重新显示时（例如从 JEI 配方界面返回）复位关闭与交互中间态，
     * 避免 {@link #removed()} 置位后所有 RPC 回调被 {@code closed} 拦截。
     */
    @Override
    public void added() {
        this.closed = false;
        this.interactionPending = false;
        this.interactionSyncPending = false;
        this.doubleclick = false;
        this.pendingCraftingSlot = -1;
        this.pendingCraftingButton = 0;
        this.quickCrafting = false;
        this.quickMoveDragging = false;
        this.quickCraftSlots.clear();
        this.quickCraftStorageSlots.clear();
        this.quickCraftCraftingSlots.clear();
        this.quickCraftInventorySlots.clear();
        this.quickMoveSlots.clear();
        this.pendingQuickMoveSlots.clear();
        this.storageQuickMoveSlots.clear();
        this.quickMoveMovedBySlot.clear();
        this.carried = this.player.inventoryMenu.getCarried();
    }

    @Override
    public void removed() {
        this.closed = true;

        this.reorderRequest++;
        this.syncRequest++;
        this.metadataPending = false;
        // 记录上次关闭界面时是否为合成模式，下次打开时据此恢复
        StorageClientStub.craftingSetLastOpened(this.sourcePos, this.mode == ScreenMode.CRAFTING);
        if (this.tracksOpenState && this.minecraft.player != null) {
            StorageClientStub.setOpen(this.sourcePos, false);
        }
        // 关闭界面时让服务端把指针物品放回背包
        this.returnCarriedToInventory();
        if (SettingClientStub.setting().storage().getSearch() == SearchMode.CLEAR) {
            SettingClientStub.update("");
        }
        super.removed();
    }

    /**
     * 让服务端把指针物品放回玩家背包。关闭界面时服务端 {@code containerMenu}
     * 仍是 {@code inventoryMenu}，由 RPC 直接操作背包并广播，避免客户端
     * {@code handleInventoryMouseClick} 在容器关闭后被服务端忽略。
     */
    private void returnCarriedToInventory() {
        if (this.minecraft.player == null) {
            return;
        }
        StorageClientStub.returnCarriedToInventory(this.sourcePos);
        this.carried = this.player.inventoryMenu.getCarried();
    }

    /**
     * 以下 dispatch 系列复刻 {@code Screen} 的默认输入分发（遍历子组件），
     * 刻意不调用 {@code AbstractContainerScreen} 的对应实现——那些实现会通过
     * {@code slotClicked} → {@code handleInventoryMouseClick} 走原版容器同步，
     * 而仓储界面的全部同步由 RPC 管控。
     */

    private boolean dispatchMouseClicked(double mouseX, double mouseY, int button) {
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : this.children()) {
            if (listener.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(listener);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    private void dispatchMouseReleased(double mouseX, double mouseY, int button) {
        this.setDragging(false);
        this.getChildAt(mouseX, mouseY)
            .filter(listener -> listener.mouseReleased(mouseX, mouseY, button));
    }

    private boolean dispatchMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.getFocused() != null
            && this.isDragging()
            && button == 0
            && this.getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean dispatchMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.getChildAt(mouseX, mouseY)
            .filter(listener -> listener.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
            .isPresent();
    }

    /**
     * {@code AbstractContainerScreen.keyPressed} 仅在 {@code hoveredSlot != null} 时才会触发
     * 原版容器点击（CLONE/THROW/SWAP）；本界面自绘渲染，从不调用 {@code AbstractContainerScreen.render}，
     * {@code hoveredSlot} 恒为 null，因此可直接复用父类实现（含 ESC 与 Tab/方向键焦点导航），
     * 不会产生任何原版容器同步。
     */
    private boolean dispatchKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean dispatchKeyReleased(int keyCode, int scanCode, int modifiers) {
        return this.getFocused() != null && this.getFocused().keyReleased(keyCode, scanCode, modifiers);
    }

    public int getLeftPos() {
        return this.leftPos;
    }

    public int getTopPos() {
        return this.topPos;
    }

    public int getImageWidth() {
        return this.imageWidth;
    }

    public int getImageHeight() {
        return this.imageHeight;
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
        if (this.mode == ScreenMode.CRAFTING) {
            Integer craftingSlot = this.getCraftingSlot(mouseX, mouseY);
            if (craftingSlot != null) {
                ItemStack stack = craftingSlot == 0
                                   ? this.crafting.stonecutterInput()
                                   : this.crafting.craftingInput().get(craftingSlot - 1);
                if (stack.isEmpty()) {
                    return null;
                }
                int x;
                int y;
                if (craftingSlot == 0) {
                    x = this.leftPos + StorageScreen.CRAFTING_STONECUTTER_X;
                    y = this.topPos + StorageScreen.CRAFTING_STONECUTTER_Y;
                } else {
                    int index = craftingSlot - 1;
                    x = this.leftPos + StorageScreen.CRAFTING_GRID_X + index % 3 * StorageScreen.CRAFTING_SLOT_SIZE;
                    y = this.topPos + StorageScreen.CRAFTING_GRID_Y + index / 3 * StorageScreen.CRAFTING_SLOT_SIZE;
                }
                return new ItemArea(stack, x, y);
            }
        }
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        for (int displayIndex = 0; displayIndex < StorageScreen.VISIBLE_STORAGE_SLOTS; displayIndex++) {
            int orderIndex = firstOrderIndex + displayIndex;
            int x = this.leftPos + StorageScreen.STORAGE_X
                    + displayIndex % StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            int y = this.topPos + StorageScreen.STORAGE_Y
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
        int x = this.leftPos + 114 + 18 * (inventorySlot % 9);
        int y = inventorySlot < 9
                ? this.topPos + 140 + 58
                : this.topPos + 140 + 18 * ((inventorySlot - 9) / 9);
        return new ItemArea(stack, x, y);
    }

    private record ItemArea(ItemStack stack, int x, int y) {
    }

    private @Nullable Integer getStorageSlot(double mouseX, double mouseY) {
        int firstOrderIndex = this.scrollRow * StorageScreen.STORAGE_COLUMNS;
        for (int displayIndex = 0; displayIndex < StorageScreen.VISIBLE_STORAGE_SLOTS; displayIndex++) {
            int orderIndex = firstOrderIndex + displayIndex;
            int x = this.leftPos + StorageScreen.STORAGE_X
                + displayIndex % StorageScreen.STORAGE_COLUMNS * StorageScreen.SLOT_SIZE;
            int y = this.topPos + StorageScreen.STORAGE_Y
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
        int y = this.topPos + 140 + 58;
        for (int column = 0; column < 9; column++) {
            int x = this.leftPos + 114 + 18 * column;
            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                return column;
            }
        }

        for (int row = 0; row < 3; row++) {
            y = this.topPos + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.leftPos + 114 + 18 * column;
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
            this.storageScrollable.reset();
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
                this.storageScrollable.calculateScroll(this.scrollRow);
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
                        this.storageScrollable.calculateScroll(this.scrollRow);
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
                                this.storageScrollable.calculateScroll(this.scrollRow);
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
            this.displayOrder = this.applySearchFilter(new IntArrayList(this.order));
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
            this.displayOrder = this.applySearchFilter(new IntArrayList(this.order));
            return;
        }

        this.rebuildFoldedGroups(false);
    }

    /**
     * 服务端（无客户端语言环境）普通文本搜索只按 id path 过滤，本地化名称匹配
     * 由客户端完成：非 @/# 前缀的搜索词同时匹配物品的本地化显示名与 id path。
     */
    private IntList applySearchFilter(IntList order) {
        String search = SettingClientStub.setting().storage().getSearchContent().strip().toLowerCase(Locale.ROOT);
        if (search.isEmpty() || search.charAt(0) == '@' || search.charAt(0) == '#') {
            return order;
        }
        IntArrayList filtered = new IntArrayList(order.size());
        for (int slot : order) {
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

    private void rebuildFoldedGroups(boolean preserveRepresentatives) {
        List<IntList> groups = new ArrayList<>();
        Map<Item, IntList> groupsByItem = new HashMap<>();
        String search = SettingClientStub.setting().storage().getSearchContent().strip().toLowerCase(Locale.ROOT);
        boolean filterBySearch = !search.isEmpty() && search.charAt(0) != '@' && search.charAt(0) != '#';
        for (int slot : this.order) {
            UnlimitedItemStack stack = this.contents.getOrDefault(slot, UnlimitedItemStack.EMPTY);
            if (stack.isEmpty()) {
                continue;
            }
            if (filterBySearch) {
                String name = stack.toStack().getHoverName().getString().toLowerCase(Locale.ROOT);
                String idPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                if (!name.contains(search) && !idPath.contains(search)) {
                    continue;
                }
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
    
    public static ResourceLocation texture(String path) {
        return SharedTextures.textureGui("misc/storage_station/" + path);
    }

    @Getter
    protected enum ScreenMode {
        NORMAL(SharedTextures.bg("misc", "storage_station")),
        CRAFTING(SharedTextures.bg("misc", "storage_station_crafting")),
        ;

        private final ResourceLocation background;

        ScreenMode(ResourceLocation background) {
            this.background = background;
        }

        public ScreenMode next() {
            return switch (this) {
                case NORMAL -> ScreenMode.CRAFTING;
                case CRAFTING -> ScreenMode.NORMAL;
            };
        }
    }
}
