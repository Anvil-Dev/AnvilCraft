package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import dev.dubhe.anvilcraft.data.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.data.recipe.transform.TransformResult;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.AnvilCraftRecipeComponents;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.ArrayUtils;

/**
 * 生物转换配方架构
 */
public interface MobTransformRecipeSchema {
    @SuppressWarnings("unused")
    class MobTransformRecipeJs extends RecipeJS {
        @HideFromJS
        @Override
        public RecipeJS id(ResourceLocation id) {
            this.id = new ResourceLocation(
                    id.getNamespace().equals("minecraft") ? this.type.id.getNamespace() : id.getNamespace(),
                    "%s/%s".formatted(this.type.id.getPath(), id.getPath())
            );
            setValue(ID, this.id);
            save();
            return this;
        }

        /**
         * KubeJS
         */
        public MobTransformRecipeJs input(EntityType<?> entityType) {
            setValue(INPUT, entityType);
            save();
            return this;
        }

        /**
         * KubeJS
         */
        public MobTransformRecipeJs addOutput(EntityType<?> entityType) {
            return addOutput(entityType, 1.0);
        }

        /**
         * KubeJS
         */
        public MobTransformRecipeJs addOutput(EntityType<?> entityType, double probability) {
            if (getValue(RESULTS) == null) setValue(RESULTS, new TransformResult[0]);
            TransformResult result = new TransformResult(entityType, probability);
            setValue(RESULTS, ArrayUtils.add(getValue(RESULTS), result));
            save();
            return this;
        }

        public MobTransformRecipeJs predicate(String nbtPath, String comparator, long value) {
            if (getValue(PREDICATES) == null) setValue(PREDICATES, new NumericTagValuePredicate[0]);
            NumericTagValuePredicate.ValueFunction fn = NumericTagValuePredicate.ValueFunction.valueOf(comparator);
            NumericTagValuePredicate predicate = new NumericTagValuePredicate(nbtPath, fn, value);
            setValue(PREDICATES, ArrayUtils.add(getValue(PREDICATES), predicate));
            save();
            return this;
        }
    }

    RecipeKey<ResourceLocation> ID = AnvilCraftRecipeComponents.RESOURCE_LOCATION
            .key("id");
    RecipeKey<EntityType<?>> INPUT = AnvilCraftRecipeComponents.RECIPE_ENTITY
            .key("input").defaultOptional();
    RecipeKey<TransformResult[]> RESULTS = AnvilCraftRecipeComponents.RECIPE_TRANSFORM_RESULT
            .asArray().key("results").defaultOptional();

    RecipeKey<NumericTagValuePredicate[]> PREDICATES = AnvilCraftRecipeComponents.RECIPE_PREDICATES
            .asArray().key("tagPredicates").defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(
            MobTransformRecipeJs.class, MobTransformRecipeJs::new, ID, INPUT, RESULTS, PREDICATES
    ).constructor((recipe, schemaType, keys, from) -> recipe.id(from.getValue(recipe, ID)), ID);
}
