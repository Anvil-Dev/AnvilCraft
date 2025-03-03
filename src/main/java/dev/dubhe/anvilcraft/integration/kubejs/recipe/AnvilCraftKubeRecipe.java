package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.rhino.util.HideFromJS;

import java.util.function.Supplier;

public abstract class AnvilCraftKubeRecipe extends KubeRecipe {
    @HideFromJS
    public <T> T computeIfAbsent(RecipeKey<T> key, Supplier<T> supplier) {
        if (getValue(key) == null) {
            setValue(key, supplier.get());
        }
        return getValue(key);
    }

    protected abstract void validate();

    @Override
    public void afterLoaded() {
        super.afterLoaded();

        this.validate();
    }
}
