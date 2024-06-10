package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;

public class CookingEmiRecipe extends BaseItemEmiRecipe {
    /**
     * 烹饪配方
     *
     * @param category 配方类型
     * @param recipe {@link AnvilRecipe}配方
     */
    public CookingEmiRecipe(EmiRecipeCategory category, AnvilRecipe recipe) {
        super(category, recipe);
        this.workBlockStates.add(Blocks.CAMPFIRE.defaultBlockState());
        this.workBlockPoses.add(new Vec2(0, -2));
        this.outputWorkBlockPoses.add(new Vec2(0, -2));
        this.height = 99;
    }
}
