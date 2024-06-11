package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

public class ItemInjectRecipe extends BaseItemEmiRecipe {
    public ItemInjectRecipe(EmiRecipeCategory category, AnvilRecipe recipe) {
        super(category, recipe);
        isOutputBlock = false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        addInputArrow(widgets, 72, 32);
        addStraightArrow(widgets, 120, 41);
        addInputSlots(widgets);
        widgets.addDrawable(90, 25, 0, 0, new BlockWidget(
                workBlockStates,
                workBlockPoses
        ));
        widgets.addDrawable(135, 25, 0, 0, new BlockWidget(
                List.of(ANVIL_BLOCK_STATE, outputBlockState),
                outputWorkBlockPoses)
        );
    }
}
