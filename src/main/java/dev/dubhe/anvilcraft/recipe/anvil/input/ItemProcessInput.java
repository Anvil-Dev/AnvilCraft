package dev.dubhe.anvilcraft.recipe.anvil.input;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record ItemProcessInput(List<ItemStack> items) implements RecipeInput, IItemsInput {
    @Override
    public ItemStack getItem(int pIndex) {
        return items.get(pIndex);
    }

    @Override
    public int size() {
        return items.size();
    }
}
