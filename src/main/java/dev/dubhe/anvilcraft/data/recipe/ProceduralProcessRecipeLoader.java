package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ReversedSmearAlikeRecipe;
import net.minecraft.world.item.Items;
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
        ProceduralProcessRecipeBuilder.of(Blocks.COPPER_BLOCK)
            .addStep(
                ItemInjectRecipe.builder().inputBlock(Blocks.IRON_BLOCK).requires(Items.IRON_BLOCK).resultBlock(ModBlocks.WIP_BLOCK).buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder().inputBlock(ModBlocks.WIP_BLOCK).requires(Items.IRON_BLOCK).resultBlock(ModBlocks.WIP_BLOCK).buildRecipe()
            )
            .result(ModBlocks.HEAVY_IRON_BLOCK)
            .icon(ModBlocks.HEAVY_IRON_BLOCK.asStack())
            .loop(4)
            .multipleLoopFirstStep(
                ItemInjectRecipe.builder().inputBlock(ModBlocks.WIP_BLOCK).requires(Items.IRON_BLOCK).resultBlock(ModBlocks.WIP_BLOCK).buildRecipe()
            )
            .save(provider, "iron_blocks_inject_procedural_loop_example");
        ProceduralProcessRecipeBuilder.of(Blocks.IRON_BLOCK)
            .addStep(
                ItemInjectRecipe.builder().inputBlock(Blocks.IRON_BLOCK).requires(ModItems.CIRCUIT_BOARD).resultBlock(ModBlocks.WIP_BLOCK).buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder().inputBlock(ModBlocks.WIP_BLOCK).requires(ModItems.PROCESSOR).resultBlock(ModBlocks.WIP_BLOCK).buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder().inputBlock(ModBlocks.WIP_BLOCK).requires(ModItems.DISK).resultBlock(ModBlocks.REDSTONE_COMPUTER).buildRecipe()
            )
            .result(ModBlocks.REDSTONE_COMPUTER)
            .icon(ModBlocks.REDSTONE_COMPUTER.asStack())
            .save(provider, "redstone_computer_from_procedural");
        //砧子辐照还没人写，我这里先拿中子辐照和砧子反向涂抹代替了
        //当然，时空超算应该也还没写完吧，这里先拿物品收集器替代了
        ProceduralProcessRecipeBuilder.of(ModBlocks.REDSTONE_COMPUTER.get())
            .addStep(
                ItemInjectRecipe.builder().inputBlock(ModBlocks.REDSTONE_COMPUTER.get()).requires(ModItems.TRANSCENDIUM_NUGGET).resultBlock(ModBlocks.WIP_BLOCK).buildRecipe()
            )
            .addStep(
                ReversedSmearAlikeRecipe.builder().fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get()).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .addStep(
                ReversedSmearAlikeRecipe.builder().input(ModBlocks.WIP_BLOCK.get()).input(ModBlocks.CONFINED_TIME_ANVILON.get()).result(ModBlocks.WIP_BLOCK.get()).buildRecipe()
            )
            .result(ModBlocks.ITEM_COLLECTOR)
            .icon(ModBlocks.ITEM_COLLECTOR.asStack())
            .loop(3)
            .multipleLoopFirstStep(
                ItemInjectRecipe.builder().inputBlock(ModBlocks.WIP_BLOCK).requires(ModItems.TRANSCENDIUM_NUGGET).resultBlock(ModBlocks.WIP_BLOCK).buildRecipe()
            )
            .save(provider, "spacetime_supercomputer_from_redstone_computer");
    }

}
