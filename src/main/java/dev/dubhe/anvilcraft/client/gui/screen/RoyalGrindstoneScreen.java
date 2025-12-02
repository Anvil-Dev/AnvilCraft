package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RoyalGrindstoneScreen extends AbstractContainerScreen<RoyalGrindstoneMenu> {
    private int tickCounter = 0;
    private int recipeIndex = 0;

    private static final ResourceLocation GRINDSTONE_LOCATION =
        AnvilCraft.of("textures/gui/container/smithing/background/royal_grindstone.png");

    public RoyalGrindstoneScreen(
        RoyalGrindstoneMenu menu, Inventory playerInventory, @SuppressWarnings("unused") Component title) {
        super(menu, playerInventory, Component.translatable("screen.anvilcraft.royal_grindstone.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderLabels(guiGraphics);
    }

    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        g.blit(GRINDSTONE_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
        g.setColor(1f, 1f, 1f, 1);
        ItemStack repairToolItem = this.menu.getSlot(0).getItem();
        ItemStack repairItem = this.menu.getSlot(1).getItem();
        ItemStack resultItem = this.menu.getSlot(3).getItem();
        List<Map.Entry<Item, RoyalGrindstoneMenu.RepairCostRecipeEntry>> recipes =
            new ArrayList<>(RoyalGrindstoneMenu.REPAIR_COST_RECIPES.entrySet());

        ItemStack displayRepair = ItemStack.EMPTY;
        ItemStack displayResult = ItemStack.EMPTY;
        if (repairItem.isEmpty() && resultItem.isEmpty()) {
            if (this.menu.totalCurseCount > 0 && this.menu.totalRepairCost <= 0) {
                displayRepair = RoyalGrindstoneMenu.DEFAULT_REPAIR_MATERIAL.getDefaultInstance();
                displayResult = RoyalGrindstoneMenu.REPAIR_COST_RECIPES
                    .get(RoyalGrindstoneMenu.DEFAULT_REPAIR_MATERIAL)
                    .item().getDefaultInstance();
            } else if (repairToolItem.isEmpty()) {
                var entry = recipes.get(recipeIndex);
                displayRepair = entry.getKey().getDefaultInstance();
                displayResult = entry.getValue().item().getDefaultInstance();
            } else {
                var entry = getCurrentRecipe(recipes, this.menu.totalRepairCost);
                displayRepair = entry.getKey().getDefaultInstance();
                displayResult = entry.getValue().item().getDefaultInstance();
            }
        } else if (resultItem.isEmpty()) {
            displayResult = RoyalGrindstoneMenu.REPAIR_COST_RECIPES.get(repairItem.getItem()).item().getDefaultInstance();
        } else if (repairItem.isEmpty()) {
            Item repair = RoyalGrindstoneMenu.REPAIR_COST_RECIPES.entrySet().stream()
                .filter(e -> e.getValue().item() == resultItem.getItem())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(RoyalGrindstoneMenu.DEFAULT_REPAIR_MATERIAL);
            displayRepair = repair.getDefaultInstance();
        }

        if (!displayRepair.isEmpty()) renderMaskedItem(g, displayRepair, i + 89, j + 22);
        if (!displayResult.isEmpty()) renderMaskedItem(g, displayResult, i + 89, j + 47);
    }

    private void renderMaskedItem(GuiGraphics g, ItemStack stack, int x, int y) {
        final int maskColor = 0x55777777;
        g.renderItem(stack, x, y, 0);
        g.fill(RenderType.guiOverlay(), x, y, x + 16, y + 16, maskColor);
    }

    private Map.Entry<Item, RoyalGrindstoneMenu.RepairCostRecipeEntry> getCurrentRecipe(
        List<Map.Entry<Item, RoyalGrindstoneMenu.RepairCostRecipeEntry>> recipes,
        int repairCost
    ) {
        recipes.sort(Comparator.comparingInt(entry -> entry.getValue().count()));
        recipeIndex = recipeIndex % recipes.size();
        int checked = 0;
        while (checked < recipes.size()) {
            Map.Entry<Item, RoyalGrindstoneMenu.RepairCostRecipeEntry> candidate = recipes.get(recipeIndex);
            int requiredCost = candidate.getValue().count();
            if (requiredCost <= repairCost) return candidate;
            recipeIndex = (recipeIndex + 1) % recipes.size();
            checked++;
        }
        return Map.entry(
            RoyalGrindstoneMenu.DEFAULT_REPAIR_MATERIAL,
            RoyalGrindstoneMenu.REPAIR_COST_RECIPES.get(RoyalGrindstoneMenu.DEFAULT_REPAIR_MATERIAL)
        );
    }

    protected void renderLabels(GuiGraphics guiGraphics) {
        if (this.menu.getSlot(2).hasItem()) {
            Component usedGoldText = Component.literal("" + this.menu.usedGold);
            Component removedCurseCountText = Component.translatable(
                "screen.anvilcraft.royal_grindstone.remove_curse_count",
                this.menu.removedCurseCount, this.menu.totalCurseCount);
            Component removedRepairCostText = Component.translatable(
                "screen.anvilcraft.royal_grindstone.remove_repair_cost",
                this.menu.removedRepairCost, this.menu.totalRepairCost);
            drawLabel(
                (int) (92 + 4.5 - (this.font.width(usedGoldText) / 2f)),
                38,
                usedGoldText,
                guiGraphics);
            drawLabel(
                170 - this.font.width(removedCurseCountText),
                13,
                removedCurseCountText,
                guiGraphics);
            drawLabel(
                170 - this.font.width(removedRepairCostText),
                58,
                removedRepairCostText,
                guiGraphics);
        }
    }

    private void drawLabel(int x, int y, Component component, @NotNull GuiGraphics guiGraphics) {
        int i = (this.width - this.imageWidth - 2) / 2;
        int j = (this.height - this.imageHeight + 23) / 2;
        x += i;
        y += j;
        guiGraphics.drawString(this.font, component, x + 2, y - 10, 8453920);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickCounter++;
        if (tickCounter % (20 * 3) == 0) recipeIndex = (recipeIndex + 1) % RoyalGrindstoneMenu.REPAIR_COST_RECIPES.size();
    }
}
