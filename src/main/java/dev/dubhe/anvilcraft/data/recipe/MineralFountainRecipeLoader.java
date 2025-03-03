package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class MineralFountainRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        mineralFountainDeepslate(provider, Blocks.RAW_COPPER_BLOCK, Blocks.DEEPSLATE_COPPER_ORE);
        mineralFountainDeepslate(provider, Blocks.RAW_IRON_BLOCK, Blocks.DEEPSLATE_IRON_ORE);
        mineralFountainDeepslate(provider, Blocks.RAW_GOLD_BLOCK, Blocks.DEEPSLATE_GOLD_ORE);
        mineralFountainDeepslate(provider, ModBlocks.RAW_ZINC.get(), ModBlocks.DEEPSLATE_ZINC_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.RAW_TIN.get(), ModBlocks.DEEPSLATE_TIN_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.RAW_LEAD.get(), ModBlocks.DEEPSLATE_LEAD_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.RAW_SILVER.get(), ModBlocks.DEEPSLATE_SILVER_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.RAW_TITANIUM.get(), ModBlocks.DEEPSLATE_TITANIUM_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.RAW_TUNGSTEN.get(), ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());
        mineralFountainDeepslate(provider, ModBlocks.RAW_URANIUM.get(), ModBlocks.DEEPSLATE_URANIUM_ORE.get());


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

    private static void mineralFountainDeepslate(RegistrateRecipeProvider provider, Block require, Block result) {
        MineralFountainRecipe.builder()
            .needBlock(require)
            .fromBlock(Blocks.DEEPSLATE)
            .toBlock(result)
            .save(provider);
    }
}
