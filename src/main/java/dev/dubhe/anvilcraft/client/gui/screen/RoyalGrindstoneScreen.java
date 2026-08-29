package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        if (this.menu.getSlot(2).hasItem()) {
            Component removedText = Component.translatable("screen.anvilcraft.royal_grindstone.will_remove");
            this.drawLabel(
                63,
                11,
                removedText,
                graphics
            );
            Component removedRepairCostText = Component.translatable(
                "screen.anvilcraft.royal_grindstone.repair_cost",
                this.menu.removedRepairCost, this.menu.totalRepairCost
            );
            this.drawLabel(
                63,
                22,
                removedRepairCostText,
                graphics
            );
            Component removedCurseCountText = Component.translatable(
                "screen.anvilcraft.royal_grindstone.curse_count",
                this.menu.removedCurseCount, this.menu.totalCurseCount
            );
            this.drawLabel(
                63,
                33,
                removedCurseCountText,
                graphics
            );
            Component usedGoldText = Component.translatable(
                "screen.anvilcraft.royal_grindstone.gold_cost",
                this.menu.usedGold
            );
            this.drawLabel(
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            RoyalGrindstoneScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
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
                var entry = recipes.get(this.recipeIndex);
                displayRepair = entry.getKey().getDefaultInstance();
                displayResult = entry.getValue().item().getDefaultInstance();
            } else {
                var entry = this.getCurrentRecipe(recipes, this.menu.totalRepairCost);
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

        if (!displayRepair.isEmpty()) this.extractMaskedItem(graphics, displayRepair, this.leftPos + 35, this.topPos + 21);
        if (!displayResult.isEmpty()) this.extractMaskedItem(graphics, displayResult, this.leftPos + 35, this.topPos + 45);
    }

    private void extractMaskedItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        final int maskColor = 0x55777777;
        graphics.item(stack, x, y, 0);
        graphics.fill(x, y, x + 16, y + 16, maskColor);
    }

    private Map.Entry<Item, RoyalGrindstoneMenu.RepairCostRecipeEntry> getCurrentRecipe(
        List<Map.Entry<Item, RoyalGrindstoneMenu.RepairCostRecipeEntry>> recipes,
        int repairCost
    ) {
        recipes.sort(Comparator.comparingInt(entry -> entry.getValue().count()));
        this.recipeIndex = this.recipeIndex % recipes.size();
        int checked = 0;
        while (checked < recipes.size()) {
            Map.Entry<Item, RoyalGrindstoneMenu.RepairCostRecipeEntry> candidate = recipes.get(this.recipeIndex);
            int requiredCost = candidate.getValue().count();
            if (requiredCost <= repairCost) return candidate;
            this.recipeIndex = (this.recipeIndex + 1) % recipes.size();
            checked++;
        }
        return Map.entry(
            RoyalGrindstoneMenu.DEFAULT_REPAIR_MATERIAL,
            RoyalGrindstoneMenu.REPAIR_COST_RECIPES.get(RoyalGrindstoneMenu.DEFAULT_REPAIR_MATERIAL)
        );
    }

    @SuppressWarnings("SameParameterValue")
    private void drawLabel(int x, int y, Component component, GuiGraphicsExtractor graphics) {
        graphics.text(
            this.font,
            component,
            x + 2,
            y + 2,
            0xFF80FF20
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.tickCounter++;
        if (this.tickCounter % (20 * 3) == 0) this.recipeIndex = (this.recipeIndex + 1) % RoyalGrindstoneMenu.REPAIR_COST_RECIPES.size();
    }
}
