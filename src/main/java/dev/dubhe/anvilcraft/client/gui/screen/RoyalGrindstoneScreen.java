package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RoyalGrindstoneScreen extends AbstractContainerScreen<RoyalGrindstoneMenu> {
    private int tickCounter = 0;
    private int recipeIndex = 0;

    private static final Identifier BACKGROUND = SharedTextures.bg("crafting", "royal_grindstone");

    public RoyalGrindstoneScreen(
        RoyalGrindstoneMenu menu, Inventory playerInventory, @SuppressWarnings("unused") Component title) {
        super(menu, playerInventory, Component.translatable("screen.anvilcraft.royal_grindstone.title"));
    }

    @Override
    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        if (this.menu.getSlot(2).hasItem()) {
            Component removedText = Component.translatable("screen.anvilcraft.royal_grindstone.will_remove");
            drawLabel(
                63,
                11,
                removedText,
                graphics
            );
            Component removedRepairCostText = Component.translatable(
                "screen.anvilcraft.royal_grindstone.repair_cost",
                this.menu.removedRepairCost, this.menu.totalRepairCost
            );
            drawLabel(
                63,
                22,
                removedRepairCostText,
                graphics
            );
            Component removedCurseCountText = Component.translatable(
                "screen.anvilcraft.royal_grindstone.curse_count",
                this.menu.removedCurseCount, this.menu.totalCurseCount
            );
            drawLabel(
                63,
                33,
                removedCurseCountText,
                graphics
            );
            Component usedGoldText = Component.translatable(
                "screen.anvilcraft.royal_grindstone.gold_cost",
                this.menu.usedGold
            );
            drawLabel(
                63,
                44,
                usedGoldText,
                graphics
            );
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    protected void renderBg(GuiGraphicsExtractor g, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.getImageWidth()) / 2;
        int j = (this.height - this.getImageHeight()) / 2;
        g.blit(BACKGROUND, i, j, 0, 0, this.getImageWidth(), this.getImageHeight());
        g.setColor(1F, 1F, 1F, 1);
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

        if (!displayRepair.isEmpty()) renderMaskedItem(g, displayRepair, i + 35, j + 21);
        if (!displayResult.isEmpty()) renderMaskedItem(g, displayResult, i + 35, j + 45);
    }

    private void renderMaskedItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
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

    private void drawLabel(int x, int y, Component component, GuiGraphicsExtractor graphics) {
        graphics.drawString(
            this.font,
            component,
            x + 2,
            y + 2,
            8453920
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickCounter++;
        if (tickCounter % (20 * 3) == 0) recipeIndex = (recipeIndex + 1) % RoyalGrindstoneMenu.REPAIR_COST_RECIPES.size();
    }
}
