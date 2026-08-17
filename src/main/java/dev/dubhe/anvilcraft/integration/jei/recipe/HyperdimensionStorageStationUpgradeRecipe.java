package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;

public record HyperdimensionStorageStationUpgradeRecipe() {
    public static ImmutableList<HyperdimensionStorageStationUpgradeRecipe> getAllRecipes() {
        return ImmutableList.of(new HyperdimensionStorageStationUpgradeRecipe());
    }
}
