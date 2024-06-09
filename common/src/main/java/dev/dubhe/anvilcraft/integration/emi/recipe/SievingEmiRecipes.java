package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.emi.AnvilCraftEmiRecipeTypes;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public class SievingEmiRecipes extends BaseItemEmiRecipe {
    public SievingEmiRecipes(AnvilRecipe recipe) {
        super(AnvilCraftEmiRecipeTypes.SIEVING, recipe);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        super.addWidgets(widgets);
        widgets.addDrawable(93, 39, 0, 0, new BlockWidget(
                new BlockState[]{ANVIL_BLOCK_STATE, Blocks.SCAFFOLDING.defaultBlockState()},
                new Vec2[]{new Vec2(0, 2), Vec2.ZERO}
        ));
        widgets.addDrawable(128, 39, 0, 0, new BlockWidget(
                new BlockState[]{ANVIL_BLOCK_STATE, Blocks.SCAFFOLDING.defaultBlockState()},
                new Vec2[]{new Vec2(0, 1), Vec2.ZERO}
        ));
    }
}
