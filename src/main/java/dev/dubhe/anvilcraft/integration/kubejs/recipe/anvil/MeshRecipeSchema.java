package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public interface MeshRecipeSchema {
    RecipeKey<Ingredient> INPUT = IngredientComponent.INGREDIENT.inputKey("input").defaultOptional();
    RecipeKey<ItemStack> RESULT = ItemStackComponent.STRICT_ITEM_STACK.outputKey("result").defaultOptional();
    RecipeKey<NumberProvider> RESULT_AMOUNT = AnvilCraftRecipeComponents.NUMBER_PROVIDER.outputKey("result_amount")
        .optional(ConstantValue.exactly(1))
        .alwaysWrite();
    RecipeSchema SCHEMA = new RecipeSchema(INPUT, RESULT, RESULT_AMOUNT)
        .factory(new KubeRecipeFactory(AnvilCraft.of("mesh"), MeshKubeRecipe.class, MeshKubeRecipe::new))
        .constructor(INPUT, RESULT, RESULT_AMOUNT)
        .constructor(INPUT, RESULT)
        .constructor(new IDRecipeConstructor())
        .constructor();

    @SuppressWarnings({"unused"})
    class MeshKubeRecipe extends AnvilCraftKubeRecipe {
        public MeshKubeRecipe input(Ingredient ingredient) {
            setValue(INPUT, ingredient);
            save();
            return this;
        }

        public MeshKubeRecipe result(ItemStack result) {
            setValue(RESULT, result);
            save();
            return this;
        }

        public MeshKubeRecipe resultAmount(NumberProvider amount) {
            setValue(RESULT_AMOUNT, amount);
            save();
            return this;
        }

        public MeshKubeRecipe chanceOutput(int count, float chance) {
            return resultAmount(BinomialDistributionGenerator.binomial(count, chance));
        }

        public MeshKubeRecipe chanceOutput(float chance) {
            return chanceOutput(1, chance);
        }

        public MeshKubeRecipe uniformOutput(int min, float max) {
            return resultAmount(UniformGenerator.between(min, max));
        }

        @Override
        protected void validate() {
            if (getValue(INPUT) == null) {
                throw new KubeRuntimeException("input is empty!").source(sourceLine);
            }
            if (getValue(RESULT) == null) {
                throw new KubeRuntimeException("result is empty!").source(sourceLine);
            }
            if (getValue(RESULT_AMOUNT) == null) {
                throw new KubeRuntimeException("result_amount is empty!").source(sourceLine);
            }
        }
    }
}
