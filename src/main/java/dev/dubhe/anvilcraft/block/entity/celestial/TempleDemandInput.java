package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** {@link TempleDemandRecipe#matches} 使用的空输入包装。 */
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
