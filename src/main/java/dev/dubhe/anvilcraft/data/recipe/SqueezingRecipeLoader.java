package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static dev.dubhe.anvilcraft.data.recipe.util.RecipeLoaderUtil.getName;

public class SqueezingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        squeezing(provider, Blocks.WET_SPONGE, Blocks.SPONGE, Blocks.WATER_CAULDRON, 250);
        squeezing(provider, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.WATER_CAULDRON, 250);
        squeezing(provider, Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, Blocks.LAVA_CAULDRON, 250);
        squeezing(provider, Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.POWDER_SNOW_CAULDRON, 250);

        SqueezingRecipe.builder()
            .requires(Blocks.SCULK)
            .result(Blocks.AIR)
            .transform(ModBlocks.EXP_FLUID_CAULDRON.get())
            .produce(250)
            .chance(0.1f)
            .noFrostAnvil()
            .save(provider, AnvilCraft.of("squeezing/exp_fluid_from_sculk"));

        SqueezingRecipe.builder()
            .requires(Blocks.SCULK)
            .result(Blocks.AIR)
            .transform(ModBlocks.EXP_FLUID_CAULDRON.get())
            .produce(250)
            .chance(0.4f)
            .frostAnvil()
            .save(provider, AnvilCraft.of("squeezing/exp_fluid_from_sculk_use_frost_anvil"));
    }

    public static void squeezing(RegistrumRecipeProvider provider, Block requires, Block result, Block cauldron, int produce) {
        SqueezingRecipe.builder()
            .requires(requires)
            .result(result)
            .transform(cauldron)
            .produce(produce)
            .save(provider, AnvilCraft.of("squeezing/%s_from_%s".formatted(getName(cauldron), getName(requires))));
    }
}
