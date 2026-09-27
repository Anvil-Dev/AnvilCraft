package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.ComplexFluidJeiRecipe;
import mezz.jei.common.Internal;

import java.util.List;

public final class EnchantedGoldJeiProbe {
    public static void verify() {
        var manager = Internal.getJeiRuntime().getRecipeManager();
        var holder = manager.createRecipeLookup(AnvilCraftJeiPlugin.FLUID_MIXING).get()
            .filter(recipe -> recipe.id().identifier().equals(AnvilCraft.of("jei/solid_liquid/enchanted_gold_ingot")))
            .findFirst().orElseThrow();
        var recipe = (ComplexFluidJeiRecipe) holder.value();
        var inputs = recipe.getDisplayFluidInputs();
        if (inputs.size() != 1 || !inputs.getFirst().stream().map(stack -> stack.getAmount()).toList().equals(List.of(16, 4, 1))
            || recipe.getDisplayItemResults().size() != 1 || recipe.isHeaterRequired()) {
            throw new IllegalStateException("Enchanted gold JEI alternatives differ from source");
        }
        AnvilCraft.LOGGER.info("PORT_ENCHANTED_GOLD_JEI_PASSED");
    }
}
