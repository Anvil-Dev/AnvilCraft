package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.typings.desc.DescriptionContext;
import dev.latvian.mods.kubejs.typings.desc.TypeDescJS;
import net.minecraft.resources.ResourceLocation;

public class AnvilCraftRecipeComponents {
    public static final RecipeComponent<ResourceLocation> RESOURCE_LOCATION = new RecipeComponent<>() {
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
            return from instanceof CharSequence c ? ResourceLocation.tryParse(c.toString()) : ResourceLocation.tryParse(String.valueOf(from));
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
            System.out.println(from);
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

}
