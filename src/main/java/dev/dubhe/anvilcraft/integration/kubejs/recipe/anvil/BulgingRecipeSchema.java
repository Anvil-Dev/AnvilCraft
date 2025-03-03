package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BlockComponent;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface BulgingRecipeSchema {
    RecipeKey<List<Ingredient>> INGREDIENTS = IngredientComponent.INGREDIENT.asList().inputKey("ingredients").defaultOptional();
    RecipeKey<List<ChanceItemStack>> RESULTS = AnvilCraftRecipeComponents.CHANCE_ITEM_STACK.asList().inputKey("results").defaultOptional();
    RecipeKey<Block> CAULDRON = BlockComponent.BLOCK.outputKey("cauldron").optional(Blocks.WATER_CAULDRON).alwaysWrite();
    RecipeKey<Boolean> PRODUCE_FLUID = BooleanComponent.BOOLEAN.otherKey("produce_fluid").optional(false).alwaysWrite();
    RecipeKey<Boolean> CONSUME_FLUID = BooleanComponent.BOOLEAN.otherKey("consume_fluid").optional(false).alwaysWrite();
    RecipeKey<Boolean> FROM_WATER = BooleanComponent.BOOLEAN.otherKey("from_water").optional(false).alwaysWrite();
    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULTS, CAULDRON, PRODUCE_FLUID, CONSUME_FLUID, FROM_WATER)
        .factory(new KubeRecipeFactory(AnvilCraft.of("bulging"), BulgingKubeRecipe.class, BulgingKubeRecipe::new))
        .constructor(INGREDIENTS, RESULTS, CAULDRON, PRODUCE_FLUID, CONSUME_FLUID, FROM_WATER)
        .constructor(INGREDIENTS, RESULTS)
        .constructor(INGREDIENTS, RESULTS, CAULDRON, PRODUCE_FLUID, CONSUME_FLUID)
        .constructor(new IDRecipeConstructor())
        .constructor();

    @SuppressWarnings({"DataFlowIssue", "unused"})
    class BulgingKubeRecipe extends AnvilCraftKubeRecipe {
        public BulgingKubeRecipe requires(Ingredient... ingredient) {
            computeIfAbsent(INGREDIENTS, ArrayList::new).addAll(Arrays.stream(ingredient).toList());
            save();
            return this;
        }

        public BulgingKubeRecipe requires(Ingredient ingredient, int count) {
            if (getValue(INGREDIENTS) == null) setValue(INGREDIENTS, new ArrayList<>());
            for (int i = 0; i < count; i++) {
                getValue(INGREDIENTS).add(ingredient);
            }
            save();
            return this;
        }

        public BulgingKubeRecipe result(ItemStack stack, float chance) {
            if (getValue(RESULTS) == null) setValue(RESULTS, new ArrayList<>());
            getValue(RESULTS).add(ChanceItemStack.of(stack).withChance(chance));
            save();
            return this;
        }

        public BulgingKubeRecipe result(ItemStack stack) {
            return result(stack, 1.0f);
        }

        public BulgingKubeRecipe cauldron(Block block) {
            setValue(CAULDRON, block);
            save();
            return this;
        }

        public BulgingKubeRecipe produceFluid(boolean produceFluid) {
            setValue(PRODUCE_FLUID, produceFluid);
            save();
            return this;
        }

        public BulgingKubeRecipe consumeFluid(boolean consumeFluid) {
            setValue(CONSUME_FLUID, consumeFluid);
            save();
            return this;
        }

        public BulgingKubeRecipe fromWater(boolean fromWater) {
            setValue(FROM_WATER, fromWater);
            save();
            return this;
        }

        @Override
        protected void validate() {
            if (computeIfAbsent(INGREDIENTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Inputs is Empty!").source(sourceLine);
            }
            if (computeIfAbsent(RESULTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Result is Empty!").source(sourceLine);
            }
        }
    }
}
