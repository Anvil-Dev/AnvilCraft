package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class StorageScreen extends Screen {
    private static final Identifier BACKGROUND = SharedTextures.bg("misc", "storage_station");
    private static final Identifier CAPACITY = SharedTextures.textureGui("misc/storage_station/capacity");
    private static final Identifier SEARCH_CLEAR = SharedTextures.textureGui("misc/storage_station/search_clear");
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
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static final int BG_WIDTH = 300;
    private static final int BG_HEIGHT = 222;
    private final BlockPos sourcePos;
    private final Player player;

    private @Nullable EditBox search;
    private @Nullable CategoryList categories;

    private ItemStack carried = ItemStack.EMPTY;
    private double fullness;
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
            this.reorder();
        });
        this.addRenderableWidget(new SwitchableButton(
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
            (_, index) -> SettingClientStub.update(SearchMode.values()[index])
        ));
        List<Identifier> sortTextures = Lists.newArrayList(
            StorageScreen.SORT_COUNT,
            StorageScreen.SORT_MOD,
            StorageScreen.SORT_NAME
        );
        this.addRenderableWidget(new SwitchableButton(
            this.left + 28,
            this.top + 23,
            24,
            20,
            sortTextures,
            20,
            24,
            40,
            (_, index) -> SettingClientStub.update(SortMode.values()[index])
        ));
        this.addRenderableWidget(new SwitchableButton(
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
            }
        ));
        this.addRenderableWidget(new SwitchableButton(
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
            _ -> this.reorder(),
            _ -> this.minecraft.setScreenAndShow(new CategorySettingsScreen(this.sourcePos))
        ));

        SettingClientStub.load().thenAcceptAsync(
            setting -> {
                if (this.categories != null) {
                    this.categories.rebuild(setting);
                }
                if (setting.storage().getOrder() == OrderMode.SEQUENTIAL) {
                    sortTextures.set(0, StorageScreen.SORT_COUNT);
                    sortTextures.set(2, StorageScreen.SORT_NAME);
                } else {
                    sortTextures.set(0, StorageScreen.SORT_COUNT_REVERSED);
                    sortTextures.set(2, StorageScreen.SORT_NAME_REVERSED);
                }
            },
            this.screenExecutor
        );
        StorageClientStub.load(this.sourcePos).thenAcceptAsync(
            fullness -> this.fullness = fullness,
            this.screenExecutor
        );
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
        this.extractPlayerInventory(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractCarriedItem(graphics, mouseX, mouseY);
        this.extractTooltip(graphics, mouseX, mouseY);
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
        if (MathUtil.isInRange(mouseX, mouseY, this.left + 2, this.top + 23, this.left + 26, this.top + 43)) {
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

    private void extractCarriedItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.carried.isEmpty()) {
            return;
        }
        graphics.nextStratum();
        graphics.item(this.carried, mouseX - 8, mouseY - 8);
        graphics.itemDecorations(this.font, this.carried, mouseX - 8, mouseY - 8);
    }
    // endregion

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.search != null && (event.button() == 0 || event.button() == 1)) {
            boolean hovered = MathUtil.isInRange(event.x(), event.y(), this.left + 6, this.top + 6, this.left + 100, this.top + 16);
            this.search.setFocused(hovered);
            this.setFocused(hovered ? this.search : null);
        }

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() == 0 || event.button() == 1) {
            int slot = this.getInventorySlot(event.x(), event.y());
            if (slot == -1 || this.minecraft.gameMode == null) {
                return false;
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
                // Forge MC-146650: Emulate MC bug, so we don't drop from hotbar when pressing drop without hovering over a item.
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
        return invSlot < 9 ? invSlot + 36 : invSlot;
    }

    private int getScreenSlot() {
        return this.getScreenSlot(this.getInventorySlot());
    }

    private void reorder() {
        StorageClientStub.reorder(this.sourcePos);
    }
}
