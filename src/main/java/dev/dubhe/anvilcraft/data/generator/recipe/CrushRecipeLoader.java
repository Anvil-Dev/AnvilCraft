package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.CrushRecipe;
import net.minecraft.world.level.block.Blocks;

public class CrushRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        CrushRecipe.builder()
                .input(Blocks.STONE)
                .result(Blocks.COBBLESTONE)
                .save(provider, AnvilCraft.of("crush/cobblestone"));
    }
}
