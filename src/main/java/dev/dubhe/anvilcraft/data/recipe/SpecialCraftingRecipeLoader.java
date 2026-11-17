package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.CanningFoodRecipe;
import dev.dubhe.anvilcraft.recipe.CelestialAnvilRecipe;
import dev.dubhe.anvilcraft.recipe.PillRecipe;
import net.minecraft.data.recipes.SpecialRecipeBuilder;

public class SpecialCraftingRecipeLoader {

    public static void init(RegistrumRecipeProvider provider) {
        SpecialRecipeBuilder.special(CelestialAnvilRecipe::new)
            .save(provider, AnvilCraft.recipe("celestial_anvil"));
        SpecialRecipeBuilder.special(CanningFoodRecipe::new)
            .save(provider, AnvilCraft.recipe("canned_food"));
        SpecialRecipeBuilder.special(PillRecipe::new)
            .save(provider, AnvilCraft.recipe("pill"));
    }
}
