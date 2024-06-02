package dev.dubhe.anvilcraft.data.recipe.anvil;

import java.util.Arrays;

public enum AnvilRecipeType {
    NULL("null"),
    STAMPING("stamping");

    private final String type;

    AnvilRecipeType(String type) {
        this.type = type;
    }

    public String toString() {
        return this.type;
    }
}
