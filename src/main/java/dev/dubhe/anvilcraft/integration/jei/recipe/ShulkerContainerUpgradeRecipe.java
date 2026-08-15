package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;

public record ShulkerContainerUpgradeRecipe() {
    public static ImmutableList<ShulkerContainerUpgradeRecipe> getAllRecipes() {
        return ImmutableList.of(new ShulkerContainerUpgradeRecipe());
    }
}