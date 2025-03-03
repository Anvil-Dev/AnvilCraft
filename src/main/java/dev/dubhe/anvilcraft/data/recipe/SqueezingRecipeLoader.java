package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.SqueezingRecipe;
import net.minecraft.world.level.block.Blocks;

public class SqueezingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        SqueezingRecipe.builder()
            .inputBlock(Blocks.WET_SPONGE)
            .resultBlock(Blocks.SPONGE)
            .cauldron(Blocks.WATER_CAULDRON)
            .save(provider, AnvilCraft.of("squeezing/water_from_wet_sponge"));

        SqueezingRecipe.builder()
            .inputBlock(Blocks.MOSS_BLOCK)
            .resultBlock(Blocks.MOSS_CARPET)
            .cauldron(Blocks.WATER_CAULDRON)
            .save(provider, AnvilCraft.of("squeezing/water_from_moss_block"));

        SqueezingRecipe.builder()
            .inputBlock(Blocks.MAGMA_BLOCK)
            .resultBlock(Blocks.NETHERRACK)
            .cauldron(ModBlocks.LAVA_CAULDRON.get())
            .save(provider, AnvilCraft.of("squeezing/lava_from_magma_block"));

        SqueezingRecipe.builder()
            .inputBlock(Blocks.SNOW_BLOCK)
            .resultBlock(Blocks.ICE)
            .cauldron(Blocks.POWDER_SNOW_CAULDRON)
            .save(provider, AnvilCraft.of("squeezing/power_snow_from_ice"));
    }
}
