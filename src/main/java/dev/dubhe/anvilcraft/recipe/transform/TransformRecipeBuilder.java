package dev.dubhe.anvilcraft.recipe.transform;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TransformRecipeBuilder {
    protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    private final EntityType<?> inputType;
    private final List<TransformResult> results = new ArrayList<>();
    private final List<NumericTagValuePredicate> predicates = new ArrayList<>();
    private final List<TagModification> tagModifications = new ArrayList<>();
    private final List<TransformOptions> options = new ArrayList<>();

    public TransformRecipeBuilder(EntityType<?> inputType) {
        this.inputType = inputType;
    }

    public TransformRecipeBuilder unlockedBy(String s, Criterion<?> criterion) {
        criteria.put(s, criterion);
        return this;
    }

    public MobTransformRecipe create() {
        return new MobTransformRecipe(inputType, results, predicates, tagModifications, options);
    }

    public TransformRecipeBuilder to(EntityType<?> res) {
        this.results.add(new TransformResult(res, 1d));
        return this;
    }

    public TransformRecipeBuilder result(EntityType<?> resultEntityType, double d) {
        this.results.add(new TransformResult(resultEntityType, d));
        return this;
    }

    /**
     *
     */
    public TransformRecipeBuilder predicate(@NotNull Consumer<NumericTagValuePredicate.Builder> predicateBuilder) {
        NumericTagValuePredicate.Builder builder = NumericTagValuePredicate.builder();
        predicateBuilder.accept(builder);
        predicates.add(builder.build());
        return this;
    }

    /**
     * 修改生物nbt
     */
    public TransformRecipeBuilder tagModification(@NotNull Consumer<TagModification.Builder> predicateBuilder) {
        TagModification.Builder builder = TagModification.builder();
        predicateBuilder.accept(builder);
        tagModifications.add(builder.build());
        return this;
    }

    /**
     * 生物转化额外选项
     */
    public TransformRecipeBuilder option(TransformOptions option) {
        options.add(option);
        return this;
    }

    public void save(RecipeOutput recipeOutput) {
        save(
            recipeOutput,
            AnvilCraft.of(BuiltInRegistries.ENTITY_TYPE.getKey(inputType).getPath())
                .withPrefix("mob_transform/"));
    }

    public void save(RecipeOutput pRecipeOutput, ResourceLocation pId) {
        Advancement.Builder advancement = pRecipeOutput
            .advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pId))
            .rewards(AdvancementRewards.Builder.recipe(pId))
            .requirements(AdvancementRequirements.Strategy.OR);
        criteria.forEach(advancement::addCriterion);
        MobTransformRecipe recipe = create();
        pRecipeOutput.accept(pId, recipe, advancement.build(pId.withPrefix("recipe/")));
    }
}
