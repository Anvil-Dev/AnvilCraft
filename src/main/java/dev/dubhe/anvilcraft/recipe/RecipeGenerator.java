package dev.dubhe.anvilcraft.recipe;

import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.UnpackRecipe;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import org.slf4j.Logger;

import java.util.Optional;

public class RecipeGenerator {

    private static final Logger logger = LogUtils.getLogger();
    private static final String HASH_TO_CHAR = "0123456789abcdefghijklmnopqrstuv";

    private static ResourceLocation generateRecipeId(
        RecipeType<?> recipeType,
        RecipeHolder<?> recipeHolder
    ) {
        logger.debug("Generating anvil recipe for {}", recipeHolder.id());
        logger.debug("Recipe type of {} is {}", recipeHolder.id(), recipeType);
        ResourceLocation newId = hashRecipeId(recipeHolder.id());
        logger.debug("New id of {} is {}", recipeHolder.id(), newId);
        return newId;
    }

    private static ResourceLocation hashRecipeId(ResourceLocation rl) {
        long hash = 0;
        for (char c : rl.toString().toCharArray()) {
            hash *= 19980731;
            hash += c;
        }
        StringBuilder hashedId = new StringBuilder(rl.getPath());
        hashedId.append("_generated_");
        for (int i = 0; i < 13; i++) {
            hashedId.append(HASH_TO_CHAR.charAt((int)(hash >>> (5 * i)) & 31));
        }
        return AnvilCraft.of(hashedId.toString());
    }

    public static Optional<RecipeHolder<?>> handleVanillaRecipe(
        RecipeType<?> recipeType,
        RecipeHolder<?> recipeHolder
    ) {
        if (recipeType != RecipeType.SMOKING && recipeType != RecipeType.CRAFTING) return Optional.empty();
        if (recipeType == RecipeType.SMOKING) {
            SmokingRecipe recipe = (SmokingRecipe) recipeHolder.value();
            CookingRecipe newRecipe = CookingRecipe.builder()
                .requires(recipe.ingredient)
                .result(recipe.result)
                .buildRecipe();
            return Optional.of(new RecipeHolder<>(generateRecipeId(recipeType, recipeHolder), newRecipe));
        }
//        if (recipeType == RecipeType.BLASTING) {
//            BlastingRecipe recipe = (BlastingRecipe) recipeHolder.value();
//            AbstractItemProcessBuilder<SuperHeatingRecipe> builder =
//                SuperHeatingRecipe.builder()
//                    .requires(recipe.ingredient)
//                    .generated(true);
//            ItemStack result = recipe.result.copy();
//            logger.debug("Result of new recipe {} is {}", newId, result);
//            for (ItemStack item : recipe.ingredient.getItems()) {
//                logger.debug("Ingredient Item {} has following tags:", item);
//                item.getTags().forEach(it -> logger.debug("\t- {}", it.location()));
//            }
//            SuperHeatingRecipe newRecipe = builder.result(result).buildRecipe();
//            return Optional.of(new RecipeHolder<>(newId, newRecipe));
//        }
//        if (recipeType == RecipeType.SMELTING) {
//            SmeltingRecipe recipe = (SmeltingRecipe) recipeHolder.value();
//            AbstractItemProcessBuilder<SuperHeatingRecipe> builder =
//                SuperHeatingRecipe.builder()
//                    .requires(recipe.ingredient)
//                    .generated(true);
//            ItemStack result = recipe.result.copy();
//            logger.debug("Result of new recipe {} is {}", newId, result);
//            for (ItemStack item : recipe.ingredient.getItems()) {
//                logger.debug("Ingredient Item {} has following tags:", item);
//                item.getTags().forEach(it -> logger.debug("\t- {}", it.location()));
//            }
//            SuperHeatingRecipe newRecipe = builder.result(result)
//                .buildRecipe();
//            return Optional.of(new RecipeHolder<>(newId, newRecipe));
//        }
        if (recipeType == RecipeType.CRAFTING) {
            CraftingRecipe recipe = (CraftingRecipe) recipeHolder.value();
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                ShapedRecipePattern pattern = shapedRecipe.pattern;
                if (pattern.height() == pattern.width()
                    && (pattern.height() == 2 || pattern.height() == 3)
                    && RecipeUtil.allIngredientEquals(pattern.ingredients())
                ) {
                    ItemCompressRecipe newRecipe = ItemCompressRecipe.builder()
                        .result(shapedRecipe.result)
                        .requires(pattern.ingredients().getFirst(), pattern.height() * pattern.height())
                        .buildRecipe();
                    return Optional.of(new RecipeHolder<>(generateRecipeId(recipeType, recipeHolder), newRecipe));
                }
            } else {
                if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                    NonNullList<Ingredient> ingredients = shapelessRecipe.getIngredients();
                    if (ingredients.size() == 1) {
                        UnpackRecipe newRecipe = UnpackRecipe.builder()
                            .result(shapelessRecipe.result)
                            .requires(ingredients.getFirst())
                            .buildRecipe();
                        return Optional.of(new RecipeHolder<>(generateRecipeId(recipeType, recipeHolder), newRecipe));
                    }
                    if (RecipeUtil.allIngredientEquals(ingredients)) {
                        ItemCompressRecipe newRecipe = ItemCompressRecipe.builder()
                            .result(shapelessRecipe.result)
                            .requires(ingredients.getFirst(), ingredients.size())
                            .buildRecipe();
                        return Optional.of(new RecipeHolder<>(generateRecipeId(recipeType, recipeHolder), newRecipe));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
