package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Locale;

public record ColoredConcreteRecipe(Color color, List<ItemIngredientPredicate> ingredients, ChanceItemStack result) {
    public static ImmutableList<ColoredConcreteRecipe> getAllRecipes() {
        ImmutableList.Builder<ColoredConcreteRecipe> builder = ImmutableList.builder();
        for (BulgingRecipe recipe : JeiRecipeUtil.getRecipesFromType(ModRecipeTypes.BULGING_TYPE.get())) {
            if (recipe.getResultItems().isEmpty()) continue;
            ChanceItemStack result = recipe.getResultItems().getFirst();
            if (!result.stack().is(ModItemTags.REINFORCED_CONCRETE)) continue;
            Color color = Color.valueOf(
                BuiltInRegistries.ITEM.getKey(result.getItem()).getPath().substring(20).toUpperCase(Locale.ROOT));
            builder.add(new ColoredConcreteRecipe(color, recipe.getInputItems(), result));
        }
        return builder.build();
    }
}
