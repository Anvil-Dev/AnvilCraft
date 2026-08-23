package dev.dubhe.anvilcraft.init.item.tabs;

import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSection;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public class BuildingBlocksSections extends DisplayItemsGenerator {
    @Override
    public void accept() {
        if (!(this.output instanceof CreativeTabSections sections)) {
            return;
        }
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/building_blocks/materials.png")
                )
                .text(Component.translatable("anvilcraft.creative.section.building_blocks.materials"))
                .build(),
            content -> {
                content.accept(ModBlocks.HEAVY_IRON_BLOCK);
                content.accept(ModBlocks.POLISHED_HEAVY_IRON_BLOCK);
                content.accept(ModBlocks.POLISHED_HEAVY_IRON_STAIRS);
                content.accept(ModBlocks.POLISHED_HEAVY_IRON_SLAB);
                content.accept(ModBlocks.CUT_HEAVY_IRON_BLOCK);
                content.accept(ModBlocks.CUT_HEAVY_IRON_STAIRS);
                content.accept(ModBlocks.CUT_HEAVY_IRON_SLAB);
                content.accept(ModBlocks.HEAVY_IRON_PLATE);
                content.accept(ModBlocks.HEAVY_IRON_COLUMN);
                content.accept(ModBlocks.HEAVY_IRON_BEAM);
                content.accept(ModBlocks.HEAVY_IRON_WALL);
                content.accept(ModBlocks.HEAVY_IRON_DOOR);
                content.accept(ModBlocks.HEAVY_IRON_TRAPDOOR);
                content.accept(ModBlocks.ROYAL_STEEL_BLOCK);
                content.accept(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK);
                content.accept(ModBlocks.CUT_ROYAL_STEEL_BLOCK);
                content.accept(ModBlocks.CUT_ROYAL_STEEL_STAIRS);
                content.accept(ModBlocks.CUT_ROYAL_STEEL_SLAB);
                content.accept(ModBlocks.CUT_ROYAL_STEEL_PILLAR);
                content.accept(ModBlocks.TEMPERING_GLASS);
                content.accept(ModBlocks.FROST_METAL_BLOCK);
                content.accept(ModBlocks.CUT_FROST_METAL_BLOCK);
                content.accept(ModBlocks.CUT_FROST_METAL_STAIRS);
                content.accept(ModBlocks.CUT_FROST_METAL_SLAB);
                content.accept(ModBlocks.CUT_FROST_METAL_PILLAR);
                content.accept(ModBlocks.FROST_GLASS);
                content.accept(ModBlocks.EMBER_METAL_BLOCK);
                content.accept(ModBlocks.CUT_EMBER_METAL_BLOCK);
                content.accept(ModBlocks.CUT_EMBER_METAL_STAIRS);
                content.accept(ModBlocks.CUT_EMBER_METAL_SLAB);
                content.accept(ModBlocks.CUT_EMBER_METAL_PILLAR);
                content.accept(ModBlocks.EMBER_GLASS);
                content.accept(ModBlocks.MULTIPHASE_MATTER_BLOCK);
                content.accept(ModBlocks.TRANSCENDIUM_BLOCK);
                content.accept(ModBlocks.NEGATIVE_MATTER_BLOCK);
                content.accept(ModBlocks.SINGULARITY_CRYSTAL);
                content.accept(ModBlocks.HYPERCUBE);
                content.accept(ModBlocks.BRASS_BLOCK);
                content.accept(ModBlocks.CUT_BRASS_BLOCK);
                content.accept(ModBlocks.CUT_BRASS_STAIRS);
                content.accept(ModBlocks.CUT_BRASS_SLAB);
                content.accept(ModBlocks.CUT_BRASS_PILLAR);
                content.accept(ModBlocks.CHISELED_BRASS_BLOCK);
                content.accept(ModBlocks.BRONZE_BLOCK);
                content.accept(ModBlocks.CUT_BRONZE_BLOCK);
                content.accept(ModBlocks.CUT_BRONZE_STAIRS);
                content.accept(ModBlocks.CUT_BRONZE_SLAB);
                content.accept(ModBlocks.CUT_BRONZE_PILLAR);
                content.accept(ModBlocks.CHISELED_BRONZE_BLOCK);
                content.accept(ModBlocks.ZINC_BLOCK);
                content.accept(ModBlocks.TIN_BLOCK);
                content.accept(ModBlocks.TITANIUM_BLOCK);
                content.accept(ModBlocks.LEAD_BLOCK);
                content.accept(ModBlocks.SILVER_BLOCK);
                content.accept(ModBlocks.URANIUM_BLOCK);
                content.accept(ModBlocks.PLUTONIUM_BLOCK);
                content.accept(ModBlocks.TUNGSTEN_BLOCK);
                content.accept(ModBlocks.HEATED_TUNGSTEN_BLOCK);
                content.accept(ModBlocks.REDHOT_TUNGSTEN_BLOCK);
                content.accept(ModBlocks.GLOWING_TUNGSTEN_BLOCK);
                content.accept(ModBlocks.INCANDESCENT_TUNGSTEN_BLOCK);
                content.accept(Items.NETHERITE_BLOCK);
                content.accept(ModBlocks.HEATED_NETHERITE_BLOCK);
                content.accept(ModBlocks.REDHOT_NETHERITE_BLOCK);
                content.accept(ModBlocks.GLOWING_NETHERITE_BLOCK);
                content.accept(ModBlocks.INCANDESCENT_NETHERITE_BLOCK);
                content.accept(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK);
                content.accept(ModBlocks.CURSED_GOLD_BLOCK);
                content.accept(ModBlocks.ENCHANTED_GOLD_BLOCK);
                content.accept(ModBlocks.TOPAZ_BLOCK);
                content.accept(ModBlocks.RUBY_BLOCK);
                content.accept(ModBlocks.SAPPHIRE_BLOCK);
                content.accept(ModBlocks.CHROMATIC_STONE);
                content.accept(ModBlocks.EXP_GEM_BLOCK);
                content.accept(ModBlocks.FLINT_BLOCK);
                content.accept(ModBlocks.POLISHED_FLINT_BLOCK);
                content.accept(ModBlocks.CUT_FLINT_BLOCK);
                content.accept(ModBlocks.CUT_FLINT_STAIRS_BLOCK);
                content.accept(ModBlocks.CUT_FLINT_SLAB_BLOCK);
                content.accept(ModBlocks.CUT_FLINT_PILLAR_BLOCK);
                content.accept(ModBlocks.SUGAR_BLOCK);
                content.accept(ModBlocks.GUNPOWER_BLOCK);
                content.accept(ModBlocks.ROTTEN_FLESH_BLOCK);
                content.accept(ModBlocks.PLYWOOD_BLOCK);
                content.accept(ModBlocks.PLYWOOD_STAIRS);
                content.accept(ModBlocks.PLYWOOD_SLAB);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/building_blocks/resources.png")
                )
                .text(Component.translatable("anvilcraft.creative.section.building_blocks.resources"))
                .build(),
            content -> {
                content.accept(ModBlocks.CINERITE);
                content.accept(ModBlocks.QUARTZ_SAND);
                content.accept(ModBlocks.LEVITATION_POWDER_BLOCK);
                content.accept(ModBlocks.NETHER_DUST);
                content.accept(ModBlocks.END_DUST);
                content.accept(ModBlocks.STURDY_DEEPSLATE);
                content.accept(ModBlocks.DEEPSLATE_ZINC_ORE);
                content.accept(ModBlocks.DEEPSLATE_TIN_ORE);
                content.accept(ModBlocks.DEEPSLATE_TITANIUM_ORE);
                content.accept(ModBlocks.DEEPSLATE_LEAD_ORE);
                content.accept(ModBlocks.DEEPSLATE_SILVER_ORE);
                content.accept(ModBlocks.DEEPSLATE_URANIUM_ORE);
                content.accept(ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
                content.accept(ModBlocks.VOID_STONE);
                content.accept(ModBlocks.EARTH_CORE_SHARD_ORE);
                content.accept(ModBlocks.RAW_ZINC_BLOCK);
                content.accept(ModBlocks.RAW_TIN_BLOCK);
                content.accept(ModBlocks.RAW_TITANIUM_BLOCK);
                content.accept(ModBlocks.RAW_LEAD_BLOCK);
                content.accept(ModBlocks.RAW_SILVER_BLOCK);
                content.accept(ModBlocks.RAW_URANIUM_BLOCK);
                content.accept(ModBlocks.RAW_TUNGSTEN_BLOCK);
                content.accept(ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK);
                content.accept(ModBlocks.VOID_MATTER_BLOCK);
                content.accept(ModBlocks.EARTH_CORE_SHARD_BLOCK);
                content.accept(ModBlocks.ANCIENT_SEA_REEF);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/building_blocks/concrete.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.building_blocks.concrete"))
            .build(),
            content -> {
                for (Color color : Color.values()) {
                    this.acceptFolded(content, ModBlocks.REINFORCED_CONCRETES.get(color));
                }
                for (Color color : Color.values()) {
                    this.acceptFolded(content, ModBlocks.REINFORCED_CONCRETE_STAIRS.get(color));
                }
                for (Color color : Color.values()) {
                    this.acceptFolded(content, ModBlocks.REINFORCED_CONCRETE_SLABS.get(color));
                }
                for (Color color : Color.values()) {
                    this.acceptFolded(content, ModBlocks.REINFORCED_CONCRETE_WALLS.get(color));
                }
            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/building_blocks/foods.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.building_blocks.foods"))
            .build(),
            content -> {
                content.accept(ModBlocks.CAKE_BASE_BLOCK);
                content.accept(ModBlocks.CREAM_BLOCK);
                content.accept(ModBlocks.BERRY_CREAM_BLOCK);
                content.accept(ModBlocks.CHOCOLATE_CREAM_BLOCK);
                content.accept(ModBlocks.CAKE_BLOCK);
                content.accept(ModBlocks.BERRY_CAKE_BLOCK);
                content.accept(ModBlocks.CHOCOLATE_CAKE_BLOCK);
                content.accept(ModBlocks.LARGE_CAKE);
                content.accept(ModBlocks.CHOCOLATE_BLOCK);
                content.accept(ModBlocks.CHOCOLATE_STAIRS);
                content.accept(ModBlocks.CHOCOLATE_SLAB);
                content.accept(ModBlocks.WHITE_CHOCOLATE_BLOCK);
                content.accept(ModBlocks.WHITE_CHOCOLATE_STAIRS);
                content.accept(ModBlocks.WHITE_CHOCOLATE_SLAB);
                content.accept(ModBlocks.BLACK_CHOCOLATE_BLOCK);
                content.accept(ModBlocks.BLACK_CHOCOLATE_STAIRS);
                content.accept(ModBlocks.BLACK_CHOCOLATE_SLAB);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/building_blocks/special.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.building_blocks.special"))
            .build(),
            content -> {
                content.accept(ModBlocks.FROST_DECO_BLOCK);
                content.accept(ModBlocks.FROST_DECO_OUTLINE);
                content.accept(ModBlocks.EMBER_DECO_BLOCK);
                content.accept(ModBlocks.EMBER_DECO_OUTLINE);
                content.accept(ModBlocks.TRANSCENDENCE_DECO_BLOCK);
                content.accept(ModBlocks.TRANSCENDENCE_DECO_OUTLINE);
                content.accept(ModBlocks.ARROW);
                content.accept(ModBlocks.CHECK_MARK);
                content.accept(ModBlocks.CROSS_MARK);
                content.accept(ModBlocks.EXCLAMATION_MARK);
                content.accept(ModBlocks.QUESTION_MARK);
            }
        );
    }
}
