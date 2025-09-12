package dev.dubhe.anvilcraft.integration.kubejs.recipe.mineral;

import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.BlockStatePredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceBlockStateComponent;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.level.block.Block;

public interface MineralFountainRecipeSchema {
    @SuppressWarnings("unused")
    class MineralFountainKubeRecipe extends KubeRecipe {
        public MineralFountainKubeRecipe needBlock(Block block) {
            setValue(NEED_BLOCK, BlockStatePredicate.builder().of(block).build());
            save();
            return this;
        }

        public MineralFountainKubeRecipe fromBlock(Block block) {
            setValue(FROM_BLOCK, BlockStatePredicate.builder().of(block).build());
            save();
            return this;
        }

        public MineralFountainKubeRecipe toBlock(Block block) {
            setValue(TO_BLOCK, new ChanceBlockState(block.defaultBlockState(), 1.0f));
            save();
            return this;
        }
    }

    RecipeKey<BlockStatePredicate> NEED_BLOCK = BlockStatePredicateComponent.INSTANCE.otherKey("need_block").defaultOptional();
    RecipeKey<BlockStatePredicate> FROM_BLOCK = BlockStatePredicateComponent.INSTANCE.inputKey("from_block").defaultOptional();
    RecipeKey<ChanceBlockState> TO_BLOCK = ChanceBlockStateComponent.INSTANCE.inputKey("to_block").defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(NEED_BLOCK, FROM_BLOCK, TO_BLOCK)
        .factory(new KubeRecipeFactory(AnvilCraft.of("mineral_fountain"), MineralFountainKubeRecipe.class, MineralFountainKubeRecipe::new))
        .constructor(NEED_BLOCK, FROM_BLOCK, TO_BLOCK)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
