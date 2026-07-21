package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
public class VanillaRecipesWrap {
    public static Multimap<Item, ShapelessRecipe> shapelessRecipes;
    public static Multimap<Item, ShapedRecipe> shapedRecipes;
    public static Multimap<Item, BlastingRecipe> blastingRecipes;
    public static Multimap<Item, SmokingRecipe> smokingRecipes;
    public static Multimap<Item, CampfireCookingRecipe> campfireCookingRecipes;
    public static Multimap<Item, SmeltingRecipe> smeltingRecipes;
    public static List<RecipeHolder<InWorldRecipe>> recipes;

    public static List<RecipeHolder<InWorldRecipe>> init(Collection<RecipeHolder<?>> recipes) {
        VanillaRecipesWrap.shapelessRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.shapedRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.blastingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.smokingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.campfireCookingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.smeltingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.recipes = new ArrayList<>();
        for (RecipeHolder<?> recipeHolder : recipes) {
            switch (recipeHolder.value()) {
                case ShapelessRecipe recipe -> VanillaRecipesWrap.shapelessRecipes.put(recipe.result.item().value(), recipe);
                case ShapedRecipe recipe -> VanillaRecipesWrap.shapedRecipes.put(recipe.result.item().value(), recipe);
                case CampfireCookingRecipe recipe -> VanillaRecipesWrap.campfireCookingRecipes.put(recipe.result.item().value(), recipe);
                case BlastingRecipe recipe -> VanillaRecipesWrap.blastingRecipes.put(recipe.result.item().value(), recipe);
                case SmokingRecipe recipe -> VanillaRecipesWrap.smokingRecipes.put(recipe.result.item().value(), recipe);
                case SmeltingRecipe recipe -> VanillaRecipesWrap.smeltingRecipes.put(recipe.result.item().value(), recipe);
                default -> {
                }
            }
        }
        VanillaRecipesWrap.shapelessRecipes.forEach((_, recipe) -> VanillaRecipesWrap.wrap(recipe));
        VanillaRecipesWrap.shapedRecipes.forEach((_, recipe) -> VanillaRecipesWrap.wrap(recipe));
        VanillaRecipesWrap.blastingRecipes.forEach((_, recipe) -> VanillaRecipesWrap.wrap(recipe));
        VanillaRecipesWrap.smokingRecipes.forEach((_, recipe) -> VanillaRecipesWrap.wrap(recipe));
        VanillaRecipesWrap.campfireCookingRecipes.forEach((_, recipe) -> VanillaRecipesWrap.wrap(recipe));
        VanillaRecipesWrap.smeltingRecipes.forEach((_, recipe) -> VanillaRecipesWrap.wrap(recipe));
        return VanillaRecipesWrap.recipes;
    }

