package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.BulgingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class BulgingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        BulgingRecipe.builder()
                .requires(Items.DIRT)
                .cauldron(Blocks.WATER_CAULDRON)
                .consumeFluid(true)
                .result(new ItemStack(Items.CLAY))
                .save(provider);

        BulgingRecipe.builder()
                .requires(ModItems.SEA_HEART_SHELL_SHARD)
                .cauldron(Blocks.POWDER_SNOW_CAULDRON)
                .consumeFluid(true)
                .result(new ItemStack(ModItems.PRISMARINE_CLUSTER.asItem()))
                .save(provider);
    }
}
