package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jetbrains.annotations.NotNull;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@SuppressWarnings("unused")
public class ModItemGroups {
    public static final RegistryEntry<CreativeModeTab> ANVILCRAFT_TOOL = REGISTRATE
        .defaultCreativeTab("tools", builder -> builder
            .icon(ModItems.MAGNET::asStack)
            .displayItems((ctx, entries) -> {
                entries.accept(ModItems.GUIDE_BOOK.get().getDefaultInstance());
                entries.accept(ModItems.MAGNET.get().getDefaultInstance());
                entries.accept(ModItems.GEODE.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_PICKAXE.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_AXE.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_SHOVEL.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_HOE.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_SWORD.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_PICKAXE.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_AXE.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_SHOVEL.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_HOE.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_SWORD.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_METAL_PICKAXE.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_METAL_AXE.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_METAL_SHOVEL.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_METAL_HOE.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_METAL_SWORD.get().getDefaultInstance());
                entries.accept(ModItems.ANVIL_HAMMER.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_ANVIL_HAMMER.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_ANVIL_HAMMER.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE.get().getDefaultInstance());
                entries.accept(ModItems.DISK.get().getDefaultInstance());
                entries.accept(ModItems.CRAB_CLAW.asStack());
                entries.accept(ModItems.CAPACITOR.get().getDefaultInstance());
                entries.accept(ModItems.CAPACITOR_EMPTY.get().getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE.get().getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE_BLACK.get().getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE_WHITE.get().getDefaultInstance());
                entries.accept(ModItems.CREAMY_BREAD_ROLL.get().getDefaultInstance());
                entries.accept(ModItems.BEEF_MUSHROOM_STEW.get().getDefaultInstance());
                entries.accept(ModItems.UTUSAN.get().getDefaultInstance());
                entries.accept(ModItems.SEEDS_PACK.get().getDefaultInstance());
                entries.accept(ModItemGroups.createMaxLevelBook(ModEnchantments.FELLING));
                entries.accept(ModItemGroups.createMaxLevelBook(ModEnchantments.HARVEST));
                entries.accept(ModItemGroups.createMaxLevelBook(ModEnchantments.BEHEADING));
            })
            .title(REGISTRATE.addLang("itemGroup", AnvilCraft.of("tools"), "AnvilCraft: Utilities"))
            .build()
        )
        .register();


