package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.emi.AnvilCraftEmiRecipeTypes;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public class StampingEmiRecipes extends BaseItemEmiRecipe {
    public StampingEmiRecipes(AnvilRecipe recipe) {
        super(AnvilCraftEmiRecipeTypes.STAMPING, recipe);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        super.addWidgets(widgets);
        widgets.addDrawable(93, 39, 0, 0, new BlockWidget(
                new BlockState[]{ANVIL_BLOCK_STATE, ModBlocks.STAMPING_PLATFORM.getDefaultState()},
                new Vec2[]{Vec2.ZERO, new Vec2(0, 2)}
        ));
        widgets.addDrawable(128, 39, 0, 0, new BlockWidget(
                new BlockState[]{ANVIL_BLOCK_STATE, ModBlocks.STAMPING_PLATFORM.getDefaultState()},
                new Vec2[]{Vec2.ZERO, new Vec2(0, 1)}
        ));
    }
}
