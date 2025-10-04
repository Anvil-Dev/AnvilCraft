package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.util.RenderUtil;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.inventory.component.ShulkerContainerSlot;
import dev.dubhe.anvilcraft.network.ShulkerContainerSyncPacket;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShulkerContainerScreen extends AbstractContainerScreen<ShulkerContainerMenu> {
    private static EditBox searching;
    private static SearchMode searchMode = SearchMode.CLEAR;
    private static SortMode sortMode = SortMode.NUMBER;
    private static SortOrderMode sortOrderMode = SortOrderMode.SEQUENTIAL;
    private static NbtDisplayMode nbtDisplayMode = NbtDisplayMode.UNFOLD;

    public ShulkerContainerScreen(ShulkerContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 300;
        this.imageHeight = 222;
        this.titleLabelX = 111;
        this.titleLabelY = 7;
        this.inventoryLabelX = 111;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
        if (ShulkerContainerScreen.searchMode == SearchMode.RETENTION) {
            ShulkerContainerScreen.searching = new EditBox(
                this.font,
                this.getGuiLeft() + 7,
                this.getGuiTop() + 7,
                92,
                9,
                ShulkerContainerScreen.searching,
                Component.empty()
            );
        } else {
            ShulkerContainerScreen.searching = new EditBox(
                this.font,
                this.getGuiLeft() + 7,
                this.getGuiTop() + 7,
                92,
                9,
                Component.empty()
            );
        }
        ShulkerContainerScreen.searching.setBordered(false);
        ShulkerContainerScreen.searching.setTextShadow(false);
        this.addRenderableWidget(ShulkerContainerScreen.searching);
        this.addRenderableWidget(new SwitchableButton(
            this.getGuiLeft() + 2,
            this.getGuiTop() + 23,
            24,
            20,
            List.of(
                TextureConstants.SHULKER_CONTAINER_SEARCH_CLEAR,
                TextureConstants.SHULKER_CONTAINER_SEARCH_RETENTION
            ),
            20,
            24,
            40,
            (button, i) -> ShulkerContainerScreen.searchMode = SearchMode.values()[i]
        ).setCurrent(ShulkerContainerScreen.searchMode.ordinal()));
        this.addRenderableWidget(new SwitchableButton(
            this.getGuiLeft() + 28,
            this.getGuiTop() + 23,
            24,
            20,
            List.of(
                TextureConstants.SHULKER_CONTAINER_SORT_BY_NUMBER,
                TextureConstants.SHULKER_CONTAINER_SORT_BY_MOD,
                TextureConstants.SHULKER_CONTAINER_SORT_BY_NAME
            ),
            20,
            24,
            40,
            (button, i) -> ShulkerContainerScreen.sortMode = SortMode.values()[i]
        ).setCurrent(ShulkerContainerScreen.sortMode.ordinal()));
        this.addRenderableWidget(new SwitchableButton(
            this.getGuiLeft() + 54,
            this.getGuiTop() + 23,
            24,
            20,
            List.of(
                TextureConstants.SHULKER_CONTAINER_SEQUENTIAL_ORDER,
                TextureConstants.SHULKER_CONTAINER_REVERSE_ORDER
            ),
            20,
            24,
            40,
            (button, i) -> ShulkerContainerScreen.sortOrderMode = SortOrderMode.values()[i]
        ).setCurrent(ShulkerContainerScreen.sortOrderMode.ordinal()));
        this.addRenderableWidget(new SwitchableButton(
            this.getGuiLeft() + 80,
            this.getGuiTop() + 23,
            24,
            20,
            List.of(
                TextureConstants.SHULKER_CONTAINER_NBT_UNFOLD,
                TextureConstants.SHULKER_CONTAINER_NBT_FOLD
            ),
            20,
            24,
            40,
            (button, i) -> ShulkerContainerScreen.nbtDisplayMode = NbtDisplayMode.values()[i]
        ).setCurrent(ShulkerContainerScreen.nbtDisplayMode.ordinal()));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
            TextureConstants.SHULKER_CONTAINER_BG,
            this.leftPos,
            this.topPos,
            0,
            0,
            300,
            222,
            512,
            256
        );
    }

    @Override
    protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
        int i = slot.x;
        int j = slot.y;
        int j1 = slot.x + slot.y * this.imageWidth;
        guiGraphics.renderFakeItem(itemstack, i, j, j1);

        if (slot instanceof ShulkerContainerSlot scSlot) {
            RenderUtil.renderItemDecorations(guiGraphics, this.font, scSlot.getUnlimitedItem(), i, j, countString);
        } else {
            guiGraphics.renderItemDecorations(this.font, itemstack, i, j, countString);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        //noinspection DataFlowIssue - For Minecraft.getInstance().getConnection().registryAccess() - 此时已有Connection
        PacketSplitter.INSTANCE.split(
            ShulkerContainerSyncPacket.TYPE,
            ShulkerContainerSyncPacket.STREAM_CODEC,
            new ShulkerContainerSyncPacket(this.menu.blockEntity.getUUID()),
            1640,
            Minecraft.getInstance().getConnection().registryAccess(),
            PacketDistributor::sendToServer
        );
    }

    enum SearchMode {
        CLEAR, RETENTION
    }

    enum SortMode {
        NUMBER, MOD, NAME
    }

    enum SortOrderMode {
        SEQUENTIAL, REVERSE
    }

    enum NbtDisplayMode {
        UNFOLD, FOLD
    }
}