    public static void wrap(@Nullable ShapelessRecipe recipe) {
        if (recipe == null) return;
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        if (ingredients.isEmpty()) return;
        Ingredient first = ingredients.getFirst();
        if (first.isEmpty() || first.isCustom()) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        if (ingredients.size() == 1 && result.count() > 1) {
            VanillaRecipesWrap.wrapUnpack(first, result);
        }
        if (ingredients.size() != 4 && ingredients.size() != 9) return;
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.equals(first)) return;
        }
        VanillaRecipesWrap.wrapItemCompress(first, ingredients.size(), result);
    }

    public static void wrap(@Nullable ShapedRecipe recipe) {
        if (recipe == null) return;
        if (recipe.getHeight() != recipe.getWidth()) return;
        List<Optional<Ingredient>> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) return;
        Optional<Ingredient> firstOp = ingredients.getFirst();
        if (firstOp.isEmpty()) return;
        Ingredient first = firstOp.get();
        if (first.isEmpty() || first.isCustom()) return;
        if (ingredients.size() <= 1) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        if (!result.is(ModItemTags.COMPRESS_ITEM)) return;
        for (Optional<Ingredient> ingredient : ingredients) {
            if (!ingredient.map(i -> i.equals(first)).orElse(false)) return;
        }
        VanillaRecipesWrap.wrapItemCompress(first, ingredients.size(), result);
    }

    public static void wrap(@Nullable BlastingRecipe recipe) {
        if (recipe == null) return;
        Ingredient input = recipe.input();
        if (input.isEmpty() || input.isCustom()) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        boolean boost = true;
        for (Holder<Item> value : input.getValues()) {
            if (value.is(ModItemTags.SUPER_HEATING_BOOST_PRODUCTION)) continue;
            boost = false;
            break;
        }
        SuperHeatingRecipe superHeating = SuperHeatingRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(input.getValues()),
                1,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result.withCount(result.count() * (boost ? 2 : 1)))
            .buildRecipe();
        String ingredient = VanillaRecipesWrap.process(input);
        String res = VanillaRecipesWrap.process(result);
        ResourceKey<Recipe<?>> key = ResourceKey.create(
            Registries.RECIPE,
            AnvilCraft.of("super_heating_warp_%s_2_%s".formatted(ingredient, res))
        );
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, superHeating));
    }

    public static void wrap(@Nullable SmokingRecipe recipe) {
        if (recipe == null) return;
        Ingredient input = recipe.input();
        if (input.isEmpty() || input.isCustom()) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        FastCookingRecipe cooking = FastCookingRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(input.getValues()),
                1,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result)
            .buildRecipe();
        String ingredient = VanillaRecipesWrap.process(input);
        String res = VanillaRecipesWrap.process(result);
        ResourceKey<Recipe<?>> key = ResourceKey.create(
            Registries.RECIPE,
            AnvilCraft.of("smoking_warp_%s_2_%s".formatted(ingredient, res))
        );
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, cooking));
    }

    public static void wrap(@Nullable CampfireCookingRecipe recipe) {
        if (recipe == null) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        if (VanillaRecipesWrap.smokingRecipes.containsKey(result.item().value())) return;
        Ingredient input = recipe.input();
        if (input.isEmpty() || input.isCustom()) return;
        FastCookingRecipe cooking = FastCookingRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(input.getValues()),
                1,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result)
            .buildRecipe();
        String ingredient = VanillaRecipesWrap.process(input);
        String res = VanillaRecipesWrap.process(result);
        ResourceKey<Recipe<?>> key = ResourceKey.create(
            Registries.RECIPE,
            AnvilCraft.of("smoking_warp_%s_2_%s".formatted(ingredient, res))
        );
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, cooking));
    }

    public static void wrap(@Nullable SmeltingRecipe recipe) {
        if (recipe == null) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        if (VanillaRecipesWrap.smokingRecipes.containsKey(result.item().value())) return;
        if (VanillaRecipesWrap.blastingRecipes.containsKey(result.item().value())) return;
        if (VanillaRecipesWrap.campfireCookingRecipes.containsKey(result.item().value())) return;
        Ingredient input = recipe.input();
        if (input.isEmpty() || input.isCustom()) return;
        boolean boost = true;
        for (Holder<Item> value : input.getValues()) {
            if (value.is(ModItemTags.SUPER_HEATING_BOOST_PRODUCTION)) continue;
            boost = false;
            break;
        }
        SuperHeatingRecipe superHeating = SuperHeatingRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(input.getValues()),
                1,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result.withCount(result.count() * (boost ? 2 : 1)))
            .buildRecipe();
        String ingredient = VanillaRecipesWrap.process(input);
        String res = VanillaRecipesWrap.process(result);
        ResourceKey<Recipe<?>> key = ResourceKey.create(
            Registries.RECIPE,
            AnvilCraft.of("heating_warp_%s_2_%s".formatted(ingredient, res))
        );
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, superHeating));
    }

    private static void wrapUnpack(Ingredient first, ItemStackTemplate result) {
        UnpackRecipe recipe = UnpackRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(first.getValues()),
                1,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result)
            .buildRecipe();
        String ingredient = VanillaRecipesWrap.process(first);
        String res = VanillaRecipesWrap.process(result);
        ResourceKey<Recipe<?>> key = ResourceKey.create(
            Registries.RECIPE,
            AnvilCraft.of("unpack_warp_%s_2_%s".formatted(ingredient, res))
        );
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, recipe));
    }

    private static void wrapItemCompress(Ingredient first, int count, ItemStackTemplate result) {
        if (!result.is(Tags.Items.STORAGE_BLOCKS) && !result.is(ModItemTags.COMPRESS_ITEM)) return;
        ItemCompressRecipe recipe = ItemCompressRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(first.getValues()),
                count,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result)
            .buildRecipe();
        String ingredient = VanillaRecipesWrap.process(first);
        String res = VanillaRecipesWrap.process(result);
        ResourceKey<Recipe<?>> key = ResourceKey.create(
            Registries.RECIPE,
            AnvilCraft.of("compress_warp_%s_2_%s".formatted(ingredient, res))
        );
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, recipe));
    }

    private static String process(Ingredient ingredient) {
        return ingredient.getValues().unwrap()
            .map(TagKey::location, holder -> BuiltInRegistries.ITEM.getKey(holder.getFirst().value()))
            .toShortString()
            .replace(':', '_')
            .replace('/', '_');
    }

    private static String process(ItemStackTemplate stack) {
        return stack.typeHolder().getKey().identifier()
            .toShortString()
            .replace(':', '_')
            .replace('/', '_');
    }
}
