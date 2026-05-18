package dev.dubhe.anvilcraft.inventory.component.jewel;

import dev.dubhe.anvilcraft.inventory.container.JewelSourceContainer;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import lombok.Getter;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

public class JewelInputSlot extends Slot {
    private final JewelSourceContainer sourceContainer;
    @Getter
    @Nullable
    private Ingredient ingredient;
    @Getter
    private ItemStack @Nullable [] ingredientItems;
    @Getter
    private int hintCount;

    public JewelInputSlot(JewelSourceContainer sourceContainer, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.sourceContainer = sourceContainer;

        this.updateIngredient();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (this.ingredient == null) {
            return false;
        }
        if (!this.ingredient.test(stack)) {
            return false;
        }
        return super.mayPlace(stack);
    }

    public void updateIngredient() {
        RecipeHolder<JewelCraftingRecipe> recipe = this.sourceContainer.getRecipe();
        if (recipe != null) {
            var mergedIngredients = this.sourceContainer.getRecipe().value().mergedIngredients;
            if (getSlotIndex() > mergedIngredients.size() - 1) {
                this.ingredient = null;
                this.ingredientItems = null;
            } else {
                var entry = mergedIngredients.get(getSlotIndex());
                this.ingredient = entry.getKey();
                this.ingredientItems = this.ingredient.items()
                    .map(holder -> holder.value().getDefaultInstance())
                    .toArray(ItemStack[]::new);
                this.hintCount = entry.getIntValue();
            }
        } else {
            this.ingredient = null;
            this.ingredientItems = null;
        }
    }
}
