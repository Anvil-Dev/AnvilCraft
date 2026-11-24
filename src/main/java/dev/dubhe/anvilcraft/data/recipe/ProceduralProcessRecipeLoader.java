package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.IrradiatorType;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockProcessingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class ProceduralProcessRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        var blocks = provider.getRegistries().lookupOrThrow(Registries.BLOCK);
        ProceduralProcessRecipeBuilder.of(BlockStatePredicate.builder().of(blocks, BlockTags.SHULKER_BOXES).build())
            .addStep(
                BlockCompressRecipe.builder()
                    .input(blocks, BlockTags.SHULKER_BOXES)
                    .input(blocks, BlockTags.SHULKER_BOXES)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder()
                    .input(blocks, BlockTags.SHULKER_BOXES)
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder()
                    .input(blocks, BlockTags.SHULKER_BOXES)
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder()
                    .input(blocks, ModBlockTags.STORAGE_BLOCKS_VOID_MATTER)
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(ModBlocks.SPACE_OVERCOMPRESSOR)
            .icon(new ItemStackTemplate(ModBlocks.SPACE_OVERCOMPRESSOR.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/nesting_shulker_box"),
                AnvilCraft.of("block/wip_display/over_nesting_shulker_box"),
                AnvilCraft.of("block/wip_display/supercritical_nesting_shulker_box")
            )
            .save(provider);

        // 能质逆变器
        ProceduralProcessRecipeBuilder.of(ModBlocks.LASER_RECEIVER.get())
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.LASER_RECEIVER.get(), IrradiatorType.MASS)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.ENERGY)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK.get())
                    .requires(provider.getItems(), ModItemTags.TRANSCENDIUM_NUGGETS)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .result(ModBlocks.MASS_ENERGY_INVERTER)
            .icon(new ItemStackTemplate(ModBlocks.MASS_ENERGY_INVERTER.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/mass_energy_inverter_wip"),
                AnvilCraft.of("block/wip_display/mass_energy_inverter_wip_2"),
                AnvilCraft.of("block/wip_display/mass_energy_inverter_wip_3")
            )
            .loop(3)
            .multipleLoopFirstStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.MASS)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .save(provider, "mass_energy_inverter_mass_first");
        ProceduralProcessRecipeBuilder.of(ModBlocks.LASER_RECEIVER.get())
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.LASER_RECEIVER.get(), IrradiatorType.ENERGY)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.MASS)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK.get())
                    .requires(provider.getItems(), ModItemTags.TRANSCENDIUM_NUGGETS)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .result(ModBlocks.MASS_ENERGY_INVERTER)
            .icon(new ItemStackTemplate(ModBlocks.MASS_ENERGY_INVERTER.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/mass_energy_inverter_wip"),
                AnvilCraft.of("block/wip_display/mass_energy_inverter_wip_2"),
                AnvilCraft.of("block/wip_display/mass_energy_inverter_wip_3")
            )
            .loop(3)
            .multipleLoopFirstStep(
                BlockProcessingRecipe.builder()
                    .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.ENERGY)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .save(provider, "mass_energy_inverter_energy_first");

        ProceduralProcessRecipeBuilder.of(Blocks.PURPUR_BLOCK)
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(Blocks.PURPUR_BLOCK)
                    .requires(provider.getItems(), Tags.Items.ENDER_PEARLS)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK)
                    .requires(provider.getItems(), Tags.Items.CHESTS_WOODEN)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder()
                    .input(Blocks.PURPUR_BLOCK)
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(Blocks.SHULKER_BOX)
            .icon(new ItemStackTemplate(Items.SHULKER_BOX))
            .displayedModels(
                AnvilCraft.of("block/wip_display/shulker_box_wip"),
                AnvilCraft.of("block/wip_display/shulker_box_wip_2")
            )
            .save(provider);

        // 红石计算机
        // 时空超算 (时间先)
        ProceduralProcessRecipeBuilder.of(ModBlocks.ADVANCED_COMPARATOR.get())
            .addStep(BlockProcessingRecipe.builder()
                .fakeNeutronIrradiation(ModBlocks.ADVANCED_COMPARATOR.get(), IrradiatorType.TIME)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .addStep(BlockProcessingRecipe.builder()
                .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.SPACE)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .addStep(ItemInjectRecipe.builder()
                .inputBlock(ModBlocks.WIP_BLOCK.get()).requires(provider.getItems(), ModItemTags.TRANSCENDIUM_NUGGETS)
                .resultBlock(ModBlocks.WIP_BLOCK).buildRecipe())
            .result(ModBlocks.SPACETIME_SUPERCOMPUTER)
            .icon(new ItemStackTemplate(ModBlocks.SPACETIME_SUPERCOMPUTER.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/spacetime_supercomputer_wip"),
                AnvilCraft.of("block/wip_display/spacetime_supercomputer_wip_2"),
                AnvilCraft.of("block/wip_display/spacetime_supercomputer_wip_3")
            )
            .loop(3)
            .multipleLoopFirstStep(BlockProcessingRecipe.builder()
                .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.TIME)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .save(provider, "spacetime_supercomputer_from_advanced_comparator");

        // 时空超算 (空间先)
        ProceduralProcessRecipeBuilder.of(ModBlocks.ADVANCED_COMPARATOR.get())
            .addStep(BlockProcessingRecipe.builder()
                .fakeNeutronIrradiation(ModBlocks.ADVANCED_COMPARATOR.get(), IrradiatorType.SPACE)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .addStep(BlockProcessingRecipe.builder()
                .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.TIME)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .addStep(ItemInjectRecipe.builder()
                .inputBlock(ModBlocks.WIP_BLOCK.get()).requires(provider.getItems(), ModItemTags.TRANSCENDIUM_NUGGETS)
                .resultBlock(ModBlocks.WIP_BLOCK).buildRecipe())
            .result(ModBlocks.SPACETIME_SUPERCOMPUTER)
            .icon(new ItemStackTemplate(ModBlocks.SPACETIME_SUPERCOMPUTER.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/spacetime_supercomputer_wip"),
                AnvilCraft.of("block/wip_display/spacetime_supercomputer_wip_2"),
                AnvilCraft.of("block/wip_display/spacetime_supercomputer_wip_3")
            )
            .loop(3)
            .multipleLoopFirstStep(BlockProcessingRecipe.builder()
                .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.SPACE)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .save(provider, "spacetime_supercomputer_from_advanced_comparator_2");

        // 远古残骸
        ProceduralProcessRecipeBuilder.of(BlockStatePredicate.builder().of(blocks, ModBlockTags.STORAGE_BLOCKS_TUNGSTEN).build())
            .addStep(ItemInjectRecipe.builder()
                .inputBlock(blocks, ModBlockTags.STORAGE_BLOCKS_TUNGSTEN).requires(Items.NETHERITE_SCRAP)
                .resultBlock(ModBlocks.WIP_BLOCK).buildRecipe())
            .addStep(BlockProcessingRecipe.builder()
                .fakeTimeWarp(ModBlocks.WIP_BLOCK.get())
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .result(Blocks.ANCIENT_DEBRIS)
            .icon(new ItemStackTemplate(Blocks.ANCIENT_DEBRIS.asItem()))
            .displayedModels(AnvilCraft.of("block/wip_display/ancient_debris_wip"))
            .save(provider);

        // 下界合金块
        ProceduralProcessRecipeBuilder.of(BlockStatePredicate.builder().of(blocks, Tags.Blocks.ORES_NETHERITE_SCRAP).build())
            .addStep(BlockCompressRecipe.builder()
                .input(blocks, Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD).input(blocks, Tags.Blocks.ORES_NETHERITE_SCRAP)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .addStep(BlockCompressRecipe.builder()
                .input(blocks, Tags.Blocks.ORES_NETHERITE_SCRAP).input(ModBlocks.WIP_BLOCK.get())
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .addStep(BlockProcessingRecipe.builder()
                .fakeSuperHeating(ModBlocks.WIP_BLOCK.get())
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .result(Blocks.NETHERITE_BLOCK)
            .icon(new ItemStackTemplate(Blocks.NETHERITE_BLOCK.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/netherite_block_wip"),
                AnvilCraft.of("block/wip_display/netherite_block_wip_2")
            )
            .loop(2)
            .multipleLoopFirstStep(BlockCompressRecipe.builder()
                .input(blocks, Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD).input(ModBlocks.WIP_BLOCK.get())
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .save(provider);

        // 铁块增值
        ProceduralProcessRecipeBuilder.of(BlockStatePredicate.builder().of(blocks, Tags.Blocks.STORAGE_BLOCKS_IRON).build())
            .addStep(BlockCompressRecipe.builder()
                .input(blocks, Tags.Blocks.STORAGE_BLOCKS_IRON).input(blocks, Tags.Blocks.STORAGE_BLOCKS_IRON)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .addStep(BlockProcessingRecipe.builder()
                .fakeNeutronIrradiation(ModBlocks.WIP_BLOCK.get(), IrradiatorType.MASS)
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .result(ModBlocks.HEAVY_IRON_BLOCK)
            .icon(new ItemStackTemplate(ModBlocks.HEAVY_IRON_BLOCK.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/heavy_iron_block_wip"),
                AnvilCraft.of("block/wip_display/heavy_iron_block_wip_2")
            )
            .loop(2)
            .multipleLoopFirstStep(BlockCompressRecipe.builder()
                .input(blocks, Tags.Blocks.STORAGE_BLOCKS_IRON).input(ModBlocks.WIP_BLOCK.get())
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .save(provider);

        // 杩滃彜娴风
        ProceduralProcessRecipeBuilder.of(Blocks.WET_SPONGE)
            .addStep(ItemInjectRecipe.builder()
                .inputBlock(Blocks.WET_SPONGE).requires(Items.HEART_OF_THE_SEA)
                .resultBlock(ModBlocks.WIP_BLOCK).buildRecipe())
            .addStep(ItemInjectRecipe.builder()
                .inputBlock(ModBlocks.WIP_BLOCK).requires(provider.getItems(), ModItemTags.GEMS_SAPPHIRE)
                .resultBlock(ModBlocks.WIP_BLOCK).buildRecipe())
            .addStep(BlockProcessingRecipe.builder()
                .fakeTimeWarp(ModBlocks.WIP_BLOCK.get())
                .result(ModBlocks.WIP_BLOCK.get()).buildRecipe())
            .result(ModBlocks.ANCIENT_SEA_REEF)
            .icon(new ItemStackTemplate(ModBlocks.ANCIENT_SEA_REEF.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/ancient_sea_reef_wip"),
                AnvilCraft.of("block/wip_display/ancient_sea_reef_wip_2")
            )
            .save(provider);

        // 石碑方块
        ProceduralProcessRecipeBuilder.of(Blocks.BLACKSTONE)
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(Blocks.BLACKSTONE)
                    .requires(Items.BOOK)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK)
                    .requires(ModItems.DISK)
                    .resultBlock(ModBlocks.WIP_BLOCK)
                    .buildRecipe()
            )
            .addStep(
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.WIP_BLOCK)
                    .requires(ModItems.LEGACY_ESSENCE)
                    .resultBlock(ModBlocks.MONOLITH)
                    .buildRecipe()
            )
            .result(ModBlocks.MONOLITH)
            .icon(new ItemStackTemplate(ModBlocks.MONOLITH.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/monolith_wip"),
                AnvilCraft.of("block/wip_display/monolith_wip")
            )
            .save(provider, "monolith_inject");

        ProceduralProcessRecipeBuilder.of(Blocks.BLACKSTONE)
            .addStep(
                BlockCompressRecipe.builder()
                    .input(ModBlocks.SPACETIME_SUPERCOMPUTER.get())
                    .input(Blocks.BLACKSTONE)
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder()
                    .input(ModBlocks.MASS_ENERGY_INVERTER.get())
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.MONOLITH.get())
                    .buildRecipe()
            )
            .result(ModBlocks.MONOLITH)
            .icon(new ItemStackTemplate(ModBlocks.MONOLITH.asItem()))
            .displayedModels(
                AnvilCraft.of("block/wip_display/monolith_wip")
            )
            .save(provider, "monolith_compress");
    }
}
