package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class SqueezingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        squeezing(provider, Blocks.WET_SPONGE, Blocks.SPONGE, Blocks.WATER_CAULDRON, "water_from_wet_sponge");
        squeezing(provider, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.WATER_CAULDRON, "water_from_moss_block");
        squeezing(provider, Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, ModBlocks.LAVA_CAULDRON.get(), "lava_from_magma_block");
        squeezing(provider, Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.POWDER_SNOW_CAULDRON, "power_snow_from_ice");
    }

    public static void squeezing(RegistrateRecipeProvider provider, Block requires, Block result, Block cauldron, String save) {
        SqueezingRecipe.builder()
            .requires(requires)
            .result(result)
            .transform(cauldron)
            .produceFluid(true)
            .save(provider, AnvilCraft.of("squeezing/%s".formatted(save)));
    }
}
