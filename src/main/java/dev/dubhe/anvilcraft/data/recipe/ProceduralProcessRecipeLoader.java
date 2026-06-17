package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.block.state.IrradiatorType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ReversedSmearAlikeRecipe;
import net.minecraft.world.level.block.Blocks;

public class ProceduralProcessRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        ProceduralProcessRecipeBuilder.of(Blocks.IRON_BLOCK)
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(Blocks.IRON_BLOCK)
                    .requires(ModItems.CIRCUIT_BOARD)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK)
                    .requires(ModItems.PROCESSOR)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK)
                    .requires(ModItems.DISK)
                    .resultBlock(ModBlocks.REDSTONE_COMPUTER)
                    .buildRecipe()
            )
            .result(ModBlocks.REDSTONE_COMPUTER)
            .icon(ModBlocks.REDSTONE_COMPUTER.asStack())
            .save(provider, "redstone_computer_from_procedural");

        ProceduralProcessRecipeBuilder.of(ModBlocks.REDSTONE_COMPUTER.get())
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.REDSTONE_COMPUTER.get())
                    .requires(ModItems.TRANSCENDIUM_NUGGET)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                ReversedSmearAlikeRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.TIME)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                ReversedSmearAlikeRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.SPACE)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(ModBlocks.SPACETIME_SUPERCOMPUTER)
            .icon(ModBlocks.SPACETIME_SUPERCOMPUTER.asStack())
            .loop(3)
            .multipleLoopFirstStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK)
                    .requires(ModItems.TRANSCENDIUM_NUGGET)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .save(provider, "spacetime_supercomputer_from_redstone_computer");

        ProceduralProcessRecipeBuilder.of(ModBlocks.REDSTONE_COMPUTER.get())
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.REDSTONE_COMPUTER.get())
                    .requires(ModItems.TRANSCENDIUM_NUGGET)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                ReversedSmearAlikeRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.SPACE)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                ReversedSmearAlikeRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.TIME)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(ModBlocks.SPACETIME_SUPERCOMPUTER)
            .icon(ModBlocks.SPACETIME_SUPERCOMPUTER.asStack())
            .loop(3)
            .multipleLoopFirstStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK)
                    .requires(ModItems.TRANSCENDIUM_NUGGET)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .save(provider, "spacetime_supercomputer_from_redstone_computer_2");
    }
}
