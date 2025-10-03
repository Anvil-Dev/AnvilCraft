package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.util.RenderUtil;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.inventory.component.ShulkerContainerSlot;
import dev.dubhe.anvilcraft.network.ShulkerContainerSyncPacket;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class ShulkerContainerScreen extends AbstractContainerScreen<ShulkerContainerMenu> {
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
}
