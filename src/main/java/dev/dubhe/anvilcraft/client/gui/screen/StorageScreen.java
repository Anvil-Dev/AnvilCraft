package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class StorageScreen extends Screen {
    private static final Identifier BACKGROUND = SharedTextures.bg("misc", "storage_station");
    private static final Identifier CAPACITY = SharedTextures.textureGui("misc/storage_station/capacity");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static final int BG_WIDTH = 300;
    private static final int BG_HEIGHT = 222;
    private final BlockPos sourcePos;
    private final Player player;
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
            },
            this.screenExecutor
        );
        StorageClientStub.load(this.sourcePos).thenAcceptAsync(
            fullness -> this.fullness = fullness,
            this.screenExecutor
        );
    }

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

    private void extractCarriedItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.carried.isEmpty()) {
            return;
        }
        graphics.nextStratum();
        graphics.item(this.carried, mouseX - 8, mouseY - 8);
        graphics.itemDecorations(this.font, this.carried, mouseX - 8, mouseY - 8);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        InputConstants.Key key = InputConstants.getKey(event);
        if (super.keyPressed(event)) {
            return true;
        } else if (this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0 && event.button() != 1) {
            return false;
        }

        int slot = this.getInventorySlot(event.x(), event.y());
        if (slot == -1 || this.minecraft.gameMode == null) {
            return false;
        }

        this.player.inventoryMenu.setCarried(this.carried);
        this.minecraft.gameMode.handleContainerInput(
            this.player.inventoryMenu.containerId,
            slot < 9 ? slot + 36 : slot,
            event.button(),
            ContainerInput.PICKUP,
            this.player
        );
        this.carried = this.player.inventoryMenu.getCarried();
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
        super.removed();
    }

    // protected boolean checkHotbarKeyPressed(KeyEvent event) {
    //     var key = com.mojang.blaze3d.platform.InputConstants.getKey(event);
    //     if (this.carried.isEmpty() && this.hoveredSlot != null) {
    //         if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(key)) {
    //             this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
    //             return true;
    //         }
    //         for (int i = 0; i < 9; i++) {
    //             if (this.minecraft.options.keyHotbarSlots[i].isActiveAndMatches(key)) {
    //                 this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
    //                 return true;
    //             }
    //         }
    //     }
    //     return false;
    // }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void reorder() {
        StorageClientStub.reorder(this.sourcePos);
    }
}
