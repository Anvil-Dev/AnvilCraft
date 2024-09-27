package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class MineralFountainRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        MineralFountainRecipe.builder()
                .needBlock(Blocks.DEEPSLATE_IRON_ORE)
                .fromBlock(Blocks.DEEPSLATE)
                .toBlock(Blocks.DEEPSLATE_IRON_ORE)
                .save(provider);

        MineralFountainChanceRecipe.builder()
                .dimension(Level.OVERWORLD.location())
                .fromBlock(Blocks.DEEPSLATE)
                .toBlock(ModBlocks.VOID_STONE.get())
                .chance(0.1)
                .save(provider);

        MineralFountainChanceRecipe.builder()
                .dimension(Level.OVERWORLD.location())
                .fromBlock(Blocks.DEEPSLATE)
                .toBlock(ModBlocks.EARTH_CORE_SHARD_ORE.get())
                .chance(0.1)
                .save(provider);
    }
}
