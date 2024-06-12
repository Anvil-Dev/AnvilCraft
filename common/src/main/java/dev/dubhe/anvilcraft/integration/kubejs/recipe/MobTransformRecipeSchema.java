package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import dev.dubhe.anvilcraft.data.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.data.recipe.transform.TagModification;
import dev.dubhe.anvilcraft.data.recipe.transform.TransformOptions;
import dev.dubhe.anvilcraft.data.recipe.transform.TransformResult;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.AnvilCraftRecipeComponents;

import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.rhino.util.HideFromJS;
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
                    "%s/%s".formatted(this.type.id.getPath(), id.getPath()));
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

        /**
         *
         */
        public MobTransformRecipeJs predicate(String nbtPath, String comparator, long value) {
            if (getValue(PREDICATES) == null) setValue(PREDICATES, new NumericTagValuePredicate[0]);
            NumericTagValuePredicate.ValueFunction fn =
                    NumericTagValuePredicate.ValueFunction.valueOf(comparator);
            NumericTagValuePredicate predicate = new NumericTagValuePredicate(nbtPath, fn, value);
            setValue(PREDICATES, ArrayUtils.add(getValue(PREDICATES), predicate));
            save();
            return this;
        }

        /**
         *
         */
        public MobTransformRecipeJs modifyNbt(String path, String op, String value)
                throws CommandSyntaxException {
            if (getValue(TAG_MOD) == null) setValue(TAG_MOD, new TagModification[0]);
            StringReader reader = new StringReader(value);
            TagParser parser = new TagParser(reader);
            setValue(
                    TAG_MOD,
                    ArrayUtils.add(
                            getValue(TAG_MOD),
                            new TagModification(
                                    path, TagModification.ModifyOperation.valueOf(op), -1, parser.readValue())));
            save();
            return this;
        }

        /**
         *
         */
        public MobTransformRecipeJs modifyNbt(String path, String op, String value, int index)
                throws CommandSyntaxException {
            if (getValue(TAG_MOD) == null) setValue(TAG_MOD, new TagModification[0]);
            StringReader reader = new StringReader(value);
            TagParser parser = new TagParser(reader);
            setValue(
                    TAG_MOD,
                    ArrayUtils.add(
                            getValue(TAG_MOD),
                            new TagModification(
                                    path, TagModification.ModifyOperation.valueOf(op), index, parser.readValue())));
            save();
            return this;
        }

        /**
         *
         */
        public MobTransformRecipeJs transformOptions(String name) {
            if (getValue(OPTIONS) == null) setValue(OPTIONS, new TransformOptions[0]);
            setValue(OPTIONS, ArrayUtils.add(getValue(OPTIONS), TransformOptions.valueOf(name)));
            save();
            return this;
        }
    }

    RecipeKey<ResourceLocation> ID = AnvilCraftRecipeComponents.RESOURCE_LOCATION.key("id");
    RecipeKey<EntityType<?>> INPUT =
            AnvilCraftRecipeComponents.RECIPE_ENTITY.key("input").defaultOptional();
    RecipeKey<TransformResult[]> RESULTS = AnvilCraftRecipeComponents.RECIPE_TRANSFORM_RESULT
            .asArray()
            .key("results")
            .defaultOptional();

    RecipeKey<NumericTagValuePredicate[]> PREDICATES = AnvilCraftRecipeComponents.RECIPE_PREDICATES
            .asArray()
            .key("tagPredicates")
            .defaultOptional();

    RecipeKey<TagModification[]> TAG_MOD = AnvilCraftRecipeComponents.RECIPE_TAG_MODIFY
            .asArray()
            .key("tagModification")
            .defaultOptional();

    RecipeKey<TransformOptions[]> OPTIONS = AnvilCraftRecipeComponents.RECIPE_TRANSFORM_OPTIONS
            .asArray()
            .key("transformOptions")
            .defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(
                    MobTransformRecipeJs.class,
                    MobTransformRecipeJs::new,
                    ID,
                    INPUT,
                    RESULTS,
                    PREDICATES,
                    TAG_MOD)
            .constructor((recipe, schemaType, keys, from) -> recipe.id(from.getValue(recipe, ID)), ID);
}