    public static final RegistryEntry<CreativeModeTab> ANVILCRAFT_INGREDIENTS = REGISTRATE
        .defaultCreativeTab("ingredients", builder -> builder
            .icon(ModItems.MAGNET_INGOT::asStack)
            .displayItems((ctx, entries) -> {
                entries.accept(ModItems.CREAM.get().getDefaultInstance());
                entries.accept(ModItems.FLOUR.get().getDefaultInstance());
                entries.accept(ModItems.DOUGH.get().getDefaultInstance());
                entries.accept(ModItems.COCOA_LIQUOR.get().getDefaultInstance());
                entries.accept(ModItems.COCOA_BUTTER.get().getDefaultInstance());
                entries.accept(ModItems.COCOA_POWDER.get().getDefaultInstance());
                entries.accept(ModItems.BEEF_MUSHROOM_STEW_RAW.get().getDefaultInstance());
                entries.accept(ModItems.UTUSAN_RAW.get().getDefaultInstance());
                entries.accept(ModItems.RAW_ZINC.get().getDefaultInstance());
                entries.accept(ModItems.RAW_TIN.get().getDefaultInstance());
                entries.accept(ModItems.RAW_TITANIUM.get().getDefaultInstance());
                entries.accept(ModItems.RAW_TUNGSTEN.get().getDefaultInstance());
                entries.accept(ModItems.RAW_LEAD.get().getDefaultInstance());
                entries.accept(ModItems.RAW_SILVER.get().getDefaultInstance());
                entries.accept(ModItems.RAW_URANIUM.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_METAL_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.CURSED_GOLD_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.COPPER_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.ZINC_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.TIN_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.TITANIUM_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.TUNGSTEN_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.LEAD_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.SILVER_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.URANIUM_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.MAGNET_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.EMBER_METAL_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.CURSED_GOLD_INGOT.get().getDefaultInstance());
                entries.accept(Items.COPPER_INGOT.getDefaultInstance());
                entries.accept(ModItems.ZINC_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.TIN_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.TITANIUM_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.TUNGSTEN_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.LEAD_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.SILVER_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.URANIUM_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.BRONZE_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.BRASS_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.VOID_MATTER.get().getDefaultInstance());
                entries.accept(ModItems.EARTH_CORE_SHARD.get().getDefaultInstance());
                entries.accept(ModItems.MAGNETOELECTRIC_CORE.get().getDefaultInstance());
                entries.accept(ModItems.SEA_HEART_SHELL_SHARD.get().getDefaultInstance());
                entries.accept(ModItems.SEA_HEART_SHELL.get().getDefaultInstance());
                entries.accept(ModItems.PRISMARINE_CLUSTER.get().getDefaultInstance());
                entries.accept(ModItems.PRISMARINE_BLADE.get().getDefaultInstance());
                entries.accept(ModItems.SPONGE_GEMMULE.get().getDefaultInstance());
                entries.accept(ModItems.NETHERITE_CRYSTAL_NUCLEUS.get().getDefaultInstance());
                entries.accept(ModItems.LIME_POWDER.get().getDefaultInstance());
                entries.accept(ModItems.LEVITATION_POWDER.get().getDefaultInstance());
                entries.accept(ModItems.TOPAZ.get().getDefaultInstance());
                entries.accept(ModItems.RUBY.get().getDefaultInstance());
                entries.accept(ModItems.SAPPHIRE.get().getDefaultInstance());
                entries.accept(ModItems.RESIN.get().getDefaultInstance());
                entries.accept(ModItems.AMBER.get().getDefaultInstance());
                entries.accept(ModItems.HARDEND_RESIN.get().getDefaultInstance());
                entries.accept(ModItems.WOOD_FIBER.get().getDefaultInstance());
                entries.accept(ModItems.CIRCUIT_BOARD.get().getDefaultInstance());
            })
            .title(REGISTRATE.addLang("itemGroup", AnvilCraft.of("ingredients"), "AnvilCraft: Ingredients"))
            .build()
        )
        .register();

