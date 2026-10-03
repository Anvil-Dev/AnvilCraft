package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VanillaRecipesWrap {
    public static Multimap<Item, ShapelessRecipe> shapelessRecipes;
    public static Multimap<Item, ShapedRecipe> shapedRecipes;
    public static Multimap<Item, BlastingRecipe> blastingRecipes;
    public static Multimap<Item, SmokingRecipe> smokingRecipes;
    public static Multimap<Item, CampfireCookingRecipe> campfireCookingRecipes;
    public static Multimap<Item, SmeltingRecipe> smeltingRecipes;
    public static List<RecipeHolder<InWorldRecipe>> recipes;

    private static final Map<Recipe<?>, ResourceLocation> SOURCE_IDS = new IdentityHashMap<>();
    private static final Set<CookingSignature> WRAPPED_BLASTING = new HashSet<>();
    private static final Set<CookingSignature> WRAPPED_SMOKING = new HashSet<>();
    private static final Set<CookingSignature> WRAPPED_CAMPFIRE = new HashSet<>();

    private record CookingSignature(List<ResourceLocation> inputs, Item item, int count, DataComponentPatch components) {
        private CookingSignature(Ingredient input, ItemStack result) {
            this(inputIds(input), result.getItem(), result.getCount(), result.getComponentsPatch());
        }
    }

    public static List<RecipeHolder<InWorldRecipe>> init(HolderLookup.Provider registries, Collection<RecipeHolder<?>> recipes) {
        shapelessRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        shapedRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        blastingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        smokingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        campfireCookingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        smeltingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.recipes = new ArrayList<>();
        SOURCE_IDS.clear();
        WRAPPED_BLASTING.clear();
        WRAPPED_SMOKING.clear();
        WRAPPED_CAMPFIRE.clear();
        List<RecipeHolder<?>> sources = new ArrayList<>(recipes);
        sources.sort(Comparator.<RecipeHolder<?>>comparingInt(holder -> wrappingOrder(holder.value())).thenComparing(RecipeHolder::id));
        for (RecipeHolder<?> holder : sources) {
            Recipe<?> recipe = holder.value();
            SOURCE_IDS.put(recipe, holder.id());
            ItemStack result = recipe.getResultItem(registries);
            if (result == null) continue;
            Item item = result.getItem();
            switch (recipe) {
                case ShapelessRecipe value -> {
                    shapelessRecipes.put(item, value);
                    wrap(registries, value);
                }
                case ShapedRecipe value -> {
                    shapedRecipes.put(item, value);
                    wrap(registries, value);
                }
                case BlastingRecipe value -> {
                    blastingRecipes.put(item, value);
                    wrap(registries, value);
                }
                case SmokingRecipe value -> {
                    smokingRecipes.put(item, value);
                    wrap(registries, value);
                }
                case CampfireCookingRecipe value -> {
                    campfireCookingRecipes.put(item, value);
                    wrap(registries, value);
                }
                case SmeltingRecipe value -> {
                    smeltingRecipes.put(item, value);
                    wrap(registries, value);
                }
                default -> {
                }
            }
        }
        return VanillaRecipesWrap.recipes;
    }

    public static void wrap(HolderLookup.Provider registries, @Nullable ShapelessRecipe recipe) {
        if (recipe == null || recipe.getIngredients().isEmpty()) return;
        List<Ingredient> ingredients = recipe.getIngredients();
        Ingredient first = ingredients.getFirst();
        ItemStack result = copyResult(registries, recipe);
        if (result.isEmpty() || first.isEmpty()) return;
        if (ingredients.size() == 1 && result.getCount() > 1 && first.isSimple()) {
            UnpackRecipe unpack = UnpackRecipe.builder().requires(wrapIngredient(first, 1)).result(result).buildRecipe();
            recipes.add(new RecipeHolder<>(wrappedId(registries, "unpack", recipe, first, 1, result), unpack));
        }
        if (ingredients.size() != 4 && ingredients.size() != 9) return;
        wrapCompression(registries, recipe, ingredients, result);
    }

    public static void wrap(HolderLookup.Provider registries, @Nullable ShapedRecipe recipe) {
        if (recipe == null || recipe.getHeight() != recipe.getWidth()) return;
        List<Ingredient> ingredients = recipe.getIngredients();
        ItemStack result = copyResult(registries, recipe);
        if (ingredients.size() <= 1 || !result.is(ModItemTags.COMPRESS_ITEM)) return;
        wrapCompression(registries, recipe, ingredients, result);
    }

    public static void wrap(HolderLookup.Provider registries, @Nullable BlastingRecipe recipe) {
        if (!canWrap(recipe)) return;
        Ingredient input = recipe.getIngredients().getFirst();
        ItemStack result = copyResult(registries, recipe);
        if (result.isEmpty()) return;
        addHeating(registries, "blasting", recipe, input, result);
        WRAPPED_BLASTING.add(new CookingSignature(input, result));
    }

    public static void wrap(HolderLookup.Provider registries, @Nullable SmokingRecipe recipe) {
        if (!canWrap(recipe)) return;
        Ingredient input = recipe.getIngredients().getFirst();
        ItemStack result = copyResult(registries, recipe);
        if (result.isEmpty()) return;
        addCooking(registries, "smoking", recipe, input, result);
        WRAPPED_SMOKING.add(new CookingSignature(input, result));
    }

    public static void wrap(HolderLookup.Provider registries, @Nullable CampfireCookingRecipe recipe) {
        if (!canWrap(recipe)) return;
        Ingredient input = recipe.getIngredients().getFirst();
        ItemStack result = copyResult(registries, recipe);
        if (result.isEmpty()) return;
        CookingSignature signature = new CookingSignature(input, result);
        if (WRAPPED_SMOKING.contains(signature)) return;
        addCooking(registries, "campfire", recipe, input, result);
        WRAPPED_CAMPFIRE.add(signature);
    }

    public static void wrap(HolderLookup.Provider registries, @Nullable SmeltingRecipe recipe) {
        if (!canWrap(recipe)) return;
        Ingredient input = recipe.getIngredients().getFirst();
        ItemStack result = copyResult(registries, recipe);
        if (result.isEmpty()) return;
        CookingSignature signature = new CookingSignature(input, result);
        if (WRAPPED_BLASTING.contains(signature) || WRAPPED_SMOKING.contains(signature) || WRAPPED_CAMPFIRE.contains(signature)) return;
        addHeating(registries, "smelting", recipe, input, result);
    }

    private static void wrapCompression(
        HolderLookup.Provider registries, Recipe<?> source, List<Ingredient> ingredients, ItemStack result
    ) {
        if (!result.is(Tags.Items.STORAGE_BLOCKS) && !result.is(ModItemTags.COMPRESS_ITEM)) return;
        if (ingredients.isEmpty() || ingredients.stream().anyMatch(ingredient -> ingredient.isEmpty() || !ingredient.isSimple())) return;
        Item[] items = Arrays.stream(ingredients.getFirst().getItems())
            .filter(stack -> ingredients.stream().allMatch(ingredient -> ingredient.test(stack)))
            .map(ItemStack::getItem).distinct().toArray(Item[]::new);
        if (items.length == 0) return;
        Ingredient common = Ingredient.of(items);
        ItemCompressRecipe compressed = ItemCompressRecipe.builder()
            .requires(wrapIngredient(common, ingredients.size())).result(result).buildRecipe();
        recipes.add(new RecipeHolder<>(wrappedId(registries, "compress", source, common, ingredients.size(), result), compressed));
    }

    private static boolean canWrap(@Nullable Recipe<?> recipe) {
        if (recipe == null || recipe.getIngredients().isEmpty()) return false;
        Ingredient input = recipe.getIngredients().getFirst();
        return !input.isEmpty() && input.isSimple();
    }

    private static ItemStack copyResult(HolderLookup.Provider registries, Recipe<?> recipe) {
        ItemStack result = recipe.getResultItem(registries);
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    private static void addCooking(HolderLookup.Provider registries, String kind, Recipe<?> source, Ingredient input, ItemStack result) {
        FastCookingRecipe wrapped = FastCookingRecipe.builder().requires(wrapIngredient(input, 1)).result(result).buildRecipe();
        recipes.add(new RecipeHolder<>(wrappedId(registries, kind, source, input, 1, result), wrapped));
    }

    private static void addHeating(HolderLookup.Provider registries, String kind, Recipe<?> source, Ingredient input, ItemStack result) {
        boolean boost = Arrays.stream(input.getItems()).allMatch(stack -> stack.is(ModItemTags.SUPER_HEATING_BOOST_PRODUCTION));
        ItemStack output = result.copyWithCount(result.getCount() * (boost ? 2 : 1));
        SuperHeatingRecipe wrapped = SuperHeatingRecipe.builder().requires(wrapIngredient(input, 1)).result(output).buildRecipe();
        recipes.add(new RecipeHolder<>(wrappedId(registries, kind, source, input, 1, result), wrapped));
    }

    private static ItemIngredientPredicate wrapIngredient(Ingredient ingredient, int count) {
        Item[] items = Arrays.stream(ingredient.getItems()).map(ItemStack::getItem).distinct().toArray(Item[]::new);
        return ItemIngredientPredicate.of(items).withCount(count).build();
    }

    private static int wrappingOrder(Recipe<?> recipe) {
        return switch (recipe) {
            case ShapelessRecipe ignored -> 0;
            case ShapedRecipe ignored -> 1;
            case BlastingRecipe ignored -> 2;
            case SmokingRecipe ignored -> 3;
            case CampfireCookingRecipe ignored -> 4;
            case SmeltingRecipe ignored -> 5;
            default -> 6;
        };
    }

    private static List<ResourceLocation> inputIds(Ingredient input) {
        return Arrays.stream(input.getItems()).map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())).distinct().sorted().toList();
    }

    private static ResourceLocation wrappedId(
        HolderLookup.Provider registries, String kind, Recipe<?> source, Ingredient input, int count, ItemStack result
    ) {
        ResourceLocation sourceId = SOURCE_IDS.get(source);
        String path;
        if (sourceId != null) {
            path = "source/%s/%s".formatted(sourceId.getNamespace(), sourceId.getPath());
        } else {
            JsonObject signature = new JsonObject();
            JsonArray inputs = new JsonArray();
            inputIds(input).forEach(id -> inputs.add(id.toString()));
            signature.add("inputs", inputs);
            signature.addProperty("count", count);
            signature.add("result", ItemStack.CODEC.encodeStart(
                registries.createSerializationContext(JsonOps.INSTANCE), result).getOrThrow());
            path = "anonymous/" + Hashing.sha256().hashString(canonicalJson(signature).toString(), StandardCharsets.UTF_8);
        }
        return AnvilCraft.of("generated/vanilla/%s/%s".formatted(kind, path));
    }

    private static JsonElement canonicalJson(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = new JsonObject();
            element.getAsJsonObject().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> object.add(entry.getKey(), canonicalJson(entry.getValue())));
            return object;
        }
        if (element.isJsonArray()) {
            JsonArray array = new JsonArray();
            element.getAsJsonArray().forEach(value -> array.add(canonicalJson(value)));
            return array;
        }
        return element;
    }
}
