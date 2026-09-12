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
import dev.dubhe.anvilcraft.block.entity.storage.StorageFluidRegistry;
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
import dev.dubhe.anvilcraft.util.FluidAmountUtil;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
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
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

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
    /** 合成格补货弹跳动画时长（游戏 tick）。 */
    private static final int CRAFTING_POP_TICKS = 5;
    /** 缺失工作台/切石机提示浮窗：0.25s 淡入 + 1.25s 停留 + 0.25s 淡出。 */
    private static final int FLYOUT_FADE_IN_TICKS = 5;
    private static final int FLYOUT_HOLD_TICKS = 25;
    private static final int FLYOUT_FADE_OUT_TICKS = 5;
    /**
     * 浮层的 z。界面内各层的 z：物品图标 150、耐久条与数量数字 200、本浮层 300、
     * 原版工具提示 400（{@code GuiGraphics#renderTooltipInternal}）。取 300 才不会
     * 被数量数字压住，同时仍在工具提示之下。
     */
    private static final int FLYOUT_Z = 300;
    /**
     * 工具提示顶边相对鼠标的上移量（像素）。
     *
     * <p>见 {@code DefaultTooltipPositioner}：提示起点为 {@code (mouseX + 12, mouseY - 12)}，
     * 并自该点向下延伸。浮层若落进这段区间就会被提示盖住。</p>
     */
    private static final int TOOLTIP_TOP_OFFSET = 12;
    /** 浮层与工具提示顶边之间额外留出的间距（像素）。 */
    private static final int FLYOUT_GAP = 6;
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
    /**
     * 已连接端口中的流体，作为伪条目接在物品列表尾部。
     *
     * <p>流体不占存储类别，故用 {@link #FLUID_SLOT_BASE} 起的独立逻辑槽位号，
     * 与真实物品槽位不会冲突。</p>
     */
    @Getter
    private List<StorageServerStub.FluidEntry> fluids = List.of();
    /** 流体伪槽位的逻辑编号起点 */
    private static final int FLUID_SLOT_BASE = StorageFluidRegistry.FLUID_SLOT_BASE;
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
    /** 上一次播放合成补货拾取音效的游戏 tick（同一 tick 只播一次）。*/
    private long lastCraftingRefillSoundTick = -1;
    private boolean closed;
    private boolean spacePressed;
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
    /** 合成格槽位（0=切石机输入，1~9=合成输入）弹跳动画开始的游戏 tick，Long.MIN_VALUE 表示无动画。 */
    private final Int2LongMap craftingPopTicks = new Int2LongOpenHashMap();
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
    /** 浮窗文本；由「缺失工作台」提示与流体交互失败提示共用同一套淡入淡出。 */
    private Component flyoutMessage = Component.empty();
    /** 浮窗是否锚定在点击处（流体提示）而非指向合成区（缺失工作台提示）。 */
    private boolean flyoutAtClick;
    private int flyoutClickX;
    private int flyoutClickY;

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
        this.craftingPopTicks.defaultReturnValue(Long.MIN_VALUE);
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
            button -> StorageClientStub.deposit(
                StorageScreen.this.sourcePos,
                Screen.hasShiftDown(),
                true
            ).thenAcceptAsync(
                result -> {
                    if (result.changed()) {
                        StorageScreen.this.reorder(false);
                    }
                },
                StorageScreen.this.screenExecutor
            ),
            // 右键：把流体桶当普通物品存入，不倒进液体
            button -> StorageClientStub.deposit(
                StorageScreen.this.sourcePos,
                Screen.hasShiftDown(),
                false
            ).thenAcceptAsync(
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
        boolean craftingMode = this.mode == ScreenMode.CRAFTING;
        if (this.craftingAutoFillButton != null) {
            this.craftingAutoFillButton.visible = craftingMode;
        }
        this.craftingToStorageButton.visible = craftingMode;
        if (this.craftingClearButton != null) {
            this.craftingClearButton.visible = craftingMode;
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

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.init(minecraft, width, height);
    }

    /**
     * 打开界面时恢复上次关闭时的合成模式：读取持久化的 {@code lastOpened}，
     * 为 {@code true} 则运行一次合成模式切换（含可用性检查）。
     */
    private void restoreCraftingMode() {
        StorageClientStub.craftingAvailable(this.sourcePos)
            .thenCombine(StorageClientStub.craftingGet(this.sourcePos), (available, data) -> available && data.lastOpened())
            .thenAcceptAsync(opened -> {
                if (opened && this.mode == ScreenMode.NORMAL) {
                    this.craftingAvailable = true;
                    this.setMode(ScreenMode.CRAFTING);
                    this.loadCrafting(true);
                }
            }, this.screenExecutor);
    }

    /** 异步检查仓储是否已解锁合成模式。 */
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
        StorageClientStub.craftingUnlock(this.sourcePos).whenCompleteAsync(
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
        this.flyoutMessage = Component.translatable("tooltip.anvilcraft.storage.missing_workbench");
        this.flyoutAtClick = false;
        this.flyoutTimer = 0;
        this.flyoutVisible = true;
    }

    /**
     * 在点击处显示一条提示浮窗（如流体格交互失败的原因）。
     *
     * <p>这类提示不能走动作栏：仓储界面开着时动作栏被界面盖住，玩家看不到任何反馈。</p>
     */
    private void showNotice(Component message) {
        if (message.getString().isEmpty()) {
            return;
        }
        this.flyoutMessage = message;
        this.flyoutAtClick = true;
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
            this.renderCraftingPanel(graphics, mouseX, mouseY, partialTick);
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
            // 流体伪槽位：渲染流体图标与数量，物品相关逻辑一律跳过
            if (slot >= StorageScreen.FLUID_SLOT_BASE) {
                StorageServerStub.FluidEntry entry = this.getFluidSlot(slot);
                if (entry != null) {
                    StorageScreen.renderFluidIcon(graphics, this.minecraft.font, entry, x, y);
                }
                if (hovered) {
                    AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
                    if (entry != null) {
                        // 与物品一致：交给 renderStorageTooltip 统一渲染，
                        // 在循环内直接 renderTooltip 会与延迟渲染路径叠加成两个 tooltip
                        this.renderingTooltips = List.of(
                            entry.icon().getHoverName(),
                            Component.translatable(
                                "screen.anvilcraft.storage.fluid_amount",
                                FluidAmountUtil.formatExactAmount(entry.amount())
                            )
                        );
                    }
                }
                continue;
            }
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
    private void renderCraftingPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
            0,
            partialTick
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
                i + 1,
                partialTick
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
            -1,
            partialTick
        );
        this.renderCraftingSlot(
            graphics,
            this.getCraftingResult(),
            this.leftPos + StorageScreen.CRAFTING_RESULT_CRAFTING_X,
            this.topPos + StorageScreen.CRAFTING_RESULT_CRAFTING_Y,
            mouseX,
            mouseY,
            -1,
            partialTick
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
        int craftingSlotId,
        float partialTick
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
            float popScale = craftingSlotId >= 0
                ? this.getCraftingPopScale(craftingSlotId, partialTick)
                : 1.0F;
            // 原版 Gui.renderSlot 拾取动画：横向压缩 1/f1、纵向拉伸 (f1+1)/2，
            // 缩放中心为 (x+8, y+12)；数量文字在缩放外绘制
            if (popScale > 1.0F) {
                float scaleX = 1.0F / popScale;
                float scaleY = (popScale + 1.0F) / 2.0F;
                graphics.pose().pushPose();
                graphics.pose().translate(x + 8.0F, y + 12.0F, 0.0F);
                graphics.pose().scale(scaleX, scaleY, 1.0F);
                graphics.pose().translate(-(x + 8.0F), -(y + 12.0F), 0.0F);
            }
            graphics.renderItem(stack, x, y);
            if (popScale > 1.0F) {
                graphics.pose().popPose();
            }
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

    /**
     * 计算指定合成槽位的拾取动画因子：返回与原版 Gui.renderSlot 中相同的
     * {@code f1 = 1 + f / 5} 值（动画未启动时为 1.0F），由调用方按原版公式
     * 应用横向压缩与纵向拉伸。
     */
    private float getCraftingPopScale(int craftingSlotId, float partialTick) {
        if (this.minecraft.level == null) {
            return 1.0F;
        }
        long start = this.craftingPopTicks.get(craftingSlotId);
        if (start == Long.MIN_VALUE) {
            return 1.0F;
        }
        // 原版 Gui.renderSlot：f = popTime - partialTick；f1 = 1 + f / 5，
        // 这里用自计时替代 popTime，用 partialTick 插值平滑
        float remaining = StorageScreen.CRAFTING_POP_TICKS
            - (this.minecraft.level.getGameTime() - start) - partialTick;
        if (remaining <= 0.0F) {
            this.craftingPopTicks.put(craftingSlotId, Long.MIN_VALUE);
            return 1.0F;
        }
        return 1.0F + remaining / StorageScreen.CRAFTING_POP_TICKS;
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

    /** 渲染浮窗（flat 九宫格底，缺失工作台提示另带指向合成区的 pointer 箭头）。 */
    private void renderFlyout(GuiGraphics graphics) {
        float alpha = this.getFlyoutAlpha();
        if (alpha <= 0.0F) {
            return;
        }
        // 先落地滞留的批量文本：数量数字经 drawString 进入共享缓冲，而 GuiGraphics#flush 是
        // 「关闭深度测试 + endBatch」——等它自己被刷新时会无视 z 直接画到浮层之上，
        // 因此必须先把它们刷出来，浮层才能真正盖在最上层。
        graphics.flush();
        int textWidth = this.font.width(this.flyoutMessage);
        int textHeight = this.font.lineHeight;
        int flyoutWidth = textWidth + 5;
        int flyoutHeight = textHeight + 6;
        int flyoutX;
        int flyoutY;
        if (this.flyoutAtClick) {
            // 点击处提示：贴近点击位置上方居中，并夹在窗口内
            flyoutX = Mth.clamp(
                this.flyoutClickX - flyoutWidth / 2,
                4,
                Math.max(4, this.width - flyoutWidth - 4)
            );
            // 整体让开工具提示的顶边（鼠标上方 TOOLTIP_TOP_OFFSET），否则底边会落进提示矩形内被盖住
            flyoutY = Math.max(
                4,
                this.flyoutClickY - StorageScreen.TOOLTIP_TOP_OFFSET - StorageScreen.FLYOUT_GAP - flyoutHeight
            );
        } else {
            flyoutX = this.leftPos + 296 - flyoutWidth;
            flyoutY = this.topPos + 219;
        }
        int color = (int) (alpha * 255.0F) << 24 | 0xFFFFFF;
        // 底图走 blitOffset 参数（绝对顶点 z，不受 pose 影响），文字走 pose 平移，两者要分别设置。
        // 若不抬 z，浮层会与物品图标（z=150）、数量数字（z=200）同层而被打平压住。
        GuiRenderSupport.blitSprite(
            graphics, StorageScreen.FLYOUT_BACK, flyoutX, flyoutY,
            StorageScreen.FLYOUT_Z, flyoutWidth, flyoutHeight, color
        );
        if (!this.flyoutAtClick) {
            GuiRenderSupport.blitSprite(
                graphics, StorageScreen.FLYOUT_POINTER, this.leftPos + 284, this.topPos + 216,
                StorageScreen.FLYOUT_Z, 6, 5, color
            );
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, StorageScreen.FLYOUT_Z);
        graphics.drawString(this.font, this.flyoutMessage.copy().withColor(0xEE0000), flyoutX + 3, flyoutY + 3, color, false);
        pose.popPose();
        // 立即定型本次浮层，使其先于工具提示（z=400）绘制，避免浮层文字盖住工具提示底框
        graphics.flush();
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
        } else if (
            this.mode == ScreenMode.CRAFTING
            && this.craftingClearButton != null
            && this.craftingClearButton.visible
            && MathUtil.isInRange(mouseX, mouseY, this.leftPos + 62, this.topPos + 182, this.leftPos + 74, this.topPos + 194)
        ) {
            graphics.renderTooltip(
                this.font,
                Component.translatable("screen.anvilcraft.storage.crafting.clear"),
                mouseX,
                mouseY
            );
        } else if (
            this.mode == ScreenMode.CRAFTING
            && this.craftingAutoFillButton != null
            && this.craftingAutoFillButton.visible
            && MathUtil.isInRange(mouseX, mouseY, this.leftPos + 75, this.topPos + 182, this.leftPos + 87, this.topPos + 194)
        ) {
            graphics.renderTooltip(
                this.font,
                this.craftingAutoFillButton.getCurrent() == 1
                    ? Component.translatable(
                        "screen.anvilcraft.storage.crafting.auto_fill",
                        Component.translatable("screen.anvilcraft.storage.crafting.auto_fill.enabled")
                    )
                    : Component.translatable(
                        "screen.anvilcraft.storage.crafting.auto_fill",
                        Component.translatable("screen.anvilcraft.storage.crafting.auto_fill.disabled")
                    ),
                mouseX,
                mouseY
            );
        } else if (
            this.mode == ScreenMode.CRAFTING
            && MathUtil.isInRange(mouseX, mouseY, this.leftPos + 88, this.topPos + 182, this.leftPos + 100, this.topPos + 194)
        ) {
            if (this.craftingToStorageButton != null) {
                graphics.renderTooltip(
                    this.font,
                    this.craftingToStorageButton.getCurrent() == 1
                        ? Component.translatable(
                            "screen.anvilcraft.storage.crafting.to_storage",
                            Component.translatable("screen.anvilcraft.storage.crafting.to_storage.storage")
                        )
                        : Component.translatable(
                            "screen.anvilcraft.storage.crafting.to_storage",
                            Component.translatable("screen.anvilcraft.storage.crafting.to_storage.player")
                        ),
                    mouseX,
                    mouseY
                );
            }
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
            // 流体格：左键为流体行为（倒入 / 取出），右键保持原有物品行为
            // （把指针上的流体桶当作普通物品存入，否则流体桶将永远无法入库）
            Integer fluidSlot = this.getFluidSlotAt(mouseX, mouseY);
            if (fluidSlot != null && this.minecraft.gameMode != null) {
                if (button == 1) {
                    // 流体格内没有物品可取，右键空指针不做任何事
                    if (this.carried.isEmpty()) {
                        return true;
                    }
                    this.interactWithStorage(fluidSlot, button, StorageInput.PICKUP);
                    return true;
                }
                StorageInput action = Screen.hasShiftDown()
                                      ? StorageInput.QUICK_MOVE_FROM_STORAGE
                                      : StorageInput.FLUID_BUCKET;
                // 失败提示要显示在点击处，故先记下点击位置
                this.flyoutClickX = (int) mouseX;
                this.flyoutClickY = (int) mouseY;
                this.interactWithStorage(fluidSlot, button, action);
                return true;
            }
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
            // 在移动之前记下本次点击的物品（对应原版在 slotClicked 之前记 lastQuickMoved）。
            // 槽已空时不覆盖，否则首次点击移走整叠后，第二次点击会把记录清空、批量失效
            ItemStack clickedItem = this.player.getInventory().getItem(slot);
            if (!clickedItem.isEmpty()) {
                this.lastQuickMoved = clickedItem.copy();
            }

            if (Screen.hasAltDown()) {
                // 左键：桶装流体自动倾倒；右键：保持物品行为存入流体桶
                this.moveSameToStorage(slot, button == 0);
                return true;
            }

            if (Screen.hasShiftDown()) {
                // Shift+双击左键：与原版一致，把上一个被点击物品的同种物品整批移入仓储。
                // 首次 Shift+左键仍是单组快速移动，第二次落在 250ms 内才触发批量，节奏与原版相同
                if (button == 0 && this.isDoubleClick(slot, button)) {
                    // 首次点击已把被点槽整叠移走，改从仍持有该物品的槽位发起：
                    // 服务端 moveSameToStorage 会把该物品的所有槽位一并移入
                    int target = this.findInventorySlotWith(this.lastQuickMoved);
                    if (target != -1) {
                        this.moveSameToStorage(target, true);
                    }
                    return true;
                }
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

            // ①/② 输入槽中键：与仓储槽同构——指针为空时复制一整组到指针，
            // 否则进入中键拖拽复制（每个划过的槽填成满堆叠，指针不消耗）
            Integer craftingSlot = this.mode == ScreenMode.CRAFTING
                ? this.getCraftingSlot(mouseX, mouseY)
                : null;
            if (craftingSlot != null
                && this.player.hasInfiniteMaterials()
                && this.minecraft.options.keyPickItem.matchesMouse(2)) {
                if (this.carried.isEmpty()) {
                    this.cloneCraftingSlot(craftingSlot);
                } else {
                    this.startQuickCraft(button);
                    // 先记下起始槽：即使不再移动鼠标，松开时也会按中键语义把它填成满堆叠；
                    // 否则会落入「单次点击」分支去放 1 个物品，与原版中键语义不符
                    this.quickCraftCraftingSlots.add(craftingSlot.intValue());
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
            } else if (this.player.hasInfiniteMaterials()) {
                // 中键拖拽复制仅创造模式有效（原版 isValidQuickcraftType）；生存模式若允许开始
                // 拖拽，预览会显示物品、松开时服务端拒绝应用，物品看起来凭空消失
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
    /**
     * 上一次点击到的物品，供 Shift+双击批量使用（对应原版的 {@code lastQuickMoved}）。
     *
     * <p>首次 Shift+左键会把该槽整叠移入仓储、槽随之变空，若批量时再读该槽就取不到物品，
     * 故必须在移动之前记下它。</p>
     */
    private ItemStack lastQuickMoved = ItemStack.EMPTY;
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
        // 中键拖拽复制仅创造模式有效；生存模式下不累积任何槽位，
        // 否则渲染出的物品预览与松开时服务端拒绝应用的结果不一致（物品看起来凭空消失）
        if (button == 2 && !this.player.hasInfiniteMaterials()) {
            return true;
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

    private void moveSameToStorage(int slot, boolean pour) {
        StorageClientStub.moveSameToStorage(this.sourcePos, slot, pour).whenCompleteAsync(
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
        int serverSlot = action == StorageInput.QUICK_MOVE_TO_STORAGE || slot >= StorageScreen.FLUID_SLOT_BASE
                         ? slot
                         : this.serverSlots.get(slot);
        // 流体格同时上报流体身份：点击与处理之间列表可能变化（端口被拆 / 区块卸载 / 新流体接入），
        // 服务端按下标会取到另一种流体，故改按身份匹配
        FluidStack fluidIdentity = FluidStack.EMPTY;
        if (slot >= StorageScreen.FLUID_SLOT_BASE) {
            StorageServerStub.FluidEntry entry = this.getFluidSlot(slot);
            if (entry != null) {
                fluidIdentity = entry.icon().copyWithAmount(FluidType.BUCKET_VOLUME);
            }
        }
        StorageClientStub.interact(this.sourcePos, serverSlot, button, action, fluidIdentity).whenCompleteAsync(
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
                if (result.notice() != StorageServerStub.FluidNotice.NONE) {
                    // 交互失败原因由界面自己渲染，不走动作栏（会被界面盖住）
                    this.showNotice(result.notice().text());
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
     * ①/② 输入槽按 Q / Ctrl+Q：把槽内物品直接丢到地上。
     * {@code stack=true}（Ctrl+Q）丢出整堆，{@code false}（Q）丢 1 个。
     */
    private void throwCraftingInputSlot(int slot, boolean stack) {
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        StorageClientStub.craftingThrowSlot(this.sourcePos, slot, stack).whenCompleteAsync(
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

    /** ①/② 输入槽中键：创造模式下把槽内物品复制一整组到指针（槽内保留）。 */
    private void cloneCraftingSlot(int slot) {
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        this.player.inventoryMenu.setCarried(this.carried);
        int request = ++this.interactionRequest;
        StorageClientStub.craftingCloneSlot(this.sourcePos, slot).whenCompleteAsync(
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
     * 鼠标是否悬停在③/④ 结果槽上：返回 {@code true} 表示③（切石机）、{@code false} 表示④（合成），
     * 未悬停或不在合成界面时返回 null。
     */
    private @Nullable Boolean getHoveredCraftingResult() {
        if (this.mode != ScreenMode.CRAFTING) {
            return null;
        }
        double mouseX = this.getMouseScaledX();
        double mouseY = this.getMouseScaledY();
        int stonecutterX = this.leftPos + StorageScreen.CRAFTING_RESULT_STONECUTTER_X;
        int stonecutterY = this.topPos + StorageScreen.CRAFTING_RESULT_STONECUTTER_Y;
        if (MathUtil.isInRange(
            mouseX, mouseY, stonecutterX - 2, stonecutterY - 2, stonecutterX + 17, stonecutterY + 17
        )) {
            return true;
        }
        int craftingX = this.leftPos + StorageScreen.CRAFTING_RESULT_CRAFTING_X;
        int craftingY = this.topPos + StorageScreen.CRAFTING_RESULT_CRAFTING_Y;
        if (MathUtil.isInRange(
            mouseX, mouseY, craftingX - 2, craftingY - 2, craftingX + 17, craftingY + 17
        )) {
            return false;
        }
        return null;
    }

    /**
     * 在③/④ 结果槽按 Q / Ctrl+Q：合成并把产物直接丢到地上。
     * {@code stack=true}（Ctrl+Q）连续合成约一组，{@code false}（Q）只合成一次。
     */
    private void throwCraftingResult(boolean stonecutter, boolean stack) {
        if (this.minecraft.gameMode == null || this.interactionPending) {
            return;
        }
        this.interactionPending = true;
        int request = ++this.interactionRequest;
        StorageClientStub.craftingThrowResult(this.sourcePos, stonecutter, stack).whenCompleteAsync(
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
                    this.triggerCraftingPop(result.refilledSlots());
                    this.loadCrafting(false);
                }
                this.interactionPending = false;
            },
            this.screenExecutor
        );
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
        boolean space = this.spacePressed && !shift;
        if (shift || space) {
            this.takeAllChunk(request, stonecutter, 0, space ? 8 : 1);
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
                        this.triggerCraftingPop(result.refilledSlots());
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
     * 根据补货位掩码触发对应合成格槽位的弹跳动画（bit0 为切石机输入槽，bit1~bit9 为合成格槽），
     * 并播放玩家拾取物品音效。
     */
    private void triggerCraftingPop(int refilledSlots) {
        if (refilledSlots == 0 || this.minecraft.level == null) {
            return;
        }
        long tick = this.minecraft.level.getGameTime();
        for (int slot = 0; slot < 10; slot++) {
            if ((refilledSlots & (1 << slot)) != 0) {
                this.craftingPopTicks.put(slot, tick);
            }
        }
        this.playCraftingRefillSound(tick);
    }

    /** 播放原版玩家拾取物品音效（与接触实体物品一致），同一游戏 tick 内最多播放一次。*/
    private void playCraftingRefillSound(long tick) {
        if (tick == this.lastCraftingRefillSoundTick) {
            return;
        }
        this.lastCraftingRefillSoundTick = tick;
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0F));
    }

    /**
     * 连续合成的一个分块：调用一次服务端 {@code craftingTakeAll}，未完成
     * （done=false）时递归调用下一个分块，直至自然终止或达到总块数上限。
     */
    private void takeAllChunk(int request, boolean stonecutter, int chunkIndex, int multiplier) {
        StorageClientStub.craftingTakeAll(this.sourcePos, stonecutter, multiplier).whenCompleteAsync(
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
                    this.triggerCraftingPop(result.refilledSlots());
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
                this.takeAllChunk(request, stonecutter, chunkIndex + 1, multiplier);
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
            this.spacePressed = false;
            this.search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (keyCode == InputConstants.KEY_SPACE) {
            this.spacePressed = true;
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
        // drop 键必须绕开 dispatchKeyPressed：父类在 hoveredSlot 为 null（本界面自绘，恒为 null）时，
        // 仍会因 Forge MC-146650 的兜底分支把它标记为已处理，从而吞掉仓储槽与背包槽的丢弃
        boolean dropKey = this.minecraft.options.keyDrop.isActiveAndMatches(key);
        Integer storageSlot = this.getStorageSlot();
        if (storageSlot != null && storageSlot >= 0 && this.minecraft.gameMode != null) {
            if (this.minecraft.options.keyPickItem.isActiveAndMatches(key)) {
                this.interactWithStorage(storageSlot, 0, StorageInput.CLONE);
                return true;
            } else if (dropKey) {
                int dropMode = Screen.hasControlDown() ? Screen.hasShiftDown() ? 2 : 1 : 0;
                this.interactWithStorage(storageSlot, dropMode, StorageInput.THROW);
                return true;
            }
        }
        // ③/④ 结果槽：Q / Ctrl+Q 把合成产物直接丢到地上（同样必须早于父类吞键）
        Boolean craftingResult = this.getHoveredCraftingResult();
        if (dropKey && craftingResult != null && this.minecraft.gameMode != null) {
            this.throwCraftingResult(craftingResult, Screen.hasControlDown());
            return true;
        }
        // ①/② 输入槽：Q / Ctrl+Q 把槽内物品丢到地上（与物品栏槽位一致：Q 丢 1 个，Ctrl+Q 丢整堆）
        if (dropKey && this.mode == ScreenMode.CRAFTING && this.minecraft.gameMode != null) {
            Integer craftingSlot = this.getCraftingSlot(this.getMouseScaledX(), this.getMouseScaledY());
            if (craftingSlot != null) {
                this.throwCraftingInputSlot(craftingSlot, Screen.hasControlDown());
                return true;
            }
        }
        if (!dropKey && this.dispatchKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            this.onClose();
            return true;
        } else {
            int hoveredSlot = this.getInventorySlot();
            if (hoveredSlot == -1 || this.minecraft.gameMode == null) {
                // 未悬停背包槽位时仍按 MC-146650 视为已处理，避免落到快捷栏丢弃
                return dropKey;
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
        if (keyCode == InputConstants.KEY_SPACE) {
            this.spacePressed = false;
            return true;
        }
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
     *
     * <p>注意：父类还有一个不依赖 {@code hoveredSlot} 的兜底分支——Forge MC-146650 在
     * {@code hoveredSlot} 为 null 时仍会把 drop 键标记为已处理并返回 true。因此涉及仓储槽的
     * 取物 / 丢弃必须在调用本方法之前处理，否则 Q 会被吞掉（见 {@link #keyPressed}）。</p>
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
                    int slot = this.displayOrder.getInt(orderIndex);
                    // 流体伪槽位暂不参与物品交互，避免把伪索引发给服务端
                    return slot >= StorageScreen.FLUID_SLOT_BASE ? null : slot;
                }
                return this.carried.isEmpty() ? null : -1;
            }
        }
        return null;
    }

    private @Nullable Integer getStorageSlot() {
        return this.getStorageSlot(this.getMouseScaledX(), this.getMouseScaledY());
    }

    /**
     * 命中流体伪槽位时返回其逻辑槽位号，便于单独处理桶交互。
     *
     * @return 流体槽位号；未命中流体格时返回 {@code null}
     */
    private @Nullable Integer getFluidSlotAt(double mouseX, double mouseY) {
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
            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                int slot = this.displayOrder.getInt(orderIndex);
                return slot >= StorageScreen.FLUID_SLOT_BASE ? slot : null;
            }
        }
        return null;
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

    /**
     * 在当前玩家背包中找一个仍持有该物品的槽位（槽号语义与 {@link #getInventorySlot} 一致）。
     *
     * <p>供 Shift+双击批量使用：首次点击后原槽可能已空，需要换一个仍持有该物品的槽位
     * 作为批量入口，服务端会据此把同种物品的所有槽位一并移入仓储。</p>
     *
     * @param wanted 目标物品；为空时返回 -1
     * @return 槽号；没有匹配时返回 -1
     */
    private int findInventorySlotWith(ItemStack wanted) {
        if (wanted.isEmpty()) {
            return -1;
        }
        int size = Math.min(this.player.getInventory().getContainerSize(), Inventory.INVENTORY_SIZE);
        for (int slot = 0; slot < size; slot++) {
            if (ItemStack.isSameItemSameComponents(this.player.getInventory().getItem(slot), wanted)) {
                return slot;
            }
        }
        return -1;
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
        this.fluids = result.fluids();
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
            // 流体伪槽位没有物品内容，跳过以免取到 null
            if (logicalSlot >= StorageScreen.FLUID_SLOT_BASE) {
                continue;
            }
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
            // 与 applySyncResult 一致：保持排序的同步路径同样要刷新流体列表，
            // 否则按住 Shift 取液后界面上的储量会停留在旧值
            this.fluids = result.fluids();
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
            // 流体伪槽位不参与服务端槽位映射，也不应撑大逻辑槽位计数
            if (slot >= StorageScreen.FLUID_SLOT_BASE) {
                continue;
            }
            this.serverSlots.put(slot, slot);
            this.nextLogicalSlot = Math.max(this.nextLogicalSlot, slot + 1);
        }
    }

    private boolean hasContents(IntList slots) {
        for (int slot : slots) {
            // 流体由 fluids 提供内容，不在 contents 中
            if (slot >= StorageScreen.FLUID_SLOT_BASE) {
                continue;
            }
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
            // 流体已由服务端排入 order，此处不能再追加，否则会重复
            this.displayOrder = this.applySearchFilter(new IntArrayList(this.order));
            return;
        }

        this.rebuildFoldedGroups(false);
    }

    /**
     * 把流体伪槽位追加到物品列表末尾。
     *
     * <p>折叠显示会按物品重组列表并丢弃流体，故折叠路径需要重新追加；
     * 非折叠路径的流体位置由服务端排序决定，不走这里。</p>
     *
     * <p>只追加服务端 {@link #order} 中出现的流体槽位：分类过滤、搜索过滤与
     * 0 数量的取舍都由服务端排序统一决定，这里照搬可避免在折叠模式下漏掉它们
     * （例如把「流体」分类设为黑名单后，折叠模式仍把流体显示出来）。</p>
     */
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
        this.displayOrder = this.appendFluidSlots(foldedOrder);
    }

    private UnlimitedItemStack getDisplayedStack(int slot) {
        Int2ObjectMap<UnlimitedItemStack> displayedContents = this.nbtFolded ? this.foldedContents : this.contents;
        return displayedContents.getOrDefault(slot, UnlimitedItemStack.EMPTY);
    }

    /**
     * 取得流体伪槽位对应的流体。
     *
     * @param slot 逻辑槽位号
     * @return 对应流体，越界时为空
     */
    private @Nullable StorageServerStub.FluidEntry getFluidSlot(int slot) {
        int index = slot - StorageScreen.FLUID_SLOT_BASE;
        return index >= 0 && index < this.fluids.size() ? this.fluids.get(index) : null;
    }

    /**
     * 在槽位内绘制流体图标与数量。
     *
     * <p>数量按 {@link FluidAmountUtil} 规则显示：不足 1 B 用 mB，达到 1 B 用 B，
     * 有小数时保留三位有效数字。取空后条目仍以 0 保留，与物品一致。</p>
     */
    private static void renderFluidIcon(
        GuiGraphics graphics,
        Font font,
        StorageServerStub.FluidEntry entry,
        int x,
        int y
    ) {
        FluidStack fluid = entry.icon();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        graphics.blit(
            x, y, 0, 16, 16, sprite,
            FastColor.ARGB32.red(tint) / 255f,
            FastColor.ARGB32.green(tint) / 255f,
            FastColor.ARGB32.blue(tint) / 255f,
            1.0f
        );
        Component text = Component.literal(FluidAmountUtil.formatAmount(entry.amount()))
            .withStyle(style -> style.withFont(StorageScreen.SMALL_FONT));
        // 与物品一致：0 数量用橙色，便于与正常数量区分
        StorageScreen.renderSlotCount(graphics, font, text, entry.amount() == 0 ? 0xFFFFAA00 : -1, x, y);
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

        pose.popPose();

        // 数量（使用缩写格式，可超过 999）
        Component amount = Component.literal(FormattingUtil.toAbbrNum(count))
            .withStyle(style -> style.withFont(StorageScreen.SMALL_FONT));
        StorageScreen.renderSlotCount(graphics, font, amount, count == 0 ? 0xFFFFAA00 : -1, x, y);

        // noinspection UnstableApiUsage
        ItemDecoratorHandler.of(stack).render(graphics, font, stack, x, y);
    }

    /**
     * 在槽位右下角绘制数量文本，物品数量与流体数量共用，保证两者字号与位置一致。
     *
     * <p>使用小字体；文本宽于 16 像素时整段缩放到 0.75 并微调基线，避免溢出槽位。</p>
     *
     * @param text  已带字体样式的数量文本
     * @param color 文本颜色
     */
    private static void renderSlotCount(
        GuiGraphics graphics,
        Font font,
        Component text,
        int color,
        int x,
        int y
    ) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        // 抬高 z 使其绘制在图标之上
        pose.translate(0, 0, 200);
        pose.translate(x + 17, y + 9, 0);
        int width = font.width(text);
        if (width > 16) {
            pose.scale(0.75F, 0.75F, 1);
            pose.translate(-1F, font.lineHeight * 0.25F - 0.25F, 0);
        }
        graphics.drawString(font, text, -width, 0, color, true);
        pose.popPose();
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
