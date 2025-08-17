package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.util.VanillaConstants;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BulgingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        bulging(provider, Items.DIRT, Items.CLAY);
        bulging(provider, Items.CRIMSON_FUNGUS, Items.NETHER_WART_BLOCK);
        bulging(provider, Items.WARPED_FUNGUS, Items.WARPED_WART_BLOCK);
        bulging(provider, Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE);
        bulging(provider, Items.BRAIN_CORAL, Items.BRAIN_CORAL_BLOCK);
        bulging(provider, Items.BUBBLE_CORAL, Items.BUBBLE_CORAL_BLOCK);
        bulging(provider, Items.FIRE_CORAL, Items.FIRE_CORAL_BLOCK);
        bulging(provider, Items.HORN_CORAL, Items.HORN_CORAL_BLOCK);
        bulging(provider, Items.TUBE_CORAL, Items.TUBE_CORAL_BLOCK);
        bulging(provider, ModItems.SPONGE_GEMMULE, Items.WET_SPONGE, true);
        bulging(provider, ModItems.FLOUR, ModItems.DOUGH);
        bulging(provider, Items.DRIED_KELP, Items.KELP);
        crystallize(provider, ModItems.SEA_HEART_SHELL_SHARD, ModItems.PRISMARINE_CLUSTER, true);

        VanillaConstants.CONCRETE_POWDERS.forEach(block -> bulging(provider, block, block.concrete));

        VanillaConstants.WEATHERING_COPPERS.forEach(weatheringCopper -> {
            if (!(weatheringCopper instanceof Block block)) return;
            weatheringCopper.getNext(block.defaultBlockState()).ifPresent(
                state -> bulging(provider, block, state.getBlock())
            );
        });

        BulgingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .transform(ModBlocks.CEMENT_CAULDRONS.get(Color.GRAY).get())
            .requires(ModItems.LIME_POWDER, 4)
            .requires(ModBlocks.CINERITE)
            .save(provider, AnvilCraft.of("bulging/cement_cauldron"));

        BulgingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(Items.RED_MUSHROOM)
            .result(Blocks.RED_MUSHROOM_BLOCK)
            .result(Blocks.MUSHROOM_STEM, 0.1f)
            .save(provider);

        BulgingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(Items.BROWN_MUSHROOM)
            .result(Blocks.BROWN_MUSHROOM_BLOCK)
            .result(Blocks.MUSHROOM_STEM, 0.1f)
            .save(provider);
    }

    private static void bulging(RegistrateRecipeProvider provider, ItemLike input, ItemLike result, boolean consumeFluid) {
        BulgingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(input)
            .result(result)
            .consumeFluid(consumeFluid)
            .save(provider);
    }

    private static void bulging(RegistrateRecipeProvider provider, ItemLike input, ItemLike result) {
        bulging(provider, input, result, false);
    }

    private static void crystallize(
        RegistrateRecipeProvider provider, ItemLike input, ItemLike result, boolean consumeFluid) {
        BulgingRecipe.builder()
            .cauldron(Blocks.POWDER_SNOW_CAULDRON)
            .requires(input)
            .result(result)
            .consumeFluid(consumeFluid)
            .save(provider);
    }

    private static void crystallize(RegistrateRecipeProvider provider, ItemLike input, ItemLike result) {
        crystallize(provider, input, result, false);
    }
}
