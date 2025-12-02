package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.RecipeValidationContext;
import dev.latvian.mods.rhino.util.HideFromJS;

import java.util.function.Supplier;

public abstract class AnvilCraftKubeRecipe extends KubeRecipe {
    @HideFromJS
    public <T> T computeIfAbsent(RecipeKey<T> key, Supplier<T> supplier) {
        T value = getValue(key);
        if (value == null) {
            T t = supplier.get();
            setValue(key, t);
            return t;
        }
        return value;
    }

    protected abstract void validate();

    @Override
    public void validate(RecipeValidationContext cx) {
        super.validate(cx);
        this.validate();
    }
}
