package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

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
}
