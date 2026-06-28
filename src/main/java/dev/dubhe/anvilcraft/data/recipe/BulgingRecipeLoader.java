package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.util.VanillaConstants;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForgeMod;

public class BulgingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        BulgingRecipeLoader.bulging(provider, Items.DIRT, Items.CLAY);
        BulgingRecipeLoader.bulging(provider, Items.CRIMSON_FUNGUS, Items.NETHER_WART_BLOCK);
        BulgingRecipeLoader.bulging(provider, Items.WARPED_FUNGUS, Items.WARPED_WART_BLOCK);
        BulgingRecipeLoader.bulging(provider, Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE);
        BulgingRecipeLoader.bulging(provider, Items.BRAIN_CORAL, Items.BRAIN_CORAL_BLOCK);
        BulgingRecipeLoader.bulging(provider, Items.BUBBLE_CORAL, Items.BUBBLE_CORAL_BLOCK);
        BulgingRecipeLoader.bulging(provider, Items.FIRE_CORAL, Items.FIRE_CORAL_BLOCK);
        BulgingRecipeLoader.bulging(provider, Items.HORN_CORAL, Items.HORN_CORAL_BLOCK);
        BulgingRecipeLoader.bulging(provider, Items.TUBE_CORAL, Items.TUBE_CORAL_BLOCK);
        BulgingRecipeLoader.bulging(provider, ModItems.SPONGE_GEMMULE, Items.WET_SPONGE, 250);
        BulgingRecipeLoader.bulging(provider, ModItemTags.FLOUR, ModFoodItems.DOUGH);
        BulgingRecipeLoader.bulging(provider, Items.DRIED_KELP, Items.KELP);

        VanillaConstants.CONCRETE_POWDERS.forEach(block -> bulging(provider, block, block.concrete));

        VanillaConstants.WEATHERING_COPPERS.forEach(weatheringCopper -> {
            if (!(weatheringCopper instanceof Block block)) return;
            weatheringCopper.getNext(block.defaultBlockState()).ifPresent(
                state -> bulging(provider, block, state.getBlock())
            );
        });

        BulgingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .consume(1000)
            .transform(ModBlocks.CEMENT_CAULDRONS.get(Color.GRAY).get())
            .produce(1000)
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

        BulgingRecipe.builder()
            .cauldron(ModFluids.EXP_FLUID.getId())
            .consume(1000)
            .result(ModItems.EXP_GEM)
            .save(provider);

        BulgingRecipe.builder()
            .cauldron(NeoForgeMod.MILK.getId())
            .consume(1000)
            .result(ModFoodItems.CREAM, 4)
            .save(provider, AnvilCraft.of("bulging/cream_from_milk"));

        BulgingRecipe.builder()
            .cauldron(ModFluids.HONEY.getId())
            .consume(1000)
            .result(Items.HONEY_BLOCK)
            .save(provider, AnvilCraft.of("bulging/honey_block"));

        BulgingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .transform(ModBlocks.EXP_FLUID_CAULDRON.get())
            .requires(ModItems.EXP_GEM)
            .save(provider, AnvilCraft.of("bulging/exp_fluid_cauldron"));
    }

    private static void bulging(RegistrumRecipeProvider provider, ItemLike input, ItemLike result, int consume) {
        BulgingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(input)
            .result(result)
            .consume(consume)
            .save(provider);
    }

    private static void bulging(RegistrumRecipeProvider provider, ItemLike input, ItemLike result) {
        BulgingRecipeLoader.bulging(provider, input, result, 0);
    }

    @SuppressWarnings("SameParameterValue")
    private static void bulging(RegistrumRecipeProvider provider, TagKey<Item> input, ItemLike result, int consume) {
        BulgingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(input)
            .result(result)
            .consume(consume)
            .save(provider);
    }

    @SuppressWarnings("SameParameterValue")
    private static void bulging(RegistrumRecipeProvider provider, TagKey<Item> input, ItemLike result) {
        BulgingRecipeLoader.bulging(provider, input, result, 0);
    }

    @SuppressWarnings("SameParameterValue")
    private static void crystallize(
        RegistrumRecipeProvider provider, ItemLike input, ItemLike result, int consume
    ) {
        BulgingRecipe.builder()
            .cauldron(Blocks.POWDER_SNOW_CAULDRON)
            .requires(input)
            .result(result)
            .consume(consume)
            .save(provider);
    }
}
