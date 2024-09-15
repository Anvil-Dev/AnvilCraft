package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.BulgingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

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
        crystallize(provider, ModItems.SEA_HEART_SHELL_SHARD, ModItems.PRISMARINE_CLUSTER, true);
    }

    private static void bulging(
            RegistrateRecipeProvider provider, ItemLike input, ItemLike result, boolean consumeFluid) {
        BulgingRecipe.builder()
                .cauldron(Blocks.WATER_CAULDRON)
                .requires(input)
                .result(new ItemStack(result))
                .consumeFluid(consumeFluid)
                .save(provider);
    }

    private static void bulging(RegistrateRecipeProvider provider, ItemLike input, ItemLike result) {
        bulging(provider, input, result, false);
    }

    private static void crystallize(
            RegistrateRecipeProvider provider, ItemLike input, ItemLike result, boolean consumeFluid) {
        BulgingRecipe.builder()
                .cauldron(Blocks.POTATOES)
                .requires(input)
                .result(new ItemStack(result))
                .consumeFluid(consumeFluid)
                .save(provider);
    }

    private static void crystallize(RegistrateRecipeProvider provider, ItemLike input, ItemLike result) {
        crystallize(provider, input, result, false);
    }
}
