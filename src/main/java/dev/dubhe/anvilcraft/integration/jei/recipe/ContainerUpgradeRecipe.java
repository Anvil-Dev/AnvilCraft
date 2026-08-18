package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;

public record ContainerUpgradeRecipe(Type type) {

    public enum Type {
        CRATE_TO_CONTAINER,
        CONTAINER_TO_STATION
    }

    public static ImmutableList<ContainerUpgradeRecipe> getAllRecipes() {
        return ImmutableList.of(
            new ContainerUpgradeRecipe(Type.CRATE_TO_CONTAINER),
            new ContainerUpgradeRecipe(Type.CONTAINER_TO_STATION)
        );
    }
}
