package dev.dubhe.anvilcraft.api.recipe;

import dev.dubhe.anvilcraft.api.event.server.ServerEvent;
import dev.dubhe.anvilcraft.block.state.Half;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LaseRenderer;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.List;

public class AnvilRecipeManager {
    public final static HashMap ANVIL_RECIPE_MAP = new HashMap<>();

    public static void updateRecipes(MinecraftServer server) {
        List<AnvilRecipe> anvilRecipes = server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.ANVIL_RECIPE);

    }
}
