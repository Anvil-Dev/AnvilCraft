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

import java.util.List;

public class JewelInputSlot extends Slot {
    private final JewelSourceContainer sourceContainer;
    @Getter
    @Nullable
    private Ingredient ingredient;
    @Getter
    @Nullable
    private List<ItemStack> ingredientItems;
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
            var ingredients = recipe.value().mergedIngredients();
            if (this.getSlotIndex() > ingredients.size() - 1) {
                this.ingredient = null;
                this.ingredientItems = null;
            } else {
                var entry = ingredients.get(this.getSlotIndex());
                this.ingredient = entry.getKey();
                this.ingredientItems = entry.getKey().getValues().stream().map(holder -> holder.value().getDefaultInstance()).toList();
                this.hintCount = entry.getIntValue();
            }
        } else {
            this.ingredient = null;
            this.ingredientItems = null;
        }
    }
}