    public static final RegistryEntry<CreativeModeTab> ANVILCRAFT_FUNCTION_BLOCK = REGISTRATE
        .defaultCreativeTab("functional_block", builder -> builder
            .icon(ModBlocks.MAGNET_BLOCK::asStack)
            .displayItems((ctx, entries) -> {
                entries.accept(Items.IRON_TRAPDOOR.getDefaultInstance());
                entries.accept(Items.CAULDRON.getDefaultInstance());
                entries.accept(Items.CAMPFIRE.getDefaultInstance());
                entries.accept(Items.STONECUTTER.getDefaultInstance());
                entries.accept(Items.SCAFFOLDING.getDefaultInstance());
                entries.accept(ModBlocks.TRANSPARENT_CRAFTING_TABLE.asStack());
                entries.accept(ModBlocks.STAMPING_PLATFORM.asStack());
                entries.accept(ModBlocks.CORRUPTED_BEACON.asStack());
                entries.accept(ModBlocks.GIANT_ANVIL.asStack());
                entries.accept(Items.ANVIL.getDefaultInstance());
                entries.accept(Items.CHIPPED_ANVIL.getDefaultInstance());
                entries.accept(Items.DAMAGED_ANVIL.getDefaultInstance());
                entries.accept(ModBlocks.SPECTRAL_ANVIL.asStack());
                entries.accept(ModBlocks.ROYAL_ANVIL.asStack());
                entries.accept(ModBlocks.ROYAL_GRINDSTONE.asStack());
                entries.accept(ModBlocks.ROYAL_SMITHING_TABLE.asStack());
                entries.accept(ModBlocks.EMBER_ANVIL.asStack());
                entries.accept(ModBlocks.EMBER_GRINDSTONE.asStack());
                entries.accept(ModBlocks.EMBER_SMITHING_TABLE.asStack());
                entries.accept(ModBlocks.CREATIVE_GENERATOR.asStack());
                entries.accept(ModBlocks.HEATER.asStack());
                entries.accept(ModBlocks.TRANSMISSION_POLE.asStack());
                entries.accept(ModBlocks.REMOTE_TRANSMISSION_POLE.asStack());
                entries.accept(ModBlocks.INDUCTION_LIGHT.asStack());
                entries.accept(ModBlocks.CHARGE_COLLECTOR.asStack());
                entries.accept(ModBlocks.HELIOSTATS.asStack());
                entries.accept(ModBlocks.LOAD_MONITOR.asStack());
                entries.accept(ModBlocks.POWER_CONVERTER_SMALL.asStack());
                entries.accept(ModBlocks.POWER_CONVERTER_MIDDLE.asStack());
                entries.accept(ModBlocks.POWER_CONVERTER_BIG.asStack());
                entries.accept(ModBlocks.PIEZOELECTRIC_CRYSTAL.asStack());
                entries.accept(ModBlocks.AUTO_CRAFTER.asStack());
                entries.accept(ModBlocks.BATCH_CRAFTER.asStack());
                entries.accept(ModBlocks.ITEM_COLLECTOR.asStack());
                entries.accept(ModBlocks.THERMOELECTRIC_CONVERTER.asStack());
                entries.accept(ModBlocks.CHARGER.asStack());
                entries.accept(ModBlocks.DISCHARGER.asStack());
                entries.accept(ModBlocks.ACTIVE_SILENCER.asStack());
                entries.accept(ModBlocks.BLOCK_PLACER.asStack());
                entries.accept(ModBlocks.BLOCK_DEVOURER.asStack());
                entries.accept(ModBlocks.RUBY_LASER.asStack());
                entries.accept(ModBlocks.RUBY_PRISM.asStack());
                entries.accept(ModBlocks.IMPACT_PILE.asStack());
                entries.accept(ModBlocks.OVERSEER_BLOCK.asStack());
                entries.accept(ModBlocks.JEWEL_CRAFTING_TABLE.asStack());
                entries.accept(ModBlocks.MAGNET_BLOCK.asStack());
                entries.accept(ModBlocks.HOLLOW_MAGNET_BLOCK.asStack());
                entries.accept(ModBlocks.FERRITE_CORE_MAGNET_BLOCK.asStack());
                entries.accept(ModBlocks.CRAB_TRAP.asStack());
                entries.accept(ModBlocks.MENGER_SPONGE.asStack());
                entries.accept(ModBlocks.CHUTE.asStack());
                entries.accept(ModBlocks.MAGNETIC_CHUTE.asStack());
                entries.accept(ModBlocks.MINERAL_FOUNTAIN.asStack());
                entries.accept(ModBlocks.SPACE_OVERCOMPRESSOR.asStack());
                entries.accept(ModBlocks.COPPER_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.EXPOSED_COPPER_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.WEATHERED_COPPER_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.OXIDIZED_COPPER_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.TUNGSTEN_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.TITANIUM_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.ZINC_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.TIN_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.LEAD_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.SILVER_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.URANIUM_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.BRONZE_PRESSURE_PLATE.asStack());
                entries.accept(ModBlocks.BRASS_PRESSURE_PLATE.asStack());
            })
            .title(REGISTRATE.addLang("itemGroup",
                AnvilCraft.of("functional_block"),
                "AnvilCraft: Functional Block"))
            .build()
        )
        .register();

