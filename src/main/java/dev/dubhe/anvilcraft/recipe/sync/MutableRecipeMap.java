package dev.dubhe.anvilcraft.recipe.sync;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class MutableRecipeMap {
    private Multimap<RecipeType<?>, RecipeHolder<?>> byType;
    private final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

    private MutableRecipeMap(
        Multimap<RecipeType<?>, RecipeHolder<?>> byType,
        Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey
    ) {
        this.byType = byType;
        this.byKey = byKey;
    }

    public static MutableRecipeMap create() {
        return new MutableRecipeMap(MultimapBuilder.hashKeys().hashSetValues().build(), new HashMap<>());
    }

    public MutableRecipeMap add(RecipeHolder<?> recipe) {
        this.byType.put(recipe.value().getType(), recipe);
        this.byKey.put(recipe.id(), recipe);
        return this;
    }

    public MutableRecipeMap addAll(Iterable<RecipeHolder<?>> recipes) {
        for (RecipeHolder<?> recipe : recipes) {
            this.byType.put(recipe.value().getType(), recipe);
            this.byKey.put(recipe.id(), recipe);
        }
        return this;
    }

    public void syncFrom(RecipeMap map) {
        this.byKey.clear();
        this.byType.clear();
        this.addAll(map.values());
    }

    public RecipeMap toImmutable() {
        return new RecipeMap(this.byType, this.byKey);
    }

    public void order(Object2IntMap<ResourceKey<Recipe<?>>> recipePriorities) {
        Int2ObjectRBTreeMap<List<RecipeHolder<?>>> priorityBuilder = new Int2ObjectRBTreeMap<>();
        LinkedListMultimap<RecipeType<?>, RecipeHolder<?>> finalBuilder = LinkedListMultimap.create();

        for (RecipeHolder<?> recipe : this.byKey.values()) {
            int priority = recipePriorities.getOrDefault(recipe.id(), 0);
            priorityBuilder.computeIfAbsent(priority, _ -> new ArrayList<>()).add(recipe);
        }

        for (var list : priorityBuilder.reversed().values()) {
            for (RecipeHolder<?> recipeHolder : list) {
                finalBuilder.put(recipeHolder.value().getType(), recipeHolder);
            }
        }

        this.byType = ArrayListMultimap.create(finalBuilder);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> type) {
        return Util.cast(this.byType.get(type));
    }

    public Collection<RecipeHolder<?>> getRecipes() {
        return this.byKey.values();
    }

    public @Nullable RecipeHolder<?> byKey(ResourceKey<Recipe<?>> recipeId) {
        return this.byKey.get(recipeId);
    }

    private <T extends Recipe<?>> @Nullable RecipeHolder<T> byKeyTyped(RecipeType<T> type, ResourceKey<Recipe<?>> recipeId) {
        RecipeHolder<?> recipe = this.byKey(recipeId);
        return recipe != null && recipe.value().getType().equals(type) ? Util.cast(recipe) : null;
    }

    public <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> getRecipesFor(
        RecipeType<T> type,
        I container,
        Level level
    ) {
        return container.isEmpty()
               ? Stream.empty()
               : this.byType(type).stream().filter(r -> r.value().matches(container, level));
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
        RecipeType<T> type,
        I input,
        Level level,
        @Nullable ResourceKey<Recipe<?>> recipeHint
    ) {
        RecipeHolder<T> hintedRecipe = recipeHint != null ? this.byKeyTyped(type, recipeHint) : null;
        return this.getRecipeFor(type, input, level, hintedRecipe);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
        RecipeType<T> type,
        I input,
        Level level,
        @Nullable RecipeHolder<T> recipeHint
    ) {
        return recipeHint != null && recipeHint.value().matches(input, level)
               ? Optional.of(recipeHint)
               : this.getRecipeFor(type, input, level);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level) {
        return this.getRecipesFor(type, input, level).findFirst();
    }

    public void clear() {
        this.byType.clear();
        this.byKey.clear();
    }
}
