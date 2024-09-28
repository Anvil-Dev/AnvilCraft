package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class MineralFountainRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        mineralFountainDeepslate(provider, Blocks.DEEPSLATE_COPPER_ORE);
        mineralFountainDeepslate(provider, Blocks.DEEPSLATE_IRON_ORE);
        mineralFountainDeepslate(provider, Blocks.DEEPSLATE_GOLD_ORE);
        mineralFountainDeepslate(provider, ModBlocks.DEEPSLATE_ZINC_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.DEEPSLATE_TIN_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.DEEPSLATE_LEAD_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.DEEPSLATE_SILVER_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.DEEPSLATE_TITANIUM_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.DEEPSLATE_URANIUM_ORE.get());


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

        MineralFountainChanceRecipe.builder()
                .dimension(Level.NETHER.location())
                .fromBlock(Blocks.DEEPSLATE)
                .toBlock(ModBlocks.EARTH_CORE_SHARD_ORE.get())
                .chance(0.2)
                .save(provider);

        MineralFountainChanceRecipe.builder()
                .dimension(Level.END.location())
                .fromBlock(Blocks.DEEPSLATE)
                .toBlock(ModBlocks.VOID_STONE.get())
                .chance(0.2)
                .save(provider);
    }

    private static void mineralFountainDeepslate(RegistrateRecipeProvider provider, Block block) {
        MineralFountainRecipe.builder()
                .needBlock(block)
                .fromBlock(Blocks.DEEPSLATE)
                .toBlock(block)
                .save(provider);
    }
}
