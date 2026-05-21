package dev.dubhe.anvilcraft.integration.jei.category.extension;

import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.CanningFoodRecipe;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.Tags;

import java.util.List;
import java.util.Optional;

public class CanningFoodExtension implements ICraftingCategoryExtension<CanningFoodRecipe> {
    public static CanningFoodExtension INSTANCE = new CanningFoodExtension();

    @Override
    public List<SlotDisplay> getIngredients(RecipeHolder<CanningFoodRecipe> recipeHolder) {
        return List.of(
            new SlotDisplay.ItemSlotDisplay(ModItems.TIN_CAN),
            new SlotDisplay.TagSlotDisplay(Tags.Items.FOODS)
        );
    }

    @Override
    public void onDisplayedIngredientsUpdate(
        RecipeHolder<CanningFoodRecipe> recipeHolder,
        List<IRecipeSlotDrawable> recipeSlots,
        IFocusGroup focuses) {
        CanningFoodRecipe recipe = recipeHolder.value();
        ItemStack displayedFood = recipeSlots.stream()
            .filter(slot -> slot.getRole() == RecipeIngredientRole.INPUT)
            .map(IRecipeSlotView::getDisplayedItemStack)
            .flatMap(Optional::stream)
            .filter(recipe::isFood)
            .findFirst()
            .orElse(ItemStack.EMPTY);
        if (displayedFood.isEmpty()) return;
        recipeSlots.stream()
            .filter(slot -> slot.getRole() == RecipeIngredientRole.OUTPUT)
            .forEach(slot -> {
                if (slot.getRole() == RecipeIngredientRole.OUTPUT) {
                    slot.getDisplayedItemStack().ifPresent(canStack -> slot.createDisplayOverrides()
                        .add(ModFoodItems.CANNED_FOOD.get().setFood(canStack, displayedFood)));
                }
            });
    }
}
