package dev.dubhe.anvilcraft.integration.kubejs.recipe.mineral;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BlockComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.level.block.Block;

public interface MineralFountainRecipeSchema {
    RecipeKey<Block> NEED_BLOCK = BlockComponent.BLOCK.otherKey("need_block").defaultOptional();
    RecipeKey<Block> FROM_BLOCK = BlockComponent.BLOCK.inputKey("from_block").defaultOptional();
    RecipeKey<Block> TO_BLOCK = BlockComponent.BLOCK.inputKey("to_block").defaultOptional();
    RecipeSchema SCHEMA = new RecipeSchema(NEED_BLOCK, FROM_BLOCK, TO_BLOCK)
        .factory(new KubeRecipeFactory(AnvilCraft.of("mineral_fountain"), MineralFountainKubeRecipe.class, MineralFountainKubeRecipe::new))
        .constructor(NEED_BLOCK, FROM_BLOCK, TO_BLOCK)
        .constructor(new IDRecipeConstructor())
        .constructor();

    @SuppressWarnings("unused")
    class MineralFountainKubeRecipe extends KubeRecipe {
        public MineralFountainKubeRecipe needBlock(Block block) {
            setValue(NEED_BLOCK, block);
            save();
            return this;
        }

        public MineralFountainKubeRecipe fromBlock(Block block) {
            setValue(FROM_BLOCK, block);
            save();
            return this;
        }

        public MineralFountainKubeRecipe toBlock(Block block) {
            setValue(TO_BLOCK, block);
            save();
            return this;
        }
    }
}
