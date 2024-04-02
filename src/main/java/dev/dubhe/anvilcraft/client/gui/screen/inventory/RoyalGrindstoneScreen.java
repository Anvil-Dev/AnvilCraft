package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RoyalGrindstoneScreen extends AbstractContainerScreen<RoyalGrindstoneMenu> {
    private static final ResourceLocation GRINDSTONE_LOCATION = AnvilCraft.of("textures/gui/container/royal_grindstone.png");

    public RoyalGrindstoneScreen(RoyalGrindstoneMenu menu, Inventory playerInventory,@SuppressWarnings("unused") Component title) {
        super(menu, playerInventory, Component.translatable("screen.anvilcraft.royal_grindstone.title"));
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderLabels(guiGraphics);
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GRINDSTONE_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    protected void renderLabels(GuiGraphics guiGraphics) {
        if (this.menu.getSlot(2).hasItem()) {
            drawLabel((int) (92 + 4.5-(this.font.width(Component.literal(this.menu.usedGold.toString())) / 2)), 37, Component.literal(this.menu.usedGold.toString()), guiGraphics);
            drawLabel(112, 19, Component.literal(Component.translatable("screen.anvilcraft.royal_grindstone.remove_curse_number").getString().replace("%i", this.menu.removeCurseNumber.toString())), guiGraphics);
            drawLabel(112, 58, Component.literal(Component.translatable("screen.anvilcraft.royal_grindstone.remove_repair_cost").getString().replace("%i", this.menu.removeRepairCostNumber.toString())), guiGraphics);
        }
    }
    private void drawLabel(int x, int y, Component component, GuiGraphics guiGraphics) {
        int i = (this.width - this.imageWidth - 2) / 2;
        int j = (this.height - this.imageHeight + 23) / 2;
        x = x + i;
        y = y + j;
        guiGraphics.drawString(this.font, component, x + 2, y-10, 8453920);
    }
}
