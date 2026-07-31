package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class MineralFountainRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        MineralFountainRecipeLoader.mineralFountainDeepslate(provider, Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER, Blocks.DEEPSLATE_COPPER_ORE);
        MineralFountainRecipeLoader.mineralFountainDeepslate(provider, Tags.Blocks.STORAGE_BLOCKS_RAW_IRON, Blocks.DEEPSLATE_IRON_ORE);
        MineralFountainRecipeLoader.mineralFountainDeepslate(provider, Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD, Blocks.DEEPSLATE_GOLD_ORE);
        MineralFountainRecipeLoader.mineralFountainDeepslate(
            provider, ModBlockTags.STORAGE_BLOCKS_RAW_ZINC, ModBlocks.DEEPSLATE_ZINC_ORE.get());
        MineralFountainRecipeLoader.mineralFountainDeepslate(
            provider, ModBlockTags.STORAGE_BLOCKS_RAW_TIN, ModBlocks.DEEPSLATE_TIN_ORE.get());
        MineralFountainRecipeLoader.mineralFountainDeepslate(
            provider, ModBlockTags.STORAGE_BLOCKS_RAW_LEAD, ModBlocks.DEEPSLATE_LEAD_ORE.get());
        MineralFountainRecipeLoader.mineralFountainDeepslate(
            provider, ModBlockTags.STORAGE_BLOCKS_RAW_SILVER, ModBlocks.DEEPSLATE_SILVER_ORE.get());
        MineralFountainRecipeLoader.mineralFountainDeepslate(
            provider, ModBlockTags.STORAGE_BLOCKS_RAW_TITANIUM, ModBlocks.DEEPSLATE_TITANIUM_ORE.get());
        MineralFountainRecipeLoader.mineralFountainDeepslate(
            provider, ModBlockTags.STORAGE_BLOCKS_RAW_TUNGSTEN, ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());
        MineralFountainRecipeLoader.mineralFountainDeepslate(
            provider, ModBlockTags.STORAGE_BLOCKS_RAW_URANIUM, ModBlocks.DEEPSLATE_URANIUM_ORE.get());


        MineralFountainChanceRecipe.builder()
            .dimension(Level.OVERWORLD.identifier())
            .fromBlock(Blocks.DEEPSLATE)
            .toBlock(ModBlocks.VOID_STONE.get(), 0.01F)
            .save(provider);

        MineralFountainChanceRecipe.builder()
            .dimension(Level.OVERWORLD.identifier())
            .fromBlock(Blocks.DEEPSLATE)
            .toBlock(ModBlocks.EARTH_CORE_SHARD_ORE.get(), 0.01F)
            .save(provider);

        MineralFountainChanceRecipe.builder()
            .dimension(Level.NETHER.identifier())
            .fromBlock(Blocks.DEEPSLATE)
            .toBlock(ModBlocks.EARTH_CORE_SHARD_ORE.get(), 0.1F)
            .save(provider);

        MineralFountainChanceRecipe.builder()
            .dimension(Level.END.identifier())
            .fromBlock(Blocks.DEEPSLATE)
            .toBlock(ModBlocks.VOID_STONE.get(), 0.1F)
            .save(provider);
    }

    private static void mineralFountainDeepslate(RegistrumRecipeProvider provider, TagKey<Block> require, Block result) {
        HolderGetter<Block> blocks = provider.getRegistries().lookupOrThrow(Registries.BLOCK);
        MineralFountainRecipe.builder()
            .needBlock(blocks, require)
            .fromBlock(Blocks.DEEPSLATE)
            .toBlock(result)
            .save(provider);
    }
}
