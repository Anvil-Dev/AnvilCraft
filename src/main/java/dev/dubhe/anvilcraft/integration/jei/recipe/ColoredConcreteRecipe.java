package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.decoration.ReinforcedConcreteBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;

import java.util.List;
import java.util.Locale;

public record ColoredConcreteRecipe(Color color, List<ItemIngredientPredicate> ingredients, ChanceItemStack result) {
    public static ImmutableList<ColoredConcreteRecipe> getAllRecipes() {
        ImmutableList.Builder<ColoredConcreteRecipe> builder = ImmutableList.builder();
        for (BulgingRecipe recipe : JeiRecipeUtil.getRecipesFromType(ModRecipeTypes.BULGING.get())) {
            if (recipe.getResultItems().isEmpty()) continue;
            ChanceItemStack result = recipe.getResultItems().getFirst();
            if (!result.stack().is(ModItemTags.REINFORCED_CONCRETE)) continue;
            if (result.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ReinforcedConcreteBlock block) {
                builder.add(new ColoredConcreteRecipe(block.getColor(), recipe.getInputItems(), result));
            }
        }
        return builder.build();
    }
}
