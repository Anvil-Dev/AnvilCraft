package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.PillRecipe;
import net.minecraft.data.recipes.SpecialRecipeBuilder;

public class PillRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        SpecialRecipeBuilder.special(PillRecipe::new)
            .save(provider, AnvilCraft.of("pill"));
    }
}
