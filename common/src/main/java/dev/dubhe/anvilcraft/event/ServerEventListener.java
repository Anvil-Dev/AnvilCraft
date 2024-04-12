package dev.dubhe.anvilcraft.event;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent;
import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.init.ModHammerInits;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        Map<ResourceLocation, Recipe<?>> newRecipes = new HashMap<>();
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newRecipeMap = new HashMap<>();
        for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry
            : server.getRecipeManager().recipes.entrySet()) {
            RecipeType<?> type = entry.getKey();
            Map<ResourceLocation, Recipe<?>> recipeMap = new HashMap<>();
            for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : entry.getValue().entrySet()) {
                ResourceLocation id = recipeEntry.getKey();
                Recipe<?> recipe = recipeEntry.getValue();
                recipeMap.put(id, recipe);
                if (type == RecipeType.CRAFTING) {
                    Pair<ResourceLocation, Recipe<?>> newRecipe = ServerEventListener.processRecipes(id, recipe);
                    if (newRecipe != null) newRecipes.put(newRecipe.getFirst(), newRecipe.getSecond());
                }
            }
            newRecipeMap.put(type, recipeMap);
        }
        for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : newRecipes.entrySet()) {
            ResourceLocation location = recipeEntry.getKey();
            Recipe<?> recipe = recipeEntry.getValue();
            RecipeType<?> type = recipe.getType();
            Map<ResourceLocation, Recipe<?>> recipeMap = newRecipeMap.getOrDefault(type, new HashMap<>());
            recipeMap.put(location, recipe);
            newRecipeMap.putIfAbsent(type, recipeMap);
        }
        newRecipeMap.replaceAll((type, resourceLocationRecipeMap) -> ImmutableMap.copyOf(resourceLocationRecipeMap));
        newRecipeMap = ImmutableMap.copyOf(newRecipeMap);
        server.getRecipeManager().recipes = newRecipeMap;
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
        ResourceLocation location = AnvilCraft.of("compress/" + id.getPath());
        if (oldRecipe instanceof ShapelessRecipe recipe) {
            if (recipe.getIngredients().size() == 1 && result.getCount() != 1) {
                location = AnvilCraft.of("smash/" + id.getPath());
                AnvilRecipe recipe1 = new AnvilRecipe(location, result);
                Ingredient ingredient = recipe.getIngredients().get(0);
                recipe1.addPredicates(
                    HasItemIngredient.of(Vec3.ZERO, ingredient),
                    new HasBlock(new Vec3(0.0, -1.0, 0.0), BlockPredicate.Builder.block()
                        .of(Blocks.IRON_TRAPDOOR).build())
                );
                recipe1.addOutcomes(new SpawnItem(new Vec3(0.0, -1.0, 0.0), 1.0, result.copy()));
                return new Pair<>(location, recipe1);
            } else if (isIngredientsSame(recipe.getIngredients())) {
                return getResourceLocationRecipePair(result, location, recipe.getIngredients());
            }
        } else if (oldRecipe instanceof ShapedRecipe recipe) {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (isIngredientsSame(ingredients)) {
                if (recipe.getHeight() != recipe.getWidth()) return null;
                if (recipe.getIngredients().size() != recipe.getWidth() * recipe.getHeight()) return null;
                return getResourceLocationRecipePair(result, location, recipe.getIngredients());
            }
        }
        return null;
    }

    @NotNull
    private static Pair<ResourceLocation, Recipe<?>> getResourceLocationRecipePair(
        ItemStack result, ResourceLocation location, @NotNull NonNullList<Ingredient> ingredients
    ) {
        Ingredient ingredient = ingredients.get(0);
        int ingredientCount = ingredients.size();
        AnvilRecipe recipe = new AnvilRecipe(location, result);
        recipe.addPredicates(
            HasItemIngredient.of(new Vec3(0.0, -1.0, 0.0), ingredient, ingredientCount),
            new HasBlock(new Vec3(0.0, -1.0, 0.0), BlockPredicate.Builder.block().of(Blocks.CAULDRON).build())
        );
        recipe.addOutcomes(new SpawnItem(new Vec3(0.0, -1.0, 0.0), 1.0, result.copy()));
        return new Pair<>(location, recipe);
    }

    /**
     * 原料相同
     *
     * @param ingredients 原料
     * @return 是否相同
     */
    public static boolean isIngredientsSame(@NotNull List<Ingredient> ingredients) {
        Ingredient ingredient = ingredients.get(0);
        for (Ingredient ingredient1 : ingredients) {
            if (ingredient1.toJson().equals(ingredient.toJson())) continue;
            return false;
        }
        return true;
    }
}
