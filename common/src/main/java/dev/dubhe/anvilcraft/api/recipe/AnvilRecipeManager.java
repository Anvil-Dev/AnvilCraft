package dev.dubhe.anvilcraft.api.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AnvilRecipeManager {
    private static List<AnvilRecipe> ANVIL_RECIPE_SET = List.of();

    /**
     * 更新配方
     *
     * @param server 服务器实例
     */
    public static void updateRecipes(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        ArrayList<AnvilRecipe> anvilRecipes = new ArrayList<>(manager.getAllRecipesFor(ModRecipeTypes.ANVIL_RECIPE));
        ArrayList<AnvilRecipe> smokingRecipes = new ArrayList<>();
        ArrayList<AnvilRecipe> smeltingRecipes = new ArrayList<>();
        manager.getAllRecipesFor(RecipeType.SMOKING).forEach(
                smokingRecipe -> smokingRecipes.addAll(AnvilRecipe.of(smokingRecipe, server.registryAccess())));
        manager.getAllRecipesFor(RecipeType.CAMPFIRE_COOKING).forEach(
                campfireCookingRecipe ->
                        anvilRecipes.addAll(AnvilRecipe.of(campfireCookingRecipe, server.registryAccess())));
        manager.getAllRecipesFor(RecipeType.BLASTING).forEach(
                blastingRecipe ->
                        anvilRecipes.addAll(AnvilRecipe.of(blastingRecipe, server.registryAccess())));
        manager.getAllRecipesFor(RecipeType.SMELTING).forEach(
                smeltingRecipe -> {
                    List<AnvilRecipe> anvilRecipe =
                            AnvilRecipe.of(smeltingRecipe, server.registryAccess());
                    if (!anvilRecipe.isEmpty()
                            && smokingRecipes.stream().noneMatch((smokingRecipe) ->
                            anvilRecipe.get(0).getResultItem(server.registryAccess())
                                    .is(smokingRecipe.getResultItem(server.registryAccess()).getItem())))
                        smeltingRecipes.addAll(anvilRecipe);
                });
        manager.getAllRecipesFor(RecipeType.CRAFTING).forEach(
                craftingRecipe ->
                        anvilRecipes.addAll(AnvilRecipe.of(craftingRecipe, server.registryAccess())));
        anvilRecipes.addAll(smokingRecipes);
        anvilRecipes.addAll(smeltingRecipes);

        ANVIL_RECIPE_SET = anvilRecipes
                .stream()
                .sorted(Comparator.comparing(AnvilRecipe::getWeightCoefficient).reversed())
                .toList();
    }

    public static List<AnvilRecipe> getAnvilRecipeList() {
        return ANVIL_RECIPE_SET;
    }
}
