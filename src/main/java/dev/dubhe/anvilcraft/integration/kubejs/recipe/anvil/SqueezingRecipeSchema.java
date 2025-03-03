package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BlockComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public interface SqueezingRecipeSchema {
    RecipeKey<Block> INPUT_BLOCK = BlockComponent.BLOCK.inputKey("input_block").defaultOptional();
    RecipeKey<Block> RESULT_BLOCK = BlockComponent.BLOCK.outputKey("result_block").defaultOptional();
    RecipeKey<Block> CAULDRON = BlockComponent.BLOCK.outputKey("cauldron").defaultOptional();
    RecipeSchema SCHEMA = new RecipeSchema(INPUT_BLOCK, RESULT_BLOCK, CAULDRON)
        .factory(new KubeRecipeFactory(AnvilCraft.of("squeezing"), SqueezingKubeRecipe.class, SqueezingKubeRecipe::new))
        .constructor(INPUT_BLOCK, RESULT_BLOCK, CAULDRON)
        .constructor(INPUT_BLOCK, RESULT_BLOCK)
        .constructor(new IDRecipeConstructor())
        .constructor();

    @SuppressWarnings("unused")
    class SqueezingKubeRecipe extends AnvilCraftKubeRecipe {
        public SqueezingKubeRecipe inputBlock(Block block) {
            setValue(INPUT_BLOCK, block);
            save();
            return this;
        }

        public SqueezingKubeRecipe outputBlock(Block block) {
            setValue(RESULT_BLOCK, block);
            save();
            return this;
        }

        public SqueezingKubeRecipe cauldron(Block cauldron) {
            setValue(CAULDRON, cauldron);
            save();
            return this;
        }

        @Override
        protected void validate() {
            if (getValue(CAULDRON) == null || getValue(CAULDRON) == Blocks.AIR) {
                throw new KubeRuntimeException("input is empty!").source(sourceLine);
            }
            if (getValue(INPUT_BLOCK) == null) {
                throw new KubeRuntimeException("input is empty!").source(sourceLine);
            }
            if (getValue(RESULT_BLOCK) == null) {
                throw new KubeRuntimeException("result_block is empty!").source(sourceLine);
            }
        }
    }
}
