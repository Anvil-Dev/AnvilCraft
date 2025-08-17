package dev.dubhe.anvilcraft.recipe.generate;

import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
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
            hashedId.append(HASH_TO_CHAR.charAt((int) (hash >>> (5 * i)) & 31));
        }
        return AnvilCraft.of(hashedId.toString());
    }

    public static Optional<RecipeHolder<?>> handleVanillaRecipe(
        RecipeType<?> type,
        RecipeHolder<?> recipeHolder
    ) {
//        return switch (type) {
//            case RecipeType<?> type1 when type1 == RecipeType.CRAFTING -> {
//                CraftingRecipe recipe = (CraftingRecipe) recipeHolder.value();
//                if (recipe instanceof ShapedRecipe shapedRecipe) {
//                    ShapedRecipePattern pattern = shapedRecipe.pattern;
//                    //noinspection ConstantValue
//                    if (pattern == null) yield Optional.empty();
//                    if (pattern.height() == pattern.width()
//                        && pattern.height() != 1
//                        && RecipeUtil.allIngredientEquals(pattern.ingredients())
//                    ) {
//                        ItemCompressRecipe newRecipe = ItemCompressRecipe.builder()
//                            .result(shapedRecipe.result)
//                            .requires(pattern.ingredients().getFirst(), pattern.height() * pattern.height())
//                            .generated(true)
//                            .buildRecipe();
//                        yield Optional.of(new RecipeHolder<>(generateRecipeId(type, recipeHolder), newRecipe));
//                    }
//                } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
//                    NonNullList<Ingredient> ingredients = shapelessRecipe.getIngredients();
//                    if (ingredients.isEmpty()) yield Optional.empty();
//                    if (ingredients.size() == 1) {
//                        UnpackRecipe newRecipe = UnpackRecipe.builder()
//                            .result(shapelessRecipe.result)
//                            .requires(ingredients.getFirst())
//                            .generated(true)
//                            .buildRecipe();
//                        yield Optional.of(new RecipeHolder<>(generateRecipeId(type, recipeHolder), newRecipe));
//                    }
//                    if (RecipeUtil.allIngredientEquals(ingredients)) {
//                        ItemCompressRecipe newRecipe = ItemCompressRecipe.builder()
//                            .result(shapelessRecipe.result)
//                            .requires(ingredients.getFirst(), ingredients.size())
//                            .buildRecipe();
//                        yield Optional.of(new RecipeHolder<>(generateRecipeId(type, recipeHolder), newRecipe));
//                    }
//                }
//                yield Optional.empty();
//            }
//            /*
//            case RecipeType<?> type1 when type1 == RecipeType.SMELTING -> {
//                SmeltingRecipe recipe = (SmeltingRecipe) recipeHolder.value();
//                SuperHeatingRecipe newRecipe = SuperHeatingRecipe.builder()
//                    .blockResult(Blocks.CAULDRON)
//                    .requires(recipe.ingredient)
//                    .result(recipe.result)
//                    .generated(true)
//                    .buildRecipe();
//                yield Optional.of(new RecipeHolder<>(generateRecipeId(type, recipeHolder), newRecipe));
//            }*/
//            case RecipeType<?> type1 when type1 == RecipeType.SMOKING -> {
//                SmokingRecipe recipe = (SmokingRecipe) recipeHolder.value();
//                CookingRecipe newRecipe = CookingRecipe.builder()
//                    .requires(recipe.ingredient)
//                    .result(recipe.result)
//                    .generated(true)
//                    .buildRecipe();
//                yield Optional.of(new RecipeHolder<>(generateRecipeId(type, recipeHolder), newRecipe));
//            }
//            /*
//            case RecipeType<?> type1 when type1 == RecipeType.BLASTING -> {
//                BlastingRecipe recipe = (BlastingRecipe) recipeHolder.value();
//                SuperHeatingRecipe newRecipe = SuperHeatingRecipe.builder()
//                    .blockResult(Blocks.CAULDRON)
//                    .requires(recipe.ingredient)
//                    .result(recipe.result)
//                    .generated(true)
//                    .buildRecipe();
//                yield Optional.of(new RecipeHolder<>(generateRecipeId(type, recipeHolder), newRecipe));
//            }*/
//            default -> Optional.empty();
//        };
        return Optional.empty();
    }
}
