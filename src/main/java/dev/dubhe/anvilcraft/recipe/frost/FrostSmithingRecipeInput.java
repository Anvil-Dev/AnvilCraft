package dev.dubhe.anvilcraft.recipe.frost;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record FrostSmithingRecipeInput(ItemStack template, ItemStack material, ItemStack input) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> this.template;
            case 1 -> this.material;
            case 2 -> this.input;
            default -> throw new IllegalArgumentException("Recipe does not contain slot " + index);
        };
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return this.template.isEmpty() && this.material.isEmpty() && this.input.isEmpty();
    }
}
