package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class RoyalGrindstoneScreen extends AbstractContainerScreen<RoyalGrindstoneMenu> {
    private static final ResourceLocation GRINDSTONE_LOCATION =
        AnvilCraft.of("textures/gui/container/smithing/background/royal_grindstone.png");

    public RoyalGrindstoneScreen(
        RoyalGrindstoneMenu menu, Inventory playerInventory, @SuppressWarnings("unused") Component title
    ) {
        super(menu, playerInventory, Component.translatable("screen.anvilcraft.royal_grindstone.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderLabels(guiGraphics);
    }

    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GRINDSTONE_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    protected void renderLabels(GuiGraphics guiGraphics) {
        if (this.menu.getSlot(2).hasItem()) {
            drawLabel((int) (92 + 4.5 - (this.font.width(Component.literal("" + this.menu.usedGold)) / 2)), 38,
                Component.literal("" + this.menu.usedGold), guiGraphics);
            drawLabel(112, 19, Component.literal(
                Component.translatable("screen.anvilcraft.royal_grindstone.remove_curse_number")
                    .getString().replace("%i", "" + this.menu.removeCurseNumber)), guiGraphics);
            drawLabel(112, 58, Component.literal(
                Component.translatable("screen.anvilcraft.royal_grindstone.remove_repair_cost")
                    .getString().replace("%i", "" + this.menu.removeRepairCostNumber)), guiGraphics);
        }
    }

    private void drawLabel(int x, int y, Component component, @NotNull GuiGraphics guiGraphics) {
        int i = (this.width - this.imageWidth - 2) / 2;
        int j = (this.height - this.imageHeight + 23) / 2;
        x = x + i;
        y = y + j;
        guiGraphics.drawString(this.font, component, x + 2, y - 10, 8453920);
    }
}
