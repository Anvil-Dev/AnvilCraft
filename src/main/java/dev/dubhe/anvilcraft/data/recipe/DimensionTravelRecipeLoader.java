package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.worldgen.ModDimensionTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.DimensionTravelRecipe;
import net.minecraft.world.level.Level;

public class DimensionTravelRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        DimensionTravelRecipe.builder()
            .from(Level.OVERWORLD)
            .speed(10)
            .height(349)
            .to(ModDimensionTypes.MUN_LEVEL)
            .save(provider);
    }
}