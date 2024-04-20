package dev.dubhe.anvilcraft.event;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent;
import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeMap;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModHammerInits;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerEventListener {
    /**
     * 服务器开启事件
     *
     * @param event 事件
     */
    @SubscribeEvent
    public void onServerStarted(@NotNull ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerEventListener.processRecipes(server);
        ModHammerInits.init();
        HammerManager.register();
    }

    @SubscribeEvent
    public void onDataPackReloaded(@NotNull ServerEndDataPackReloadEvent event) {
        MinecraftServer server = event.getServer();
        ServerEventListener.processRecipes(server);
    }

    /**
     * 处理配方
     *
     * @param server 服务器
     */
    public static void processRecipes(@NotNull MinecraftServer server) {
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newRecipeMap = new HashMap<>();
        AnvilRecipeMap anvilRecipes = new AnvilRecipeMap();
        for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry
            : server.getRecipeManager().recipes.entrySet()) {
            RecipeType<?> type = entry.getKey();
            Map<ResourceLocation, Recipe<?>> recipeMap = new HashMap<>();
            for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : entry.getValue().entrySet()) {
                ResourceLocation id = recipeEntry.getKey();
                Recipe<?> recipe = recipeEntry.getValue();
                recipeMap.put(id, recipe);
                if (type != RecipeType.CRAFTING && type != RecipeType.BLASTING && type != RecipeType.SMOKING) continue;
                Pair<ResourceLocation, Recipe<?>> newRecipe = ServerEventListener.processRecipes(id, recipe);
                if (newRecipe == null) continue;
                ResourceLocation location = newRecipe.getFirst();
                Recipe<?> recipe1 = newRecipe.getSecond();
                anvilRecipes.put(location, recipe1);
            }
            if (type == ModRecipeTypes.ANVIL_RECIPE) {
                for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : recipeMap.entrySet()) {
                    ResourceLocation location = recipeEntry.getKey();
                    Recipe<?> recipe = recipeEntry.getValue();
                    anvilRecipes.put(location, recipe);
                }
            } else newRecipeMap.put(type, recipeMap);
        }
        anvilRecipes.sort();
        newRecipeMap.put(ModRecipeTypes.ANVIL_RECIPE, anvilRecipes.unmodifiable());
        newRecipeMap.replaceAll(
            (type, resourceLocationRecipeMap) -> Collections.unmodifiableMap(resourceLocationRecipeMap)
        );
        server.getRecipeManager().recipes = Collections.unmodifiableMap(newRecipeMap);
    }

    /**
     * 处理配方
     *
     * @param id        id
     * @param oldRecipe 原配方
     * @return 生成的配方
     */
    public static @Nullable Pair<ResourceLocation, Recipe<?>> processRecipes(
        ResourceLocation id, @NotNull Recipe<?> oldRecipe
    ) {
        ItemStack result = oldRecipe.getResultItem(new RegistryAccess.ImmutableRegistryAccess(List.of()));
        if (result.is(Items.IRON_TRAPDOOR)) return null;
        if (result.is(Items.PRISMARINE)) return null;
        if (oldRecipe instanceof ShapelessRecipe recipe) {
            if (recipe.getIngredients().size() == 1 && result.getCount() != 1) {
                return smash(result, recipe.getIngredients().get(0), id);
            } else if (isIngredientsSame(recipe.getIngredients())) {
                return compress(result, recipe.getIngredients(), id);
            }
        } else if (oldRecipe instanceof ShapedRecipe recipe) {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (isIngredientsSame(ingredients)) {
                if (recipe.getHeight() != recipe.getWidth()) return null;
                if (recipe.getIngredients().size() != recipe.getWidth() * recipe.getHeight()) return null;
                return compress(result, recipe.getIngredients(), id);
            }
        } else if (oldRecipe instanceof BlastingRecipe recipe) {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.size() != 1) return null;
            return heating(result, ingredients.get(0), id);
        } else if (oldRecipe instanceof SmokingRecipe recipe) {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.size() != 1) return null;
            return smoking(result, ingredients.get(0), id);
        }
        return null;
    }

    private static @NotNull Pair<ResourceLocation, Recipe<?>> heating(
        @NotNull ItemStack result, Ingredient ingredient, @NotNull ResourceLocation id
    ) {
        ResourceLocation location = AnvilCraft.of("heating/" + id.getPath());
        result.setCount(result.getCount() * 2);
        AnvilRecipe recipe1 = new AnvilRecipe(location, result);
        recipe1.addPredicates(
            HasItemIngredient.of(new Vec3(0.0, -1.0, 0.0), ingredient),
            new HasBlock(
                new Vec3(0.0, -1.0, 0.0),
                new HasBlock.ModBlockPredicate().block(Blocks.CAULDRON)
            ),
            new HasBlock(
                new Vec3(0.0, -2.0, 0.0),
                new HasBlock.ModBlockPredicate()
                    .block(ModBlocks.HEATER.get())
                    .property(IPowerComponent.OVERLOAD, false)
            )
        );
        recipe1.addOutcomes(new SpawnItem(new Vec3(0.0, -1.0, 0.0), 1.0, result.copy()));
        return new Pair<>(location, recipe1);
    }

    private static @NotNull Pair<ResourceLocation, Recipe<?>> smoking(
        @NotNull ItemStack result, Ingredient ingredient, @NotNull ResourceLocation id
    ) {
        ResourceLocation location = AnvilCraft.of("cook/" + id.getPath());
        AnvilRecipe recipe1 = new AnvilRecipe(location, result);
        recipe1.addPredicates(
            HasItemIngredient.of(new Vec3(0.0, -1.0, 0.0), ingredient),
            new HasBlock(
                new Vec3(0.0, -1.0, 0.0),
                new HasBlock.ModBlockPredicate().block(Blocks.CAULDRON)
            ),
            new HasBlock(
                new Vec3(0.0, -2.0, 0.0),
                new HasBlock.ModBlockPredicate()
                    .block(BlockTags.CAMPFIRES)
                    .property(CampfireBlock.LIT, true)
            )
        );
        recipe1.addOutcomes(new SpawnItem(new Vec3(0.0, -1.0, 0.0), 1.0, result.copy()));
        return new Pair<>(location, recipe1);
    }

    private static @NotNull Pair<ResourceLocation, Recipe<?>> smash(
        ItemStack result, @NotNull Ingredient ingredient, @NotNull ResourceLocation id
    ) {
        ResourceLocation location = AnvilCraft.of("smash/" + id.getPath());
        AnvilRecipe recipe1 = new AnvilRecipe(location, result);
        recipe1.addPredicates(
            HasItemIngredient.of(Vec3.ZERO, ingredient),
            new HasBlock(new Vec3(0.0, -1.0, 0.0), new HasBlock.ModBlockPredicate().block(Blocks.IRON_TRAPDOOR))
        );
        recipe1.addOutcomes(new SpawnItem(new Vec3(0.0, -1.0, 0.0), 1.0, result.copy()));
        return new Pair<>(location, recipe1);
    }

    @NotNull
    private static Pair<ResourceLocation, Recipe<?>> compress(
        ItemStack result, @NotNull NonNullList<Ingredient> ingredients, @NotNull ResourceLocation id
    ) {
        ResourceLocation location = AnvilCraft.of("compress/" + id.getPath());
        Ingredient ingredient = ingredients.get(0);
        int ingredientCount = ingredients.size();
        AnvilRecipe recipe = new AnvilRecipe(location, result);
        recipe.addPredicates(
            HasItemIngredient.of(new Vec3(0.0, -1.0, 0.0), ingredient, ingredientCount),
            new HasBlock(new Vec3(0.0, -1.0, 0.0), new HasBlock.ModBlockPredicate().block(Blocks.CAULDRON))
        );
        recipe.addOutcomes(new SpawnItem(new Vec3(0.0, -1.0, 0.0), 1.0, result.copy()));
        return new Pair<>(location, recipe);
    }

    private static boolean isIngredientsSame(@NotNull List<Ingredient> ingredients) {
        Ingredient ingredient = ingredients.get(0);
        for (Ingredient ingredient1 : ingredients) {
            if (ingredient1.toJson().equals(ingredient.toJson())) continue;
            return false;
        }
        return true;
    }
}
