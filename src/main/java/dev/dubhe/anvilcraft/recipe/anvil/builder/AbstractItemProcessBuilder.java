package dev.dubhe.anvilcraft.recipe.anvil.builder;

import dev.dubhe.anvilcraft.recipe.anvil.AbstractItemProcessRecipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Setter
@Accessors(fluent = true, chain = true)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractItemProcessBuilder<T extends AbstractItemProcessRecipe> extends AbstractRecipeBuilder<T> {
    protected NonNullList<Ingredient> ingredients = NonNullList.create();
    protected List<ItemStack> results = new ArrayList<>();

    public AbstractItemProcessBuilder<T> requires(Ingredient ingredient, int count) {
        for (int i = 0; i < count; i++) {
            this.ingredients.add(ingredient);
        }
        return this;
    }

    public AbstractItemProcessBuilder<T> requires(Ingredient ingredient) {
        return requires(ingredient, 1);
    }

    public AbstractItemProcessBuilder<T> requires(ItemLike pItem, int count) {
        return requires(Ingredient.of(pItem), count);
    }

    public AbstractItemProcessBuilder<T> requires(ItemLike pItem) {
        return requires(pItem, 1);
    }

    public AbstractItemProcessBuilder<T> requires(TagKey<Item> pTag, int count) {
        return requires(Ingredient.of(pTag), count);
    }

    public AbstractItemProcessBuilder<T> requires(TagKey<Item> pTag) {
        return requires(pTag, 1);
    }

    public AbstractItemProcessBuilder<T> result(ItemStack stack) {
        results.add(stack);
        return this;
    }

    @Override
    public void validate(ResourceLocation pId) {
        if (ingredients.isEmpty() || ingredients.size() > 9) {
            throw new IllegalArgumentException("Recipe ingredients size must in 0-9, RecipeId: " + pId);
        }
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Recipe results must not be null, RecipeId: " + pId);
        }
    }

    @Override
    public Item getResult() {
        return results.getFirst().getItem();
    }
}
