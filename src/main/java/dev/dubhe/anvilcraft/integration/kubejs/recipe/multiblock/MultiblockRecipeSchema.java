package dev.dubhe.anvilcraft.integration.kubejs.recipe.multiblock;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;

public interface MultiblockRecipeSchema {
    @SuppressWarnings({"DataFlowIssue", "unused"})
    class MultiblockKubeRecipe extends KubeRecipe {
        public MultiblockKubeRecipe layer(String... layers) {
            if (getValue(BLOCK_PATTERN) == null) setValue(BLOCK_PATTERN, BlockPattern.create());
            getValue(BLOCK_PATTERN).layer(layers);
            save();
            return this;
        }

        public MultiblockKubeRecipe symbol(char symbol, BlockPredicateWithState predicate) {
            if (getValue(BLOCK_PATTERN) == null) setValue(BLOCK_PATTERN, BlockPattern.create());
            getValue(BLOCK_PATTERN).symbol(symbol, predicate);
            save();
            return this;
        }

        public MultiblockKubeRecipe result(ItemStack result) {
            setValue(RESULT, result);
            save();
            return this;
        }
    }

    RecipeKey<BlockPattern> BLOCK_PATTERN = AnvilCraftRecipeComponents.BLOCK_PATTERN.inputKey("pattern").defaultOptional();
    RecipeKey<ItemStack> RESULT = ItemStackComponent.ITEM_STACK.outputKey("result").defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(BLOCK_PATTERN, RESULT)
        .factory(new KubeRecipeFactory(AnvilCraft.of("mulitblock"), MultiblockKubeRecipe.class, MultiblockKubeRecipe::new))
        .constructor(BLOCK_PATTERN, RESULT)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
