package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.RecipeUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractItemProcessRecipe implements Recipe<ItemProcessInput> {
    public final NonNullList<Ingredient> ingredients;
    public final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;
    public final List<ItemStack> results;
    public final boolean isSimple;
    protected ItemProcessInput cacheInput;
    protected int cacheMaxCraftTime = -1;

    public AbstractItemProcessRecipe(NonNullList<Ingredient> ingredients, List<ItemStack> results) {
        this.ingredients = ingredients;
        this.mergedIngredients = RecipeUtil.mergeIngredient(ingredients);
        this.results = results;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst();
    }

    @Override
    public boolean matches(ItemProcessInput pInput, Level pLevel) {
        return getMaxCraftTime(pInput) > 0;
    }

    @Override
    public ItemStack assemble(ItemProcessInput pInput, HolderLookup.Provider pRegistries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst();
    }

    public int getMaxCraftTime(ItemProcessInput pInput) {
        if (cacheInput == pInput) {
            return cacheMaxCraftTime;
        }
        int times = RecipeUtil.getMaxCraftTime(pInput, ingredients);
        cacheMaxCraftTime = times;
        cacheInput = pInput;
        return times;
    }
}
