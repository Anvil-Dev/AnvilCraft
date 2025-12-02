package dev.dubhe.anvilcraft.integration.kubejs.recipe.transform;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.recipe.transform.TagModification;
import dev.dubhe.anvilcraft.recipe.transform.TransformOptions;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface MobTransformRecipeSchema {
    @SuppressWarnings({"DataFlowIssue", "unused"})
    class MobTransformKubeRecipe extends KubeRecipe {
        public MobTransformKubeRecipe input(EntityType<?> entityType) {
            setValue(INPUT, entityType);
            save();
            return this;
        }

        public MobTransformKubeRecipe addResult(EntityType<?> entityType, double probability) {
            if (getValue(RESULTS) == null) setValue(RESULTS, new ArrayList<>());
            getValue(RESULTS).add(new TransformResult(entityType, probability));
            save();
            return this;
        }

        public MobTransformKubeRecipe addResult(EntityType<?> entityType) {
            return addResult(entityType, 1);
        }

        public MobTransformKubeRecipe predicate(Consumer<NumericTagValuePredicate.Builder> consumer) {
            if (getValue(NUMERIC_TAG_VALUE_PREDICATES) == null) setValue(NUMERIC_TAG_VALUE_PREDICATES, new ArrayList<>());
            var builder = NumericTagValuePredicate.builder();
            consumer.accept(builder);
            getValue(NUMERIC_TAG_VALUE_PREDICATES).add(builder.build());
            save();
            return this;
        }

        public MobTransformKubeRecipe modification(Consumer<TagModification.Builder> consumer) {
            if (getValue(TAG_MODIFICATIONS) == null) setValue(TAG_MODIFICATIONS, new ArrayList<>());
            var builder = TagModification.builder();
            consumer.accept(builder);
            getValue(TAG_MODIFICATIONS).add(builder.build());
            save();
            return this;
        }

        public MobTransformKubeRecipe transformOption(TransformOptions transformOptions) {
            if (getValue(TRANSFORM_OPTIONS) == null) setValue(TRANSFORM_OPTIONS, new ArrayList<>());
            getValue(TRANSFORM_OPTIONS).add(transformOptions);
            save();
            return this;
        }
    }

    RecipeKey<EntityType<?>> INPUT = AnvilCraftRecipeComponents.ENTITY_TYPE.inputKey("input").defaultOptional();
    RecipeKey<List<TransformResult>> RESULTS = AnvilCraftRecipeComponents.TRANSFORM_RESULT
        .asList().outputKey("results").defaultOptional();
    RecipeKey<List<NumericTagValuePredicate>> NUMERIC_TAG_VALUE_PREDICATES = AnvilCraftRecipeComponents.NUMERIC_TAG_VALUE_PREDICATE
        .asList().otherKey("tagPredicates").defaultOptional();
    RecipeKey<List<TagModification>> TAG_MODIFICATIONS = AnvilCraftRecipeComponents.TAG_MODIFICATION
        .asList().outputKey("tagModifications").defaultOptional();
    RecipeKey<List<TransformOptions>> TRANSFORM_OPTIONS = AnvilCraftRecipeComponents.TRANSFORM_OPTIONS
        .asList().outputKey("transformOptions").defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INPUT, RESULTS, NUMERIC_TAG_VALUE_PREDICATES, TAG_MODIFICATIONS, TRANSFORM_OPTIONS)
        .factory(new KubeRecipeFactory(AnvilCraft.of("mob_transform"), MobTransformKubeRecipe.class, MobTransformKubeRecipe::new))
        .constructor(INPUT, RESULTS, NUMERIC_TAG_VALUE_PREDICATES, TAG_MODIFICATIONS, TRANSFORM_OPTIONS)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
