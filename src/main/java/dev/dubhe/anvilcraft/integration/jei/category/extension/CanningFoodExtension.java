package dev.dubhe.anvilcraft.integration.jei.category.extension;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.CanningFoodRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CanningFoodExtension implements ICraftingCategoryExtension<CanningFoodRecipe> {

    public static CanningFoodExtension INSTANCE = new CanningFoodExtension();

    @Override
    public void setRecipe(
        RecipeHolder<CanningFoodRecipe> recipeHolder,
        IRecipeLayoutBuilder builder,
        ICraftingGridHelper craftingGridHelper,
        IFocusGroup focuses) {
        CanningFoodRecipe recipe = recipeHolder.value();
        craftingGridHelper.createAndSetIngredients(builder, List.of(
            Ingredient.of(ModItems.TIN_CAN),
            Ingredient.of(RegistryUtil.getRegistry(Registries.ITEM)
                .getTag(Tags.Items.FOODS)
                .orElseThrow()
                .stream()
                .map(Holder::value)
                .map(Item::getDefaultInstance)
                .filter(recipe::isValidFood)
            )
        ), 0, 0);
        craftingGridHelper.createAndSetOutputs(builder, List.of(ModItems.CANNED_FOOD.asStack()));
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
            .filter(recipe::isValidFood)
            .findFirst()
            .orElse(ItemStack.EMPTY);
        if (displayedFood.isEmpty()) return;
        recipeSlots.stream()
            .filter(slot -> slot.getRole() == RecipeIngredientRole.OUTPUT)
            .forEach(slot -> {
                if (slot.getRole() == RecipeIngredientRole.OUTPUT) {
                    slot.getDisplayedItemStack().ifPresent(canStack -> slot.createDisplayOverrides()
                        .addItemStack(ModItems.CANNED_FOOD.get().setFood(canStack, displayedFood)));
                }
            });
    }
}
