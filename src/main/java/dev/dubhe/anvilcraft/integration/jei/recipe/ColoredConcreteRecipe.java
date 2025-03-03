package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.ConcreteRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;

public class ColoredConcreteRecipe {
    public final Color color;
    public final List<Ingredient> ingredients;
    public final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;
    public final ChanceItemStack result;

    public ColoredConcreteRecipe(
        Color color,
        List<Ingredient> ingredients,
        List<Object2IntMap.Entry<Ingredient>> mergedIngredients,
        ChanceItemStack result) {
        this.color = color;
        this.ingredients = ingredients;
        this.mergedIngredients = mergedIngredients;
        this.result = result;
    }

    public static ImmutableList<ColoredConcreteRecipe> getAllRecipes() {
        ImmutableList.Builder<ColoredConcreteRecipe> builder = ImmutableList.builder();
        for (ConcreteRecipe concreteRecipe : JeiRecipeUtil.getRecipesFromType(ModRecipeTypes.CONCRETE_TYPE.get())) {
            for (Color color : Color.values()) {
                Item resultItem = BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse(concreteRecipe.result.formatted(color.getSerializedName())));
                builder.add(new ColoredConcreteRecipe(
                    color,
                    concreteRecipe.ingredients,
                    concreteRecipe.mergedIngredients,
                    ChanceItemStack.of(new ItemStack(resultItem, concreteRecipe.resultCount))));
            }
        }
        return builder.build();
    }
}
