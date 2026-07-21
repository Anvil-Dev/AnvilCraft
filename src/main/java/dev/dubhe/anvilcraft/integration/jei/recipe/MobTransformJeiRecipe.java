package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public record MobTransformJeiRecipe(
    Identifier id,
    EntityType<?> input,
    List<ItemIngredientPredicate> inputItems,
    List<TransformResult> results,
    ItemStack outputItem,
    int chancePercentPerItem
) {
    public MobTransformJeiRecipe {
        inputItems = List.copyOf(inputItems);
        results = List.copyOf(results);
        outputItem = outputItem.copy();
    }

    public static MobTransformJeiRecipe ofStandard(RecipeHolder<MobTransformRecipe> holder) {
        MobTransformRecipe recipe = holder.value();
        return new MobTransformJeiRecipe(
            holder.id().identifier(),
            recipe.input(),
            List.of(),
            recipe.results(),
            ItemStack.EMPTY,
            0
        );
    }

    public static MobTransformJeiRecipe ofWithItem(RecipeHolder<MobTransformWithItemRecipe> holder) {
        MobTransformWithItemRecipe recipe = holder.value();
        return new MobTransformJeiRecipe(
            holder.id().identifier(),
            recipe.input(),
            recipe.itemIngredients(),
            List.of(recipe.specialResult()),
            recipe.itemResult().create(),
            recipe.chancePercentPerItem()
        );
    }

    public boolean hasHeldItem() {
        return !this.inputItems.isEmpty();
    }
}
