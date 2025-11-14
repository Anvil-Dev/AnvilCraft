package dev.dubhe.anvilcraft.integration.jei.category.extension;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.recipe.PillRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PillRecipeExtension implements ICraftingCategoryExtension<PillRecipe> {
    @Override
    public void setRecipe(
        RecipeHolder<PillRecipe> recipeHolder,
        IRecipeLayoutBuilder builder,
        ICraftingGridHelper craftingGridHelper,
        IFocusGroup focuses
    ) {
        List<Holder.Reference<Potion>> potions = RegistryUtil.getRegistry(Registries.POTION).asLookup()
            .listElements()
            .filter((potion1) ->
                !potion1.is(Potions.WATER)
                && !potion1.is(Potions.MUNDANE)
                && !potion1.is(Potions.THICK)
                && !potion1.is(Potions.AWKWARD)
            ).toList();
        List<ItemStack> potionList = new ArrayList<>();
        potionList.addAll(potions.stream().map((potion1) -> {
            ItemStack stack = Items.POTION.getDefaultInstance();
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion1));
            return stack;
        }).toList());
        potionList.addAll(potions.stream().map((potion1) -> {
            ItemStack stack = Items.SPLASH_POTION.getDefaultInstance();
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion1));
            return stack;
        }).toList());
        potionList.addAll(potions.stream().map((potion1) -> {
            ItemStack stack = Items.LINGERING_POTION.getDefaultInstance();
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion1));
            return stack;
        }).toList());
        craftingGridHelper.createAndSetInputs(builder, List.of(potionList, List.of(ModFoodItems.PILL.asStack())), 0, 0);
        craftingGridHelper.createAndSetOutputs(builder, List.of(ModFoodItems.PILL.asStack()));
    }

    @Override
    public void onDisplayedIngredientsUpdate(
        RecipeHolder<PillRecipe> recipeHolder,
        List<IRecipeSlotDrawable> recipeSlots,
        IFocusGroup focuses
    ) {
        PillRecipe recipe = recipeHolder.value();
        ItemStack itemStack = recipeSlots.stream()
            .filter((slot) -> slot.getRole() == RecipeIngredientRole.INPUT)
            .map(IRecipeSlotView::getDisplayedItemStack)
            .flatMap(Optional::stream)
            .filter(recipe::validatePotion)
            .findFirst()
            .orElse(ItemStack.EMPTY);
        if (itemStack.isEmpty()) {
            return;
        }
        recipeSlots.stream()
            .filter((slot) -> slot.getRole() == RecipeIngredientRole.OUTPUT)
            .forEach((slot) -> {
                if (slot.getRole() == RecipeIngredientRole.OUTPUT) {
                    slot.getDisplayedItemStack().ifPresent((pill) -> {
                        pill.set(DataComponents.POTION_CONTENTS, itemStack.get(DataComponents.POTION_CONTENTS));
                        pill.set(ModComponents.WEAKENING, itemStack.getOrDefault(ModComponents.WEAKENING, false));
                        slot.createDisplayOverrides().addItemStack(pill);
                    });
                }
            });
    }
}
