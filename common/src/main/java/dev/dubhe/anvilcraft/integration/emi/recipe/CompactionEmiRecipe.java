package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.phys.Vec2;

public class CompactionEmiRecipe extends BaseBlockEmiRecipe {
    /**
     * 基础压合配方
     *
     * @param category 配方类型
     * @param recipe {@link AnvilRecipe}配方
     */
    public CompactionEmiRecipe(EmiRecipeCategory category, AnvilRecipe recipe) {
        super(category, recipe);
        outPutBlockPoses.remove(0);
        outPutBlockPoses.add(0, new Vec2(0, -1));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        addStraightArrow(widgets, 120, 41);
        widgets.addDrawable(90, 20, 0, 0, new BlockWidget(
                inputBlockStates,
                inputBlockPoses
        ));
        widgets.addDrawable(135, 20, 0, 0, new BlockWidget(
                outPutBlockStates,
                outPutBlockPoses)
        );
    }
}
