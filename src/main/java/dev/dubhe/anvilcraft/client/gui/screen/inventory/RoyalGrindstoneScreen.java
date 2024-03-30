package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;

public class RoyalGrindstoneScreen extends AbstractContainerScreen<RoyalGrindstoneMenu> {
    private static final ResourceLocation GRINDSTONE_LOCATION = AnvilCraft.of("textures/gui/container/royal_grindstone.png");

    public RoyalGrindstoneScreen(RoyalGrindstoneMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GRINDSTONE_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
        if ((((RoyalGrindstoneMenu)this.menu).getSlot(0).hasItem() || ((RoyalGrindstoneMenu)this.menu).getSlot(1).hasItem()) && !((RoyalGrindstoneMenu)this.menu).getSlot(2).hasItem()) {
            guiGraphics.blit(GRINDSTONE_LOCATION, i + 92, j + 31, this.imageWidth, 0, 28, 21);
        }
    }
}
