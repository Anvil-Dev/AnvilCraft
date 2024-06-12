package dev.dubhe.anvilcraft.api.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AnvilRecipeManager {
    @Getter
    private static List<AnvilRecipe> anvilRecipeList = List.of();

    /**
     * 更新配方
     *
     * @param server 服务器实例
     */
    public static void updateRecipes(@NotNull MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        ArrayList<AnvilRecipe> anvilRecipes =
                new ArrayList<>(manager.getAllRecipesFor(ModRecipeTypes.ANVIL_RECIPE));
        List<ItemStack> needFilter = new ArrayList<>();
        for (CampfireCookingRecipe recipe : manager.getAllRecipesFor(RecipeType.CAMPFIRE_COOKING)) {
            AnvilRecipe anvilRecipe = AnvilRecipe.of(recipe, server.registryAccess());
            if (anvilRecipe != null) anvilRecipes.add(anvilRecipe);
        }
        for (SmokingRecipe recipe : manager.getAllRecipesFor(RecipeType.SMOKING)) {
            needFilter.add(recipe.getResultItem(server.registryAccess()));
            AnvilRecipe anvilRecipe = AnvilRecipe.of(recipe, server.registryAccess());
            if (anvilRecipe != null) anvilRecipes.add(anvilRecipe);
        }
        for (BlastingRecipe recipe : manager.getAllRecipesFor(RecipeType.BLASTING)) {
            needFilter.add(recipe.getResultItem(server.registryAccess()));
            AnvilRecipe anvilRecipe = AnvilRecipe.of(recipe, server.registryAccess());
            if (anvilRecipe != null) anvilRecipes.add(anvilRecipe);
        }
        for (SmeltingRecipe recipe : manager.getAllRecipesFor(RecipeType.SMELTING)) {
            ItemStack item = recipe.getResultItem(server.registryAccess());
            if (needFilter.stream().anyMatch(stack -> item.is(stack.getItem()))) continue;
            AnvilRecipe anvilRecipe = AnvilRecipe.of(recipe, server.registryAccess());
            if (anvilRecipe != null) anvilRecipes.add(anvilRecipe);
        }
        for (CraftingRecipe recipe : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
            AnvilRecipe anvilRecipe = AnvilRecipe.of(recipe, server.registryAccess());
            if (anvilRecipe != null) anvilRecipes.add(anvilRecipe);
        }
        anvilRecipeList = Collections.synchronizedList(anvilRecipes.stream()
                .sorted(Comparator.comparing(AnvilRecipe::getWeightCoefficient).reversed())
                .toList());
    }
}
