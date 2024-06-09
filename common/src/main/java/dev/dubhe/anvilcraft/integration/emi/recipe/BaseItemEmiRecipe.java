package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.WidgetHolder;

public abstract class BaseItemEmiRecipe extends BaseEmiRecipe {
    public BaseItemEmiRecipe(EmiRecipeCategory category,
        AnvilRecipe recipe) {
        super(category, recipe, 240, 80);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        addBendArrow(widgets, 75, 34);
        addStraightArrow(widgets, 119, 34);
        addPlus(widgets, 154, 34);
        addInputSlots(inputs, widgets);
        addOutputs(outputs, widgets);
    }
}
