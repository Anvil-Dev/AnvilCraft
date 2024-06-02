package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.emi.AnvilCraftEmiRecipeTypes;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;

public class StampingEmiRecipes extends BaseItemEmiRecipe {
    public StampingEmiRecipes(AnvilRecipe recipe) {
        super(AnvilCraftEmiRecipeTypes.STAMPING, recipe);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        super.addWidgets(widgets);
        addAnvil(widgets, 93, 39, -2);
        addAnvil(widgets, 128, 39, -15);
        addBlock(widgets, 93, 39, 0, ModBlocks.STAMPING_PLATFORM.getDefaultState());
        addBlock(widgets, 128, 39, 0, ModBlocks.STAMPING_PLATFORM.getDefaultState());
    }
}
