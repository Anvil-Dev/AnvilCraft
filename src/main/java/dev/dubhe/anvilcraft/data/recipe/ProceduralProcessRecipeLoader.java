package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.IrradiatorType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockProcessingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ProceduralProcessRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        // 时空超算
        ProceduralProcessRecipeBuilder.of(ModBlocks.ADVANCED_COMPARATOR.get())
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.ADVANCED_COMPARATOR.get(), IrradiatorType.TIME)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.SPACE)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK.get())
                    .requires(ModItems.TRANSCENDIUM_NUGGET)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .result(ModBlocks.SPACETIME_SUPERCOMPUTER)
            .icon(ModBlocks.SPACETIME_SUPERCOMPUTER.asStack())
            .displayedModel(AnvilCraft.of("block/spacetime_supercomputer_wip"))
            .loop(3)
            .multipleLoopFirstStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.TIME)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .save(provider, "spacetime_supercomputer_from_advanced_comparator");
        ProceduralProcessRecipeBuilder.of(ModBlocks.ADVANCED_COMPARATOR.get())
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.ADVANCED_COMPARATOR.get(), IrradiatorType.SPACE)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.TIME)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK.get())
                    .requires(ModItems.TRANSCENDIUM_NUGGET)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .result(ModBlocks.SPACETIME_SUPERCOMPUTER)
            .icon(ModBlocks.SPACETIME_SUPERCOMPUTER.asStack())
            .displayedModel(AnvilCraft.of("block/spacetime_supercomputer_wip"))
            .loop(3)
            .multipleLoopFirstStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.SPACE)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .save(provider, "spacetime_supercomputer_from_advanced_comparator_2");

        // 远古残骸
        ProceduralProcessRecipeBuilder.of(ModBlocks.TUNGSTEN_BLOCK.get())
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.TUNGSTEN_BLOCK)
                    .requires(Items.NETHERITE_SCRAP)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeTimeWarp(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(Blocks.ANCIENT_DEBRIS)
            .icon(Blocks.ANCIENT_DEBRIS.asItem().getDefaultInstance())
            .displayedModel(AnvilCraft.of("block/ancient_debris_wip"))
            .save(provider);

        // 下界合金块
        ProceduralProcessRecipeBuilder.of(Blocks.ANCIENT_DEBRIS)
            .addStep(
                BlockCompressRecipe.builder()
                    .input(Blocks.RAW_GOLD_BLOCK)
                    .input(Blocks.ANCIENT_DEBRIS)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder()
                    .input(Blocks.ANCIENT_DEBRIS)
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeSuperHeating(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(Blocks.NETHERITE_BLOCK)
            .icon(Blocks.NETHERITE_BLOCK.asItem().getDefaultInstance())
            .displayedModel(AnvilCraft.of("block/netherite_block_wip"))
            .loop(2)
            .multipleLoopFirstStep(
                BlockCompressRecipe.builder()
                    .input(Blocks.RAW_GOLD_BLOCK)
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .save(provider);

        // 铁块增值
        ProceduralProcessRecipeBuilder.of(Blocks.IRON_BLOCK)
            .addStep(
                BlockCompressRecipe.builder()
                    .input(Blocks.IRON_BLOCK)
                    .input(Blocks.IRON_BLOCK)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.MASS)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(ModBlocks.HEAVY_IRON_BLOCK)
            .icon(ModBlocks.HEAVY_IRON_BLOCK.asStack())
            .displayedModel(AnvilCraft.of("block/heavy_iron_block_wip"))
            .loop(2)
            .multipleLoopFirstStep(
                BlockCompressRecipe.builder()
                    .input(Blocks.IRON_BLOCK)
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .save(provider);

        // 远古海礁
        ProceduralProcessRecipeBuilder.of(Blocks.WET_SPONGE)
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(Blocks.WET_SPONGE)
                    .requires(Items.HEART_OF_THE_SEA)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK)
                    .requires(ModItems.SAPPHIRE)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeTimeWarp(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(ModBlocks.ANCIENT_SEA_REEF)
            .icon(ModBlocks.ANCIENT_SEA_REEF.asItem().getDefaultInstance())
            .displayedModel(AnvilCraft.of("block/ancient_sea_reef_wip"))
            .save(provider);
    }
}
