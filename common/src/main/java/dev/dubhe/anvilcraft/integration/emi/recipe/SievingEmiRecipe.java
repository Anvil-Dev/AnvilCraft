package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;

public class SievingEmiRecipe extends BaseItemEmiRecipe {
    public SievingEmiRecipe(EmiRecipeCategory category, AnvilRecipe recipe) {
        super(category, recipe);
    }

    @Override
    protected void addOutputArrow(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 14, 19, 17, 31);
    }
}