    public static final RegistryEntry<CreativeModeTab> ANVILCRAFT_BUILD_BLOCK = REGISTRATE
        .defaultCreativeTab("building_block", builder -> builder
            .icon(ModBlocks.REINFORCED_CONCRETE_BLACK::asStack)
            .displayItems((ctx, entries) -> {
                entries.accept(ModBlocks.ROYAL_STEEL_BLOCK.asStack());
                entries.accept(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK.asStack());
                entries.accept(ModBlocks.CUT_ROYAL_STEEL_BLOCK.asStack());
                entries.accept(ModBlocks.CUT_ROYAL_STEEL_PILLAR.asStack());
                entries.accept(ModBlocks.CUT_ROYAL_STEEL_SLAB.asStack());
                entries.accept(ModBlocks.CUT_ROYAL_STEEL_STAIRS.asStack());
                entries.accept(ModBlocks.EMBER_METAL_BLOCK.asStack());
                entries.accept(ModBlocks.CUT_EMBER_METAL_BLOCK.asStack());
                entries.accept(ModBlocks.CUT_EMBER_METAL_PILLAR.asStack());
                entries.accept(ModBlocks.CUT_EMBER_METAL_SLAB.asStack());
                entries.accept(ModBlocks.CUT_EMBER_METAL_STAIRS.asStack());
                entries.accept(ModBlocks.HEAVY_IRON_BLOCK.asStack());
                entries.accept(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.asStack());
                entries.accept(ModBlocks.POLISHED_HEAVY_IRON_SLAB.asStack());
                entries.accept(ModBlocks.POLISHED_HEAVY_IRON_STAIRS.asStack());
                entries.accept(ModBlocks.CUT_HEAVY_IRON_BLOCK.asStack());
                entries.accept(ModBlocks.CUT_HEAVY_IRON_SLAB.asStack());
                entries.accept(ModBlocks.CUT_HEAVY_IRON_STAIRS.asStack());
                entries.accept(ModBlocks.HEAVY_IRON_PLATE.asStack());
                entries.accept(ModBlocks.HEAVY_IRON_COLUMN.asStack());
                entries.accept(ModBlocks.HEAVY_IRON_BEAM.asStack());
                entries.accept(ModBlocks.DEEPSLATE_ZINC_ORE.asStack());
                entries.accept(ModBlocks.DEEPSLATE_TIN_ORE.asStack());
                entries.accept(ModBlocks.DEEPSLATE_TITANIUM_ORE.asStack());
                entries.accept(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.asStack());
                entries.accept(ModBlocks.DEEPSLATE_LEAD_ORE.asStack());
                entries.accept(ModBlocks.DEEPSLATE_SILVER_ORE.asStack());
                entries.accept(ModBlocks.DEEPSLATE_URANIUM_ORE.asStack());
                entries.accept(ModBlocks.VOID_STONE.asStack());
                entries.accept(ModBlocks.EARTH_CORE_SHARD_ORE.asStack());
                entries.accept(ModBlocks.RAW_ZINC.asStack());
                entries.accept(ModBlocks.RAW_TIN.asStack());
                entries.accept(ModBlocks.RAW_TITANIUM.asStack());
                entries.accept(ModBlocks.RAW_TUNGSTEN.asStack());
                entries.accept(ModBlocks.RAW_LEAD.asStack());
                entries.accept(ModBlocks.RAW_SILVER.asStack());
                entries.accept(ModBlocks.RAW_URANIUM.asStack());
                entries.accept(ModBlocks.CURSED_GOLD_BLOCK.asStack());
                entries.accept(ModBlocks.ZINC_BLOCK.asStack());
                entries.accept(ModBlocks.TIN_BLOCK.asStack());
                entries.accept(ModBlocks.TITANIUM_BLOCK.asStack());
                entries.accept(ModBlocks.TUNGSTEN_BLOCK.asStack());
                entries.accept(ModBlocks.LEAD_BLOCK.asStack());
                entries.accept(ModBlocks.SILVER_BLOCK.asStack());
                entries.accept(ModBlocks.URANIUM_BLOCK.asStack());
                entries.accept(ModBlocks.BRONZE_BLOCK.asStack());
                entries.accept(ModBlocks.BRASS_BLOCK.asStack());
                entries.accept(ModBlocks.TOPAZ_BLOCK.asStack());
                entries.accept(ModBlocks.RUBY_BLOCK.asStack());
                entries.accept(ModBlocks.SAPPHIRE_BLOCK.asStack());
                entries.accept(ModBlocks.RESIN_BLOCK.asStack());
                entries.accept(ModBlocks.AMBER_BLOCK.asStack());
                entries.accept(ModBlocks.VOID_MATTER_BLOCK.asStack());
                entries.accept(ModBlocks.EARTH_CORE_SHARD_BLOCK.asStack());
                entries.accept(ModBlocks.NETHER_DUST.asStack());
                entries.accept(ModBlocks.END_DUST.asStack());
                entries.accept(ModBlocks.CINERITE.asStack());
                entries.accept(ModBlocks.QUARTZ_SAND.asStack());
                entries.accept(ModBlocks.DEEPSLATE_CHIPS.asStack());
                entries.accept(ModBlocks.BLACK_SAND.asStack());
                entries.accept(ModBlocks.TEMPERING_GLASS.asStack());
                entries.accept(ModBlocks.EMBER_GLASS.asStack());

                entries.accept(ModBlocks.HEATED_TUNGSTEN.asStack());
                entries.accept(ModBlocks.HEATED_NETHERITE.asStack());
                entries.accept(ModBlocks.REDHOT_TUNGSTEN.asStack());
                entries.accept(ModBlocks.REDHOT_NETHERITE.asStack());
                entries.accept(ModBlocks.GLOWING_TUNGSTEN.asStack());
                entries.accept(ModBlocks.GLOWING_NETHERITE.asStack());
                entries.accept(ModBlocks.INCANDESCENT_TUNGSTEN.asStack());
                entries.accept(ModBlocks.INCANDESCENT_NETHERITE.asStack());

                entries.accept(ModBlocks.CAKE_BASE_BLOCK.asStack());
                entries.accept(ModBlocks.CREAM_BLOCK.asStack());
                entries.accept(ModBlocks.BERRY_CREAM_BLOCK.asStack());
                entries.accept(ModBlocks.CHOCOLATE_CREAM_BLOCK.asStack());
                entries.accept(ModBlocks.CAKE_BLOCK.asStack());
                entries.accept(ModBlocks.BERRY_CAKE_BLOCK.asStack());
                entries.accept(ModBlocks.CHOCOLATE_CAKE_BLOCK.asStack());
                entries.accept(ModBlocks.LARGE_CAKE.asStack());

                entries.accept(ModBlocks.NESTING_SHULKER_BOX.asStack());
                entries.accept(ModBlocks.OVER_NESTING_SHULKER_BOX.asStack());
                entries.accept(ModBlocks.SUPERCRITICAL_NESTING_SHULKER_BOX.asStack());

                // block
                entries.accept(ModBlocks.REINFORCED_CONCRETE_WHITE.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIGHT_GRAY.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_GRAY.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BLACK.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BROWN.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_RED.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_ORANGE.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_YELLOW.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIME.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_GREEN.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_CYAN.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIGHT_BLUE.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BLUE.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_PURPLE.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_MAGENTA.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_PINK.asStack());
                // slab
                entries.accept(ModBlocks.REINFORCED_CONCRETE_WHITE_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIGHT_GRAY_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_GRAY_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BLACK_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BROWN_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_RED_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_ORANGE_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_YELLOW_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIME_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_GREEN_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_CYAN_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIGHT_BLUE_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BLUE_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_PURPLE_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_MAGENTA_SLAB.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_PINK_SLAB.asStack());
                // stair
                entries.accept(ModBlocks.REINFORCED_CONCRETE_WHITE_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIGHT_GRAY_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_GRAY_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BLACK_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BROWN_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_RED_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_ORANGE_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_YELLOW_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIME_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_GREEN_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_CYAN_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIGHT_BLUE_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BLUE_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_PURPLE_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_MAGENTA_STAIR.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_PINK_STAIR.asStack());
                // wall
                entries.accept(ModBlocks.REINFORCED_CONCRETE_WHITE_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIGHT_GRAY_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_GRAY_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BLACK_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BROWN_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_RED_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_ORANGE_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_YELLOW_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIME_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_GREEN_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_CYAN_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_LIGHT_BLUE_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_BLUE_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_PURPLE_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_MAGENTA_WALL.asStack());
                entries.accept(ModBlocks.REINFORCED_CONCRETE_PINK_WALL.asStack());
            })
            .title(REGISTRATE.addLang("itemGroup",
                AnvilCraft.of("building_block"),
                "AnvilCraft: Building Block"))
            .build()
        )
        .register();

    public static void register() {
    }

    @SuppressWarnings("SameParameterValue")
    private static @NotNull ItemStack createMaxLevelBook(@NotNull RegistryEntry<? extends Enchantment> enchantment) {
        return EnchantedBookItem.createForEnchantment(
            new EnchantmentInstance(enchantment.get(), enchantment.get().getMaxLevel())
        );
    }
}
