package dev.dubhe.anvilcraft.integration.rei.display;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import java.util.List;

public class AnvilProcessDisplay implements Display {
    protected final AnvilRecipe recipe;
    private final List<EntryIngredient> input;
    private final List<EntryIngredient> output;

    /**
     * REI
     */
    public AnvilProcessDisplay(AnvilRecipe recipe, List<EntryIngredient> input, List<EntryIngredient> output) {
        this.recipe = recipe;
        this.input = input;
        this.output = output;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return this.input;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return this.output;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIdentifier.of(AnvilCraft.of(recipe.getAnvilRecipeType().toString()));
    }
}
