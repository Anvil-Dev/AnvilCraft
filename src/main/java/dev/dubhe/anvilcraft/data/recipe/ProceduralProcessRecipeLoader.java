package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import net.minecraft.world.level.block.Blocks;

public class ProceduralProcessRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        ProceduralProcessRecipeBuilder.of(Blocks.IRON_BLOCK)
            .addStep(
                BlockCompressRecipe.builder().input(Blocks.IRON_BLOCK).input(Blocks.IRON_BLOCK).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder().input(Blocks.IRON_BLOCK).input(ModBlocks.WIP_BLOCK.get()).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder().input(Blocks.IRON_BLOCK).input(ModBlocks.WIP_BLOCK.get()).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder().input(Blocks.IRON_BLOCK).input(ModBlocks.WIP_BLOCK.get()).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder().input(Blocks.IRON_BLOCK).input(ModBlocks.WIP_BLOCK.get()).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder().input(Blocks.IRON_BLOCK).input(ModBlocks.WIP_BLOCK.get()).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder().input(Blocks.IRON_BLOCK).input(ModBlocks.WIP_BLOCK.get()).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder().input(Blocks.IRON_BLOCK).input(ModBlocks.WIP_BLOCK.get()).result(ModBlocks.HEAVY_IRON_BLOCK.get()).buildRecipe()
            )
            .result(ModBlocks.HEAVY_IRON_BLOCK)
            .icon(ModBlocks.HEAVY_IRON_BLOCK.asStack())
            .save(provider, "nine_iron_blocks_procedural_example");
    }

}
