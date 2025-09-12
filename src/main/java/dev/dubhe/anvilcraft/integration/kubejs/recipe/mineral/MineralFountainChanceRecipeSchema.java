package dev.dubhe.anvilcraft.integration.kubejs.recipe.mineral;

import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.BlockStatePredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceBlockStateComponent;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public interface MineralFountainChanceRecipeSchema {
    @SuppressWarnings("unused")
    class MineralFountainChanceKubeRecipe extends KubeRecipe {
        public MineralFountainChanceKubeRecipe dimension(ResourceLocation dimension) {
            setValue(DIMENSION, dimension);
            save();
            return this;
        }

        public MineralFountainChanceKubeRecipe fromBlock(Block block) {
            setValue(FROM_BLOCK, BlockStatePredicate.builder().of(block).build());
            save();
            return this;
        }

        public MineralFountainChanceKubeRecipe toBlock(Block block) {
            setValue(TO_BLOCK, new ChanceBlockState(block.defaultBlockState(), 1.0f));
            save();
            return this;
        }

        public MineralFountainChanceKubeRecipe chance(double chance) {
            setValue(CHANCE, chance);
            save();
            return this;
        }
    }

    RecipeKey<ResourceLocation> DIMENSION = AnvilCraftRecipeComponents.RESOURCE_LOCATION.otherKey("dimension");
    RecipeKey<BlockStatePredicate> FROM_BLOCK = BlockStatePredicateComponent.INSTANCE.inputKey("from_block").defaultOptional();
    RecipeKey<ChanceBlockState> TO_BLOCK = ChanceBlockStateComponent.INSTANCE.inputKey("to_block").defaultOptional();
    RecipeKey<Double> CHANCE = NumberComponent.DOUBLE.otherKey("chance").optional(1.0d);

    RecipeSchema SCHEMA = new RecipeSchema(DIMENSION, FROM_BLOCK, TO_BLOCK, CHANCE)
        .factory(new KubeRecipeFactory(
            AnvilCraft.of("mineral_fountain_chance"),
            MineralFountainChanceKubeRecipe.class,
            MineralFountainChanceKubeRecipe::new
        ))
        .constructor(DIMENSION, FROM_BLOCK, TO_BLOCK, CHANCE)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
