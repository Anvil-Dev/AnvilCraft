package dev.dubhe.anvilcraft.recipe.anvil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@SuppressWarnings("DuplicatedCode")
public class VanillaRecipesWrap {
    public static Multimap<Item, ShapelessRecipe> shapelessRecipes;
    public static Multimap<Item, ShapedRecipe> shapedRecipes;
    public static Multimap<Item, BlastingRecipe> blastingRecipes;
    public static Multimap<Item, SmokingRecipe> smokingRecipes;
    public static Multimap<Item, CampfireCookingRecipe> campfireCookingRecipes;
    public static Multimap<Item, SmeltingRecipe> smeltingRecipes;
    public static List<RecipeHolder<InWorldRecipe>> recipes;

    public static List<RecipeHolder<InWorldRecipe>> init(HolderLookup.Provider registries, @NotNull Collection<RecipeHolder<?>> recipes) {
        VanillaRecipesWrap.shapelessRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.shapedRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.blastingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.smokingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.campfireCookingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.smeltingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.recipes = new ArrayList<>();
        for (RecipeHolder<?> recipeHolder : recipes) {
            Recipe<?> recipe = recipeHolder.value();
            Item item = recipe.getResultItem(registries).getItem();
            if (recipe.getIngredients().isEmpty() || recipe.getIngredients().getFirst().isCustom()) {
                continue;
            }
            switch (recipe) {
                case ShapelessRecipe shapelessRecipe -> shapelessRecipes.put(item, shapelessRecipe);
                case ShapedRecipe shapedRecipe -> shapedRecipes.put(item, shapedRecipe);
                case CampfireCookingRecipe campfireCookingRecipe -> campfireCookingRecipes.put(item, campfireCookingRecipe);
                case BlastingRecipe blastingRecipe -> blastingRecipes.put(item, blastingRecipe);
                case SmokingRecipe smokingRecipe -> smokingRecipes.put(item, smokingRecipe);
                case SmeltingRecipe smeltingRecipe -> smeltingRecipes.put(item, smeltingRecipe);
                default -> {
                }
            }
        }
        VanillaRecipesWrap.shapelessRecipes.forEach((item, recipe) -> VanillaRecipesWrap.wrap(registries, recipe));
        VanillaRecipesWrap.shapedRecipes.forEach((item, recipe) -> VanillaRecipesWrap.wrap(registries, recipe));
        VanillaRecipesWrap.blastingRecipes.forEach((item, recipe) -> VanillaRecipesWrap.wrap(registries, recipe));
        VanillaRecipesWrap.smokingRecipes.forEach((item, recipe) -> VanillaRecipesWrap.wrap(registries, recipe));
        VanillaRecipesWrap.campfireCookingRecipes.forEach((item, recipe) -> VanillaRecipesWrap.wrap(registries, recipe));
        VanillaRecipesWrap.smeltingRecipes.forEach((item, recipe) -> VanillaRecipesWrap.wrap(registries, recipe));
        return VanillaRecipesWrap.recipes;
    }

