package dev.dubhe.anvilcraft.recipe.anvil.builder;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractRecipeBuilder<T extends Recipe<?>> implements RecipeBuilder {
    protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    @Override
    public RecipeBuilder unlockedBy(String pName, Criterion<?> pCriterion) {
        criteria.put(pName, pCriterion);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String pGroupName) {
        return this;
    }

    @Override
    public void save(RecipeOutput pRecipeOutput, ResourceLocation pId) {
        validate(pId);
        Advancement.Builder advancement = pRecipeOutput
            .advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pId))
            .rewards(AdvancementRewards.Builder.recipe(pId))
            .requirements(AdvancementRequirements.Strategy.OR);
        criteria.forEach(advancement::addCriterion);
        T recipe = buildRecipe();
        pRecipeOutput.accept(pId, recipe, advancement.build(pId.withPrefix("recipe/")));
    }

    @Override
    public void save(RecipeOutput recipeOutput) {
        save(
            recipeOutput,
            AnvilCraft.of(BuiltInRegistries.ITEM.getKey(getResult()).getPath())
                .withPrefix(getType() + "/"));
    }

    public abstract T buildRecipe();

    public abstract void validate(ResourceLocation pId);

    public abstract String getType();
}
