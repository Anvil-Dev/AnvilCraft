package dev.dubhe.anvilcraft.integration.kubejs.recipe.mineral;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BlockComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public interface MineralFountainChanceRecipeSchema {
    RecipeKey<ResourceLocation> DIMENSION = AnvilCraftRecipeComponents.RESOURCE_LOCATION.otherKey("dimension");
    RecipeKey<Block> FROM_BLOCK = BlockComponent.BLOCK.inputKey("from_block").defaultOptional();
    RecipeKey<Block> TO_BLOCK = BlockComponent.BLOCK.inputKey("to_block").defaultOptional();
    RecipeKey<Double> CHANCE = NumberComponent.DOUBLE.otherKey("chance").optional(1d);
    RecipeSchema SCHEMA = new RecipeSchema(DIMENSION, FROM_BLOCK, TO_BLOCK, CHANCE)
        .factory(new KubeRecipeFactory(AnvilCraft.of("mineral_fountain_chance"), MineralFountainChanceKubeRecipe.class, MineralFountainChanceKubeRecipe::new))
        .constructor(DIMENSION, FROM_BLOCK, TO_BLOCK, CHANCE)
        .constructor(new IDRecipeConstructor())
        .constructor();

    @SuppressWarnings("unused")
    class MineralFountainChanceKubeRecipe extends KubeRecipe {
        public MineralFountainChanceKubeRecipe dimension(ResourceLocation dimension) {
            setValue(DIMENSION, dimension);
            save();
            return this;
        }

        public MineralFountainChanceKubeRecipe fromBlock(Block block) {
            setValue(FROM_BLOCK, block);
            save();
            return this;
        }

        public MineralFountainChanceKubeRecipe toBlock(Block block) {
            setValue(TO_BLOCK, block);
            save();
            return this;
        }

        public MineralFountainChanceKubeRecipe chance(double chance) {
            setValue(CHANCE, chance);
            save();
            return this;
        }
    }
}
