package dev.dubhe.anvilcraft.recipe.frost;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 浮霜锻造的输入：模板槽、装备槽与材料槽。
 */
public record FrostSmithingRecipeInput(ItemStack template, ItemStack input, ItemStack material) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case IFrostSmithingRecipe.TEMPLATE_SLOT -> this.template;
            case IFrostSmithingRecipe.INPUT_SLOT -> this.input;
            case IFrostSmithingRecipe.MATERIAL_SLOT -> this.material;
            default -> throw new IllegalArgumentException("Recipe does not contain slot " + index);
        };
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return this.template.isEmpty() && this.input.isEmpty() && this.material.isEmpty();
    }
}
