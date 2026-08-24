package dev.dubhe.anvilcraft.init.item.tabs;

import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSection;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class ItemsSections extends DisplayItemsGenerator {
    @Override
    public void accept() {
        if (!(this.output instanceof CreativeTabSections sections)) {
            return;
        }
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/items/tools.png")
                )
                .textAlignment(CreativeTabSection.TextAlignment.LEFT)
                .textIndent(17)
                .text(Component.translatable("anvilcraft.creative.section.items.tools"))
            .build(),
            content -> {
                content.accept(ModItems.GUIDE_BOOK);
                content.accept(ModItems.GEODE);
                content.accept(ModItems.MAGNET);
                content.accept(ModItems.CRAB_CLAW);
                content.accept(ModItems.DISK);
                content.accept(ModItems.STRUCTURE_DISK);
                content.accept(ModItems.FILTER);
                content.accept(ModItems.HYPERDIMENSION_TERMINAL);
                content.accept(ModItems.ANVIL_HAMMER);
                content.accept(ModItems.DRAGON_ROD);
                this.enchanting(ModItems.AMETHYST_PICKAXE, Enchantments.FORTUNE, 3);
                this.enchanting(ModItems.AMETHYST_AXE, ModEnchantments.FELLING_KEY, 1);
                this.enchanting(ModItems.AMETHYST_SHOVEL, Enchantments.EFFICIENCY, 3);
                this.enchanting(ModItems.AMETHYST_HOE, ModEnchantments.HARVEST_KEY, 1);
                this.enchanting(ModItems.AMETHYST_SWORD, ModEnchantments.BEHEADING_KEY, 1);
                content.accept(ModItems.ROYAL_ANVIL_HAMMER);
                content.accept(ModItems.ROYAL_DRAGON_ROD);
                content.accept(ModItems.ROYAL_STEEL_PICKAXE);
                content.accept(ModItems.ROYAL_STEEL_AXE);
                content.accept(ModItems.ROYAL_STEEL_SHOVEL);
                content.accept(ModItems.ROYAL_STEEL_HOE);
                content.accept(ModItems.ROYAL_STEEL_SWORD);
                content.accept(ModItems.FROST_ANVIL_HAMMER);
                content.accept(ModItems.FROST_DRAGON_ROD);
                content.accept(ModItems.FROST_METAL_PICKAXE);
                content.accept(ModItems.FROST_METAL_AXE);
                content.accept(ModItems.FROST_METAL_SHOVEL);
                content.accept(ModItems.FROST_METAL_HOE);
                content.accept(ModItems.FROST_METAL_SWORD);
                content.accept(ModItems.FROST_METAL_RESONATOR);
                content.accept(ModItems.FROST_METAL_HEAVY_HALBERD);
                content.accept(ModItems.EMBER_ANVIL_HAMMER);
                content.accept(ModItems.EMBER_DRAGON_ROD);
                content.accept(ModItems.EMBER_METAL_PICKAXE);
                content.accept(ModItems.EMBER_METAL_AXE);
                content.accept(ModItems.EMBER_METAL_SHOVEL);
                content.accept(ModItems.EMBER_METAL_HOE);
                content.accept(ModItems.EMBER_METAL_SWORD);
                content.accept(ModItems.EMBER_METAL_RESONATOR);
                content.accept(ModItems.EMBER_METAL_HEAVY_HALBERD);
                content.accept(ModItems.TRANSCENDENCE_ANVIL_HAMMER);
                content.accept(ModItems.TRANSCENDENCE_DRAGON_ROD);
                content.accept(ModItems.TRANSCENDENCE_RESONATOR);
                content.accept(ModItems.TRANSCENDENCE_HEAVY_HALBERD);
                content.accept(ModItems.MULTITOOL_ITEM);
                content.accept(ModItems.FLUID_TANK_MINECART);
                content.accept(ModItems.STRUCTURE_TOOL);
                content.accept(ModItems.SEEDS_PACK);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/items/guns.png")
                )
                .textAlignment(CreativeTabSection.TextAlignment.LEFT)
                .textIndent(17)
                .text(Component.translatable("anvilcraft.creative.section.items.guns"))
            .build(),
            content -> {
                content.accept(ModItems.SPECTRAL_SLINGSHOT);
                content.accept(ModItems.ENERGY_WEAPON_PLATFORM);
                content.accept(ModItems.SPECTRAL_WEAPON_LAUNCHER);
                content.accept(ModItems.ANVIL_RAILGUN);
                content.accept(ModItems.CORRUPTED_BEACON_ACTIVATOR);
                content.accept(ModItems.TESLA_GUN);
                content.accept(ModItems.LASER_GUN);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/items/power.png")
                )
                .textAlignment(CreativeTabSection.TextAlignment.LEFT)
                .textIndent(17)
                .text(Component.translatable("anvilcraft.creative.section.items.power"))
            .build(),
            content -> {
                content.accept(ModItems.IONOCRAFT);
                this.ionoCraftBackpack(ModItems.IONOCRAFT_BACKPACK);
                content.accept(ModItems.CAPACITOR);
                content.accept(ModItems.CAPACITOR_EMPTY);
                content.accept(ModItems.SUPER_CAPACITOR);
                content.accept(ModItems.SUPER_CAPACITOR_EMPTY);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/items/magic.png")
                )
                .textAlignment(CreativeTabSection.TextAlignment.LEFT)
                .textIndent(17)
                .text(Component.translatable("anvilcraft.creative.section.items.magic"))
            .build(),
            content -> {
                content.accept(ModItems.RECOVERY_PEARL);
                content.accept(ModItems.TOTEM_OF_RECOVERY);
                content.accept(ModItems.TOTEM_OF_RAGE);
                content.accept(Items.TOTEM_OF_UNDYING);
                content.accept(ModItems.AMULET_BOX);
                content.accept(ModItems.EMERALD_AMULET);
                content.accept(ModItems.TOPAZ_AMULET);
                content.accept(ModItems.RUBY_AMULET);
                content.accept(ModItems.SAPPHIRE_AMULET);
                content.accept(ModItems.ANVIL_AMULET);
                content.accept(ModItems.COMRADE_AMULET);
                content.accept(ModItems.FEATHER_AMULET);
                content.accept(ModItems.CAT_AMULET);
                content.accept(ModItems.DOG_AMULET);
                content.accept(ModItems.SILENCE_AMULET);
                content.accept(ModItems.ABNORMAL_AMULET);
                content.accept(ModItems.GEM_AMULET);
                content.accept(ModItems.NATURE_AMULET);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/items/foods.png")
                )
                .textAlignment(CreativeTabSection.TextAlignment.LEFT)
                .textIndent(17)
                .text(Component.translatable("anvilcraft.creative.section.items.foods"))
            .build(),
            content -> {
                content.accept(ModFoodItems.CURSED_GOLDEN_APPLE);
                content.accept(ModFoodItems.CHOCOLATE);
                content.accept(ModFoodItems.CHOCOLATE_BLACK);
                content.accept(ModFoodItems.CHOCOLATE_WHITE);
                content.accept(ModFoodItems.CREAMY_BREAD_ROLL);
                content.accept(ModFoodItems.BEEF_MUSHROOM_STEW);
                content.accept(ModFoodItems.UTUSAN);
                content.accept(ModFoodItems.PILL);
                content.accept(ModItems.PILL_BOX);
                content.accept(ModItems.TIN_CAN);
                content.accept(ModFoodItems.CANNED_FOOD);
                content.accept(ModFoodItems.CREAM);
                content.accept(ModFoodItems.DOUGH);
                content.accept(ModFoodItems.FLOUR);
                content.accept(ModFoodItems.COCOA_POWDER);
                content.accept(ModFoodItems.COCOA_LIQUOR);
                content.accept(ModFoodItems.COCOA_BUTTER);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/items/materials.png")
                )
                .textAlignment(CreativeTabSection.TextAlignment.LEFT)
                .textIndent(17)
                .text(Component.translatable("anvilcraft.creative.section.items.materials"))
            .build(),
            content -> {
                content.accept(ModItems.RAW_ZINC);
                content.accept(ModItems.RAW_TIN);
                content.accept(ModItems.RAW_TITANIUM);
                content.accept(ModItems.RAW_LEAD);
                content.accept(ModItems.RAW_SILVER);
                content.accept(ModItems.RAW_URANIUM);
                content.accept(ModItems.RAW_TUNGSTEN);
                content.accept(ModItems.ZINC_NUGGET);
                content.accept(ModItems.TIN_NUGGET);
                content.accept(ModItems.TITANIUM_NUGGET);
                content.accept(ModItems.LEAD_NUGGET);
                content.accept(ModItems.SILVER_NUGGET);
                content.accept(ModItems.URANIUM_NUGGET);
                content.accept(ModItems.PLUTONIUM_NUGGET);
                content.accept(ModItems.TUNGSTEN_NUGGET);
                content.accept(ModItems.BRONZE_NUGGET);
                content.accept(ModItems.BRASS_NUGGET);
                content.accept(ModItems.COPPER_NUGGET);
                content.accept(ModItems.ROYAL_STEEL_NUGGET);
                content.accept(ModItems.FROST_METAL_NUGGET);
                content.accept(ModItems.EMBER_METAL_NUGGET);
                content.accept(ModItems.TRANSCENDIUM_NUGGET);
                content.accept(ModItems.CURSED_GOLD_NUGGET);
                content.accept(ModItems.ENCHANTED_GOLD_NUGGET);
                content.accept(ModItems.ZINC_INGOT);
                content.accept(ModItems.TIN_INGOT);
                content.accept(ModItems.TITANIUM_INGOT);
                content.accept(ModItems.LEAD_INGOT);
                content.accept(ModItems.SILVER_INGOT);
                content.accept(ModItems.URANIUM_INGOT);
                content.accept(ModItems.PLUTONIUM_INGOT);
                content.accept(ModItems.TUNGSTEN_INGOT);
                content.accept(ModItems.BRONZE_INGOT);
                content.accept(ModItems.BRASS_INGOT);
                content.accept(ModItems.MAGNET_INGOT);
                content.accept(ModItems.ROYAL_STEEL_INGOT);
                content.accept(ModItems.FROST_METAL_INGOT);
                content.accept(ModItems.EMBER_METAL_INGOT);
                content.accept(ModItems.TRANSCENDIUM_INGOT);
                content.accept(ModItems.CURSED_GOLD_INGOT);
                content.accept(ModItems.ENCHANTED_GOLD_INGOT);
                content.accept(ModItems.TOPAZ);
                content.accept(ModItems.RUBY);
                content.accept(ModItems.SAPPHIRE);
                content.accept(ModItems.EXP_GEM);
                content.accept(ModItems.NEGATIVE_MATTER);
                content.accept(ModItems.NEGATIVE_MATTER_NUGGET);
                content.accept(ModItems.NEUTRONIUM_INGOT);
                content.accept(ModItems.STABLE_NEUTRONIUM_INGOT);
                content.accept(ModItems.CHARGED_NEUTRONIUM_INGOT);
                content.accept(ModItems.VOID_MATTER);
                content.accept(ModItems.EXCITED_STATE_VOID_MATTER);
                content.accept(ModItems.EARTH_CORE_SHARD);
                content.accept(ModItems.MULTIPHASE_MATTER);
                content.accept(ModItems.RESIN);
                content.accept(ModItems.AMBER);
                content.accept(ModItems.HARDEND_RESIN);
                content.accept(ModItems.WOOD_FIBER);
                content.accept(ModItems.SPONGE_GEMMULE);
                content.accept(ModItems.LIME_POWDER);
                content.accept(ModItems.LEVITATION_POWDER);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/items/produced.png")
                )
                .textAlignment(CreativeTabSection.TextAlignment.LEFT)
                .textIndent(17)
                .text(Component.translatable("anvilcraft.creative.section.items.produced"))
                .build(),
            content -> {
                content.accept(ModItems.CIRCUIT_BOARD);
                content.accept(ModItems.PROCESSOR);
                content.accept(ModItems.HEAVY_HALBERD_CORE);
                content.accept(ModItems.RESONATOR_CORE);
                content.accept(ModItems.MULTIPHASE_TRANSCENDIUM);
                content.accept(ModItems.DYSON_SPHERE_COMPONENT);
                content.accept(ModItems.PENROSE_SPHERE_COMPONENT);
                content.accept(ModItems.MATTER_DECOMPRESSOR_COMPONENT);
                content.accept(ModItems.WORMHOLE_STABILIZER_COMPONENT);
                content.accept(ModItems.STELLAR_RING_COMPONENT);
                content.accept(ModItems.MAGNETAR_COIL_COMPONENT);
                content.accept(ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/smithing_template.png")
            )
            .textAlignment(CreativeTabSection.TextAlignment.LEFT)
            .textIndent(17)
            .text(Component.translatable("anvilcraft.creative.section.items.smithing_template"))
            .build(),
            content -> {
                content.accept(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE);
                content.accept(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE);
                content.accept(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE);
                content.accept(ModItems.PERMUTATION_TEMPLATE_ITEM);
                content.accept(ModItems.DEFORMATION_TEMPLATE_ITEM);
                content.accept(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE);
                content.accept(ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE);
                content.accept(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE);
            }
        );
        sections.section(
            CreativeTabSection.builder(
                    AnvilCraft.of("textures/gui/creative_inventory/section/items/fluids.png")
                )
                .textAlignment(CreativeTabSection.TextAlignment.LEFT)
                .textIndent(17)
                .text(Component.translatable("anvilcraft.creative.section.items.fluids"))
                .build(),
            content -> {
                content.accept(ModItems.EXP_BUCKET);
                content.accept(ModItems.OIL_BUCKET);
                content.accept(ModItems.MELT_GEM_BUCKET);
                content.accept(ModItems.HYDROGEN_BUCKET);
                content.accept(ModItems.OXYGEN_BUCKET);
                content.accept(ModItems.HELIUM_BUCKET);
                content.accept(ModItems.DEUTERIUM_BUCKET);
                content.accept(ModItems.XENON_BUCKET);
                content.accept(ModItems.KRYPTON_BUCKET);
                ModItems.CEMENT_BUCKETS.forEach((color, bucketItem) -> this.acceptFolded(content, bucketItem));
            }
        );
    }
}
