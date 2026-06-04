package dev.dubhe.anvilcraft.recipe.transform;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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
        this.criteria.put(s, criterion);
        return this;
    }

    public MobTransformRecipe create() {
        return new MobTransformRecipe(this.inputType, this.results, this.predicates, this.tagModifications, this.options);
    }

    public TransformRecipeBuilder to(EntityType<?> res) {
        this.results.add(new TransformResult(res, 1d));
        return this;
    }

    public TransformRecipeBuilder result(EntityType<?> resultEntityType, double d) {
        this.results.add(new TransformResult(resultEntityType, d));
        return this;
    }

    public TransformRecipeBuilder predicate(UnaryOperator<NumericTagValuePredicate.Builder> predicateBuilder) {
        NumericTagValuePredicate.Builder builder = NumericTagValuePredicate.builder();
        builder = predicateBuilder.apply(builder);
        this.predicates.add(builder.build());
        return this;
    }

    /// 修改生物nbt
    public TransformRecipeBuilder tagModification(Consumer<TagModification.Builder> predicateBuilder) {
        TagModification.Builder builder = TagModification.builder();
        predicateBuilder.accept(builder);
        this.tagModifications.add(builder.build());
        return this;
    }

    /// 生物转化额外选项
    public TransformRecipeBuilder option(TransformOptions option) {
        this.options.add(option);
        return this;
    }

    public void save(RecipeOutput recipeOutput) {
        this.save(
            recipeOutput,
            AnvilCraft.of(BuiltInRegistries.ENTITY_TYPE.getKey(this.inputType).getPath())
                .withPrefix("mob_transform/"));
    }

    public void save(RecipeOutput output, Identifier id) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        Advancement.Builder advancement = output
            .advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(key))
            .rewards(AdvancementRewards.Builder.recipe(key))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement::addCriterion);
        MobTransformRecipe recipe = this.create();
        output.accept(key, recipe, advancement.build(id.withPrefix("recipes/")));
    }
}
