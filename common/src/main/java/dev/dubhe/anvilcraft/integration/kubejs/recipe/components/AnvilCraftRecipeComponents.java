package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import net.minecraft.world.phys.Vec3;

public class AnvilCraftRecipeComponents {
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

    public static final RecipeComponent<Vec3> RECIPE_VEC3 = new RecipeComponent<>() {
        @Override
        public String componentType() {
            return "recipe_vec3";
        }

        @Override
        public Class<?> componentClass() {
            return Vec3.class;
        }

        @Override
        public JsonElement write(RecipeJS recipe, Vec3 value) {
            JsonArray array = new JsonArray();
            array.add(value.x);
            array.add(value.y);
            array.add(value.z);
            return array;
        }

        @Override
        public Vec3 read(RecipeJS recipe, Object from) {
            if (from instanceof JsonArray array) {
                return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
            }
            return null;
        }
    };
}
