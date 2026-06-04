package dev.dubhe.anvilcraft.integration.jei.category.extension;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.recipe.PillRecipe;
import dev.dubhe.anvilcraft.recipe.display.slot.WithAnyPotionsExcept;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.List;
import java.util.Optional;

public class PillRecipeExtension implements ICraftingCategoryExtension<PillRecipe> {
    @Override
    public List<SlotDisplay> getIngredients(RecipeHolder<PillRecipe> recipeHolder) {
        return List.of(
            new WithAnyPotionsExcept(
                new SlotDisplay.Composite(List.of(
                    new SlotDisplay.ItemSlotDisplay(Items.POTION),
                    new SlotDisplay.ItemSlotDisplay(Items.SPLASH_POTION),
                    new SlotDisplay.ItemSlotDisplay(Items.LINGERING_POTION)
                )),
                List.of(
                    Potions.WATER.key().identifier(),
                    Potions.MUNDANE.key().identifier(),
                    Potions.THICK.key().identifier(),
                    Potions.AWKWARD.key().identifier()
                )
            )
        );
    }

    @Override
    public void onDisplayedIngredientsUpdate(
        RecipeHolder<PillRecipe> recipeHolder,
        List<IRecipeSlotDrawable> recipeSlots,
        IFocusGroup focuses
    ) {
        PillRecipe recipe = recipeHolder.value();
        ItemStack itemStack = recipeSlots.stream()
            .filter(slot -> slot.getRole() == RecipeIngredientRole.INPUT)
            .map(IRecipeSlotView::getDisplayedItemStack)
            .flatMap(Optional::stream)
            .filter(recipe::validatePotion)
            .findFirst()
            .orElse(ItemStack.EMPTY);
        if (itemStack.isEmpty()) {
            return;
        }
        recipeSlots.stream()
            .filter(slot -> slot.getRole() == RecipeIngredientRole.OUTPUT)
            .forEach(slot -> {
                if (slot.getRole() == RecipeIngredientRole.OUTPUT) {
                    slot.getDisplayedItemStack().ifPresent(pill -> {
                        pill.set(DataComponents.POTION_CONTENTS, itemStack.get(DataComponents.POTION_CONTENTS));
                        pill.set(ModComponents.WEAKENING, itemStack.getOrDefault(ModComponents.WEAKENING, false));
                        slot.createDisplayOverrides().add(pill);
                    });
                }
            });
    }
}
