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
import lombok.extern.slf4j.Slf4j;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
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
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class VanillaRecipesWrap {
    public static Multimap<Item, ShapelessRecipe> shapelessRecipes;
    public static Multimap<Item, ShapedRecipe> shapedRecipes;
    public static Multimap<Item, BlastingRecipe> blastingRecipes;
    public static Multimap<Item, SmokingRecipe> smokingRecipes;
    public static Multimap<Item, CampfireCookingRecipe> campfireCookingRecipes;
    public static Multimap<Item, SmeltingRecipe> smeltingRecipes;
    public static List<RecipeHolder<InWorldRecipe>> recipes;

    private static final Map<Recipe<?>, Identifier> SOURCE_IDS = new IdentityHashMap<>();
    private static final Set<CookingSignature> WRAPPED_BLASTING = new HashSet<>();
    private static final Set<CookingSignature> WRAPPED_SMOKING = new HashSet<>();
    private static final Set<CookingSignature> WRAPPED_CAMPFIRE = new HashSet<>();

    private record CookingSignature(List<Identifier> inputs, ItemStackTemplate result) {
        private CookingSignature(Ingredient input, ItemStackTemplate result) {
            this(VanillaRecipesWrap.inputIds(input), result);
        }
    }

    public static List<RecipeHolder<InWorldRecipe>> init(Collection<RecipeHolder<?>> recipes) {
        VanillaRecipesWrap.shapelessRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.shapedRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.blastingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.smokingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.campfireCookingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.smeltingRecipes = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        VanillaRecipesWrap.recipes = new ArrayList<>();
        SOURCE_IDS.clear();
        WRAPPED_BLASTING.clear();
        WRAPPED_SMOKING.clear();
        WRAPPED_CAMPFIRE.clear();
        List<RecipeHolder<?>> sources = new ArrayList<>(recipes);
        sources.sort(Comparator.<RecipeHolder<?>>comparingInt(holder -> wrappingOrder(holder.value()))
            .thenComparing(holder -> holder.id().identifier()));
        for (RecipeHolder<?> recipeHolder : sources) {
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
        for (RecipeHolder<?> holder : sources) {
            SOURCE_IDS.put(holder.value(), holder.id().identifier());
            switch (holder.value()) {
                case ShapelessRecipe recipe -> VanillaRecipesWrap.wrap(recipe);
                case ShapedRecipe recipe -> VanillaRecipesWrap.wrap(recipe);
                case BlastingRecipe recipe -> VanillaRecipesWrap.wrap(recipe);
                case SmokingRecipe recipe -> VanillaRecipesWrap.wrap(recipe);
                case CampfireCookingRecipe recipe -> VanillaRecipesWrap.wrap(recipe);
                case SmeltingRecipe recipe -> VanillaRecipesWrap.wrap(recipe);
                default -> {
                }
            }
        }
        return VanillaRecipesWrap.recipes;
    }

    public static void wrap(@Nullable ShapelessRecipe recipe) {
        if (recipe == null) return;
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        if (ingredients.isEmpty()) return;
        Ingredient first = ingredients.getFirst();
        if (first.isEmpty()) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        if (ingredients.size() == 1 && result.count() > 1 && !first.isCustom()) {
            VanillaRecipesWrap.wrapUnpack(recipe, first, result);
        }
        if (ingredients.size() != 4 && ingredients.size() != 9) return;
        Ingredient common = compressionIngredients(ingredients);
        if (common != null) VanillaRecipesWrap.wrapItemCompress(recipe, common, ingredients.size(), result);
    }

    public static void wrap(@Nullable ShapedRecipe recipe) {
        if (recipe == null) return;
        if (recipe.getHeight() != recipe.getWidth()) return;
        List<Optional<Ingredient>> ingredients = recipe.getIngredients();
        if (ingredients.size() <= 1) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        if (!result.is(ModItemTags.COMPRESS_ITEM)) return;
        if (ingredients.stream().anyMatch(Optional::isEmpty)) return;
        Ingredient common = compressionIngredients(ingredients.stream().map(Optional::orElseThrow).toList());
        if (common != null) VanillaRecipesWrap.wrapItemCompress(recipe, common, ingredients.size(), result);
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
        ResourceKey<Recipe<?>> key = VanillaRecipesWrap.wrappedKey("blasting", recipe, input, 1, result);
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, superHeating));
        WRAPPED_BLASTING.add(new CookingSignature(input, result));
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
        ResourceKey<Recipe<?>> key = VanillaRecipesWrap.wrappedKey("smoking", recipe, input, 1, result);
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, cooking));
        WRAPPED_SMOKING.add(new CookingSignature(input, result));
    }

    public static void wrap(@Nullable CampfireCookingRecipe recipe) {
        if (recipe == null) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        Ingredient input = recipe.input();
        if (input.isEmpty() || input.isCustom()) return;
        if (WRAPPED_SMOKING.contains(new CookingSignature(input, result))) return;
        FastCookingRecipe cooking = FastCookingRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(input.getValues()),
                1,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result)
            .buildRecipe();
        ResourceKey<Recipe<?>> key = VanillaRecipesWrap.wrappedKey("campfire", recipe, input, 1, result);
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, cooking));
        WRAPPED_CAMPFIRE.add(new CookingSignature(input, result));
    }

    public static void wrap(@Nullable SmeltingRecipe recipe) {
        if (recipe == null) return;
        ItemStackTemplate result = recipe.result;
        // noinspection ConstantValue
        if (result == null) return;
        Ingredient input = recipe.input();
        if (input.isEmpty() || input.isCustom()) return;
        CookingSignature signature = new CookingSignature(input, result);
        if (WRAPPED_SMOKING.contains(signature) || WRAPPED_BLASTING.contains(signature)
            || WRAPPED_CAMPFIRE.contains(signature)) return;
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
        ResourceKey<Recipe<?>> key = VanillaRecipesWrap.wrappedKey("smelting", recipe, input, 1, result);
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, superHeating));
    }

    private static void wrapUnpack(Recipe<?> source, Ingredient first, ItemStackTemplate result) {
        UnpackRecipe recipe = UnpackRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(first.getValues()),
                1,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result)
            .buildRecipe();
        ResourceKey<Recipe<?>> key = VanillaRecipesWrap.wrappedKey("unpack", source, first, 1, result);
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, recipe));
    }

    private static @Nullable Ingredient compressionIngredients(List<Ingredient> ingredients) {
        if (ingredients.isEmpty() || ingredients.stream().anyMatch(ingredient -> !ingredient.isSimple())) return null;
        Item[] items = ingredients.getFirst().getValues().stream()
            .filter(item -> ingredients.stream().allMatch(ingredient -> ingredient.test(item.value().getDefaultInstance())))
            .map(Holder::value)
            .distinct()
            .toArray(Item[]::new);
        return items.length == 0 ? null : Ingredient.of(items);
    }

    private static void wrapItemCompress(Recipe<?> source, Ingredient first, int count, ItemStackTemplate result) {
        if (!result.is(Tags.Items.STORAGE_BLOCKS) && !result.is(ModItemTags.COMPRESS_ITEM)) return;
        ItemCompressRecipe recipe = ItemCompressRecipe.builder()
            .requires(new ItemIngredientPredicate(
                Optional.of(first.getValues()),
                count,
                DataComponentMatchers.Builder.components().build()
            ))
            .result(result)
            .buildRecipe();
        ResourceKey<Recipe<?>> key = VanillaRecipesWrap.wrappedKey("compress", source, first, count, result);
        VanillaRecipesWrap.recipes.add(new RecipeHolder<>(key, recipe));
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

    private static List<Identifier> inputIds(Ingredient input) {
        return input.getValues().stream()
            .map(holder -> BuiltInRegistries.ITEM.getKey(holder.value()))
            .distinct()
            .sorted()
            .toList();
    }

    private static ResourceKey<Recipe<?>> wrappedKey(
        String kind, Recipe<?> source, Ingredient input, int count, ItemStackTemplate result
    ) {
        Identifier sourceId = SOURCE_IDS.get(source);
        String path;
        if (sourceId != null) {
            path = "source/%s/%s".formatted(sourceId.getNamespace(), sourceId.getPath());
        } else {
            JsonObject signature = new JsonObject();
            JsonArray inputs = new JsonArray();
            VanillaRecipesWrap.inputIds(input).forEach(id -> inputs.add(id.toString()));
            signature.add("inputs", inputs);
            signature.addProperty("count", count);
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            RegistryAccess registries = server == null
                ? RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY) : server.registryAccess();
            signature.add("result", ItemStackTemplate.CODEC.encodeStart(
                registries.createSerializationContext(JsonOps.INSTANCE), result
            ).getOrThrow());
            path = "anonymous/" + Hashing.sha256().hashString(
                VanillaRecipesWrap.canonicalJson(signature).toString(), StandardCharsets.UTF_8
            );
        }
        return ResourceKey.create(Registries.RECIPE, AnvilCraft.of("generated/vanilla/%s/%s".formatted(kind, path)));
    }

    private static JsonElement canonicalJson(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = new JsonObject();
            element.getAsJsonObject().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> object.add(entry.getKey(), VanillaRecipesWrap.canonicalJson(entry.getValue())));
            return object;
        }
        if (element.isJsonArray()) {
            JsonArray array = new JsonArray();
            element.getAsJsonArray().forEach(value -> array.add(VanillaRecipesWrap.canonicalJson(value)));
            return array;
        }
        return element;
    }
}
