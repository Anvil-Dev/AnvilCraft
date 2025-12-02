package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.IngredientListComponent;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public interface JewelCraftingRecipeSchema {
    @SuppressWarnings({"DataFlowIssue", "unused"})
    class JewelCraftingKubeRecipe extends KubeRecipe {
        public JewelCraftingKubeRecipe requires(Ingredient ingredient, int count) {
            if (getValue(INGREDIENTS) == null) setValue(INGREDIENTS, new ArrayList<>());
            for (int i = 0; i < count; i++) {
                getValue(INGREDIENTS).add(ingredient);
            }
            save();
            return this;
        }

        public JewelCraftingKubeRecipe requires(Ingredient ingredient) {
            return requires(ingredient, 1);
        }

        public JewelCraftingKubeRecipe result(ItemStack result) {
            setValue(RESULT, result);
            save();
            return this;
        }
    }

    RecipeKey<List<Ingredient>> INGREDIENTS = IngredientListComponent.TYPE.inputKey("ingredients").defaultOptional();
    RecipeKey<ItemStack> RESULT = ItemStackComponent.ITEM_STACK.inputKey("result").defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULT)
        .factory(new KubeRecipeFactory(AnvilCraft.of("jewel_crafting"), JewelCraftingKubeRecipe.class, JewelCraftingKubeRecipe::new))
        .constructor(INGREDIENTS, RESULT)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
