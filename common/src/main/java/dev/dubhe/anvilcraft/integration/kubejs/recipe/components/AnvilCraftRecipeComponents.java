package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.data.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.data.recipe.transform.TagModification;
import dev.dubhe.anvilcraft.data.recipe.transform.TransformOptions;
import dev.dubhe.anvilcraft.data.recipe.transform.TransformResult;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.typings.desc.DescriptionContext;
import dev.latvian.mods.kubejs.typings.desc.TypeDescJS;

import java.util.Optional;

public class AnvilCraftRecipeComponents {
    public static final RecipeComponent<ResourceLocation> RESOURCE_LOCATION =
            new RecipeComponent<>() {
                @Override
                public String componentType() {
                    return "resource_location";
                }

                @Override
                public Class<?> componentClass() {
                    return ResourceLocation.class;
                }

                @Override
                public TypeDescJS constructorDescription(DescriptionContext ctx) {
                    return TypeDescJS.STRING;
                }

                @Override
                public JsonElement write(RecipeJS recipe, ResourceLocation value) {
                    return new JsonPrimitive(value.toString());
                }

                @Override
                public ResourceLocation read(RecipeJS recipe, Object from) {
                    return from instanceof CharSequence c
                            ? ResourceLocation.tryParse(c.toString())
                            : ResourceLocation.tryParse(String.valueOf(from));
                }

                @Override
                public String toString() {
                    return componentType();
                }
            };

    public static final RecipeComponent<RecipeOutcome> RECIPE_OUTCOME = new RecipeComponent<>() {
        @Override
        public String componentType() {
            return "recipe_outcome";
        }

        @Override
        public Class<?> componentClass() {
            return RecipeOutcome.class;
        }

        @Override
        public JsonElement write(RecipeJS recipe, RecipeOutcome value) {
            return value.toJson();
        }

        @Override
        public RecipeOutcome read(RecipeJS recipe, Object from) {
            if (from instanceof JsonObject jsonObject) {
                return RecipeOutcome.fromJson(jsonObject);
            }
            return null;
        }
    };

    public static final RecipeComponent<RecipePredicate> RECIPE_PREDICATE = new RecipeComponent<>() {
        @Override
        public String componentType() {
            return "recipe_predicate";
        }

        @Override
        public Class<?> componentClass() {
            return RecipePredicate.class;
        }

        @Override
        public JsonElement write(RecipeJS recipe, RecipePredicate value) {
            return value.toJson();
        }

        @Override
        public RecipePredicate read(RecipeJS recipe, Object from) {
            if (from instanceof JsonObject jsonObject) {
                return RecipePredicate.fromJson(jsonObject);
            }
            return null;
        }
    };

    public static final RecipeComponent<EntityType<?>> RECIPE_ENTITY = new RecipeComponent<>() {
        @Override
        public String componentType() {
            return "entity_type";
        }

        @Override
        public Class<?> componentClass() {
            return EntityType.class;
        }

        @Override
        public JsonElement write(RecipeJS recipe, EntityType<?> value) {
            return new JsonPrimitive(EntityType.getKey(value).toString());
        }

        @Override
        public EntityType<?> read(RecipeJS recipe, Object from) {
            Optional<EntityType<?>> entityType = from instanceof CharSequence c
                    ? EntityType.byString(c.toString())
                    : EntityType.byString(String.valueOf(from));
            return entityType.orElse(null);
        }
    };

    public static final RecipeComponent<TransformResult> RECIPE_TRANSFORM_RESULT =
            new RecipeComponent<>() {
                @Override
                public String componentType() {
                    return "recipe_transform_result";
                }

                @Override
                public Class<?> componentClass() {
                    return TransformResult.class;
                }

                @Override
                public JsonElement write(RecipeJS recipe, TransformResult value) {
                    return value.toJson();
                }

                @Override
                public TransformResult read(RecipeJS recipe, Object from) {
                    if (from instanceof JsonObject jsonObject) {
                        return TransformResult.fromJson(jsonObject);
                    }
                    return null;
                }
            };

    public static final RecipeComponent<NumericTagValuePredicate> RECIPE_PREDICATES =
            new RecipeComponent<>() {
                @Override
                public String componentType() {
                    return "recipe_predicates";
                }

                @Override
                public Class<?> componentClass() {
                    return NumericTagValuePredicate.class;
                }

                @Override
                public JsonElement write(RecipeJS recipe, NumericTagValuePredicate value) {
                    return value.toJson();
                }

                @Override
                public NumericTagValuePredicate read(RecipeJS recipe, Object from) {
                    if (from instanceof JsonObject jsonObject) {
                        return NumericTagValuePredicate.fromJson(jsonObject);
                    }
                    return null;
                }
            };

    public static final RecipeComponent<TagModification> RECIPE_TAG_MODIFY = new RecipeComponent<>() {
        @Override
        public String componentType() {
            return "recipe_predicates";
        }

        @Override
        public Class<?> componentClass() {
            return TagModification.class;
        }

        @Override
        public JsonElement write(RecipeJS recipe, TagModification value) {
            return value.toJson();
        }

        @Override
        public TagModification read(RecipeJS recipe, Object from) {
            if (from instanceof JsonObject jsonObject) {
                return TagModification.fromJson(jsonObject);
            }
            return null;
        }
    };
    public static final RecipeComponent<TransformOptions> RECIPE_TRANSFORM_OPTIONS =
            new RecipeComponent<>() {
                @Override
                public String componentType() {
                    return "recipe_predicates";
                }

                @Override
                public Class<?> componentClass() {
                    return TagModification.class;
                }

                @Override
                public JsonElement write(RecipeJS recipe, TransformOptions value) {
                    return value.toJson();
                }

                @Override
                public TransformOptions read(RecipeJS recipe, Object from) {
                    if (from instanceof JsonObject jsonObject) {
                        return TransformOptions.fromJson(jsonObject);
                    }
                    return null;
                }
            };
}
