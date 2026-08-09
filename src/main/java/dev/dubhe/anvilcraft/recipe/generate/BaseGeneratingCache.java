package dev.dubhe.anvilcraft.recipe.generate;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public abstract class BaseGeneratingCache<T extends Recipe<?>> {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    protected final HolderLookup.Provider registries;
    protected final String recipeId;
    protected final String recipeName;

    protected BaseGeneratingCache(HolderLookup.Provider registries, String recipeId, String recipeName) {
        this.registries = registries;
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        BaseGeneratingCache.logger().debug("Initializing {} building cache", recipeName);
    }

    protected static Logger logger() {
        return LoggerFactory.getLogger(BaseGeneratingCache.STACK_WALKER.getCallerClass());
    }

    protected ResourceKey<Recipe<?>> generateRecipeId(String type, Item recipeInput, Item recipeResult) {
        Identifier inputId = BuiltInRegistries.ITEM.getKey(recipeInput);
        Identifier resultId = BuiltInRegistries.ITEM.getKey(recipeResult);
        BaseGeneratingCache.logger().debug("Generating {} for {}", this.recipeName, resultId);
        Identifier newId = AnvilCraft.of("%s/generated/%s_from_%s_for_%s".formatted(
            this.recipeId, resultId.toString().replace(':', '_'), inputId.toString().replace(':', '_'), type));
        BaseGeneratingCache.logger().debug("The generated recipe id is {}", newId);
        return ResourceKey.create(Registries.RECIPE, newId);
    }

    public abstract Optional<List<RecipeHolder<T>>> buildRecipes();
}
