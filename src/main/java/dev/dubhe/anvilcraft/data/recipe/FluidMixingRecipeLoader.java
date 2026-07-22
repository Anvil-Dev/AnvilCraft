package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

public class FluidMixingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        FluidMixingRecipe.builder(provider.getRegistries().lookupOrThrow(Registries.FLUID))
            .requires(Fluids.WATER, 1000)
            .requires(Fluids.LAVA, 1000)
            .result(Items.OBSIDIAN, 1)
            .save(provider, "obsidian");
    }
}
