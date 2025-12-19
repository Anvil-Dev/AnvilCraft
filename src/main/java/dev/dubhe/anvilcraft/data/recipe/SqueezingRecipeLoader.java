package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static dev.dubhe.anvilcraft.data.recipe.util.RecipeLoaderUtil.getName;

public class SqueezingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        squeezing(provider, Blocks.WET_SPONGE, Blocks.SPONGE, Blocks.WATER_CAULDRON, 333);
        squeezing(provider, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.WATER_CAULDRON, 333);
        squeezing(provider, Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, ModBlocks.LAVA_CAULDRON.get(), 250);
        squeezing(provider, Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.POWDER_SNOW_CAULDRON, 333);
    }

    public static void squeezing(RegistrateRecipeProvider provider, Block requires, Block result, Block cauldron, int produce) {
        SqueezingRecipe.builder()
            .requires(requires)
            .result(result)
            .transform(cauldron)
            .produce(produce)
            .save(
                provider, AnvilCraft.of("squeezing/%s_from_%s".formatted(getName(cauldron), getName(requires))));
    }
}
