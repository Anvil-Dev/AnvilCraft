package dev.dubhe.anvilcraft.inventory.component.jewel;

import dev.dubhe.anvilcraft.inventory.container.JewelSourceContainer;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import lombok.Getter;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

public class JewelInputSlot extends Slot {
    private final JewelSourceContainer sourceContainer;
    @Getter
    @Nullable
    private Ingredient ingredient;
    @Getter
    private ItemStack @Nullable [] ingredientItems;

    public JewelInputSlot(JewelSourceContainer sourceContainer, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.sourceContainer = sourceContainer;

        updateIngredient();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (ingredient == null) {
            return false;
        }
        if (!ingredient.test(stack)) {
            return false;
        }
        return super.mayPlace(stack);
    }

    public void updateIngredient() {
        RecipeHolder<JewelCraftingRecipe> recipe = sourceContainer.getRecipe();
        if (recipe != null) {
            var mergedIngredients = sourceContainer.getRecipe().value().mergedIngredients;
            if (getSlotIndex() > mergedIngredients.size() - 1) {
                ingredient = null;
                ingredientItems = null;
            } else {
                ingredient = mergedIngredients.get(getSlotIndex()).getKey();
                ingredientItems = ingredient.getItems();
            }
        } else {
            ingredient = null;
            ingredientItems = null;
        }
    }
}
