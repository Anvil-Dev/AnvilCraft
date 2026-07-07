package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Input wrapper for {@link TempleDemandRecipe#matches}.
 */
public record TempleDemandInput(TempleDemandRecipe.Category category) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