    public static void wrap(HolderLookup.Provider registries, ShapelessRecipe recipe) {
        if (recipe == null) return;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        Ingredient first = ingredients.getFirst();
        ItemStack result = recipe.getResultItem(registries).copy();
        ItemIngredientPredicate.Builder builder1 = ItemIngredientPredicate.Builder.item();
        if (ingredients.size() == 1 && result.getCount() > 1) {
            UnpackRecipe.Builder builder = UnpackRecipe.builder();
            String ingredient = "empty";
            for (Ingredient.Value value : first.getValues()) {
                if (value instanceof Ingredient.ItemValue(ItemStack item)) {
                    ingredient = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
                    builder1.of(item);
                }
                if (value instanceof Ingredient.TagValue(TagKey<Item> tag)) {
                    ingredient = tag.location().getPath().replace("/", "_");
                    builder1.of(tag);
                }
            }
            builder.requires(builder1.withCount(ingredients.size()).build());
            builder.result(result);
            UnpackRecipe unpackRecipe = builder.buildRecipe();
            String res = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
            ResourceLocation location = AnvilCraft.of("unpack_warp_%s_2_%s".formatted(ingredient, res));
            VanillaRecipesWrap.recipes.add(new RecipeHolder<>(location, unpackRecipe));
        }
        if (ingredients.size() != 4 && ingredients.size() != 9) return;
        if (!result.is(Tags.Items.STORAGE_BLOCKS)) return;
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.equals(first)) return;
        }
        ItemCompressRecipe.Builder builder = ItemCompressRecipe.builder();
        builder1 = ItemIngredientPredicate.Builder.item();
        String ingredient = "empty";
        for (Ingredient.Value value : first.getValues()) {
            if (value instanceof Ingredient.ItemValue(ItemStack item)) {
                ingredient = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
                builder1.of(item);
            }
            if (value instanceof Ingredient.TagValue(TagKey<Item> tag)) {
                ingredient = tag.location().getPath().replace("/", "_");
                builder1.of(tag);
            }
        }
        builder.requires(builder1.withCount(ingredients.size()).build());
        builder.result(result);
        ItemCompressRecipe itemCompressRecipe = builder.buildRecipe();
        String res = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
        ResourceLocation location = AnvilCraft.of("compress_warp_%s_2_%s".formatted(ingredient, res));
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(location, itemCompressRecipe));
    }

    public static void wrap(HolderLookup.Provider registries, ShapedRecipe recipe) {
        if (recipe == null) return;
        if (recipe.getHeight() != recipe.getWidth()) return;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        Ingredient first = ingredients.getFirst();
        if (ingredients.size() <= 1) return;
        ItemStack result = recipe.getResultItem(registries).copy();
        if (!result.is(Tags.Items.STORAGE_BLOCKS)) return;
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.equals(first)) return;
        }
        ItemCompressRecipe.Builder builder = ItemCompressRecipe.builder();
        ItemIngredientPredicate.Builder builder1 = ItemIngredientPredicate.Builder.item();
        String ingredient = "empty";
        for (Ingredient.Value value : first.getValues()) {
            if (value instanceof Ingredient.ItemValue(ItemStack item)) {
                ingredient = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
                builder1.of(item);
            }
            if (value instanceof Ingredient.TagValue(TagKey<Item> tag)) {
                ingredient = tag.location().getPath().replace("/", "_");
                builder1.of(tag);
            }
        }
        builder.requires(builder1.withCount(ingredients.size()).build());
        builder.result(result);
        ItemCompressRecipe itemCompressRecipe = builder.buildRecipe();
        String res = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
        ResourceLocation location = AnvilCraft.of("compress_warp_%s_2_%s".formatted(ingredient, res));
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(location, itemCompressRecipe));
    }

    public static void wrap(HolderLookup.Provider registries, BlastingRecipe recipe) {
        if (recipe == null) return;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        ItemStack result = recipe.getResultItem(registries).copy();
        Ingredient first = ingredients.getFirst();
        ItemIngredientPredicate.Builder predicateBuilder = ItemIngredientPredicate.Builder.item();
        String ingredient = "empty";
        for (Ingredient.Value value : first.getValues()) {
            if (value instanceof Ingredient.ItemValue(ItemStack item)) {
                ingredient = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
                predicateBuilder.of(item);
            }
            if (value instanceof Ingredient.TagValue(TagKey<Item> tag)) {
                ingredient = tag.location().getPath().replace("/", "_");
                predicateBuilder.of(tag);
            }
        }
        result.setCount(result.getCount() * 2);
        String res = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
        ResourceLocation location = AnvilCraft.of("super_heating_warp_%s_2_%s".formatted(ingredient, res));
        VanillaRecipesWrap.recipes.add(
            new RecipeHolder<>(
                location,
                SuperHeatingRecipe.builder()
                    .requires(predicateBuilder.build())
                    .result(result)
                    .buildRecipe()
            )
        );
    }

    public static void wrap(HolderLookup.Provider registries, SmokingRecipe recipe) {
        if (recipe == null) return;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        ItemStack result = recipe.getResultItem(registries).copy();
        Ingredient first = ingredients.getFirst();
        ItemIngredientPredicate.Builder predicateBuilder = ItemIngredientPredicate.Builder.item();
        String ingredient = "empty";
        for (Ingredient.Value value : first.getValues()) {
            if (value instanceof Ingredient.ItemValue(ItemStack item)) {
                ingredient = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
                predicateBuilder.of(item);
            }
            if (value instanceof Ingredient.TagValue(TagKey<Item> tag)) {
                ingredient = tag.location().getPath().replace("/", "_");
                predicateBuilder.of(tag);
            }
        }
        String res = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
        ResourceLocation location = AnvilCraft.of("smoking_warp_%s_2_%s".formatted(ingredient, res));
        VanillaRecipesWrap.recipes.add(
            new RecipeHolder<>(
                location,
                CookingRecipe.builder()
                    .requires(predicateBuilder.build())
                    .result(result)
                    .buildRecipe()
            )
        );
    }

    public static void wrap(HolderLookup.Provider registries, CampfireCookingRecipe recipe) {
        if (recipe == null) return;
        ItemStack result = recipe.getResultItem(registries).copy();
        if (VanillaRecipesWrap.smokingRecipes.containsKey(result.getItem())) return;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        Ingredient first = ingredients.getFirst();
        ItemIngredientPredicate.Builder predicateBuilder = ItemIngredientPredicate.Builder.item();
        String ingredient = "empty";
        for (Ingredient.Value value : first.getValues()) {
            if (value instanceof Ingredient.ItemValue(ItemStack item)) {
                ingredient = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
                predicateBuilder.of(item);
            }
            if (value instanceof Ingredient.TagValue(TagKey<Item> tag)) {
                ingredient = tag.location().getPath().replace("/", "_");
                predicateBuilder.of(tag);
            }
        }
        String res = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
        ResourceLocation location = AnvilCraft.of("cooking_warp_%s_2_%s".formatted(ingredient, res));
        VanillaRecipesWrap.recipes.add(
            new RecipeHolder<>(
                location,
                CookingRecipe.builder()
                    .requires(predicateBuilder.build())
                    .result(result)
                    .buildRecipe()
            )
        );
    }

    public static void wrap(HolderLookup.Provider registries, SmeltingRecipe recipe) {
        if (recipe == null) return;
        ItemStack result = recipe.getResultItem(registries).copy();
        if (VanillaRecipesWrap.smokingRecipes.containsKey(result.getItem())) return;
        if (VanillaRecipesWrap.blastingRecipes.containsKey(result.getItem())) return;
        if (VanillaRecipesWrap.campfireCookingRecipes.containsKey(result.getItem())) return;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        Ingredient first = ingredients.getFirst();
        ItemIngredientPredicate.Builder predicateBuilder = ItemIngredientPredicate.Builder.item();
        String ingredient = "empty";
        for (Ingredient.Value value : first.getValues()) {
            if (value instanceof Ingredient.ItemValue(ItemStack item)) {
                ingredient = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
                predicateBuilder.of(item);
            }
            if (value instanceof Ingredient.TagValue(TagKey<Item> tag)) {
                ingredient = tag.location().getPath().replace("/", "_");
                predicateBuilder.of(tag);
            }
        }
        String res = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
        ResourceLocation location = AnvilCraft.of("heating_warp_%s_2_%s".formatted(ingredient, res));
        VanillaRecipesWrap.recipes.add(
            new RecipeHolder<>(
                location,
                SuperHeatingRecipe.builder()
                    .requires(predicateBuilder.build())
                    .result(result)
                    .buildRecipe()
            )
        );
    }
}
