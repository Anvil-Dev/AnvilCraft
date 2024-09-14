package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.TimeWarpRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class TimeWarpRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        TimeWarpRecipe.builder()
                .requires(ModItems.RESIN)
                .result(new ItemStack(ModItems.AMBER.asItem()))
                .save(provider);

        TimeWarpRecipe.builder()
                .requires(Items.EMERALD)
                .result(new ItemStack(Items.EMERALD_BLOCK))
                .consumeFluid(true)
                .cauldron(ModBlocks.MELT_GEM_CAULDRON.get())
                .save(provider);

        TimeWarpRecipe.builder()
                .requires(Items.ROTTEN_FLESH, 64)
                .produceFluid(true)
                .cauldron(ModBlocks.OIL_CAULDRON.get())
                .save(provider, AnvilCraft.of("time_warp/oil_from_rotten_flesh"));
    }
}
