package dev.dubhe.anvilcraft.api.tooltip;

import com.google.common.collect.Maps;
import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.block.PowerConverterBigBlock;
import dev.dubhe.anvilcraft.block.PowerConverterExtremelyBigBlock;
import dev.dubhe.anvilcraft.block.PowerConverterMiddleBlock;
import dev.dubhe.anvilcraft.block.PowerConverterSmallBlock;
import dev.dubhe.anvilcraft.block.PowerConverterSuperBigBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.tooltip.StorageTooltip;
import dev.dubhe.anvilcraft.item.amulet.AmuletBoxItem;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemTooltipManager {

    private static final Map<Item, String> NORMAL = Maps.newHashMap();
    private static final Map<Item, String> SHIFT = Maps.newHashMap();
    private static final Map<UUID, StorageServerStub.StorageUsage> STORAGE_USAGE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> STORAGE_USAGE_TIMES = new ConcurrentHashMap<>();
    private static final Set<UUID> STORAGE_USAGE_PENDING = ConcurrentHashMap.newKeySet();
    private static final long STORAGE_USAGE_REFRESH_INTERVAL = 2000L;

    static {
        NORMAL.put(ModItems.MAGNET.get(), "Attract surrounding items when use");
        NORMAL.put(ModItems.GEODE.get(), "Find the surrounding Amethyst Geode when using it");
        NORMAL.put(ModItems.ANVIL_HAMMER.get(), "It's a hammer, an anvil, a wrench, goggles, and a mace");
        NORMAL.put(ModItems.ROYAL_ANVIL_HAMMER.get(), "It's a hammer, an anvil, a wrench, goggles, and a mace");
        NORMAL.put(ModItems.EMBER_ANVIL_HAMMER.get(), "It's a hammer, an anvil, a wrench, goggles, and a mace");
        NORMAL.put(ModItems.FROST_ANVIL_HAMMER.get(), "It's a hammer, an anvil, a wrench, goggles, and a mace");
        NORMAL.put(ModItems.TRANSCENDENCE_ANVIL_HAMMER.get(), "It's a hammer, an anvil, a wrench, goggles, and a mace");
        NORMAL.put(ModBlocks.CURSED_GOLD_BLOCK.asItem(), "Carriers will be cursed");
        NORMAL.put(ModItems.CURSED_GOLD_INGOT.get(), "Carriers will be cursed");
        NORMAL.put(ModItems.CURSED_GOLD_NUGGET.get(), "Carriers will be cursed");
        NORMAL.put(ModBlocks.ENCHANTED_GOLD_BLOCK.asItem(), "Carrying enchanted gold cancels cursed gold debuffs");
        NORMAL.put(
            ModItems.ENCHANTED_GOLD_INGOT.get(), """
                Carrying enchanted gold cancels cursed gold debuffs
                Piglins barter with it four times"""
        );
        NORMAL.put(ModItems.ENCHANTED_GOLD_NUGGET.get(), "Carrying enchanted gold cancels cursed gold debuffs");
        NORMAL.put(
            ModFoodItems.CURSED_GOLDEN_APPLE.get(), """
                Eating teleports:
                Overworld <-> Nether
                End -> respawn point"""
        );
        NORMAL.put(ModItems.TOPAZ.get(), "Containing the power of lightning");
        NORMAL.put(ModItems.RUBY.get(), "Containing the power of fire");
        NORMAL.put(ModItems.SAPPHIRE.get(), "Containing the power of frost");
        NORMAL.put(ModBlocks.RESIN_BLOCK.asItem(), "Use to capture friendly or weak hostile creatures LivingEntity");
        NORMAL.put(ModBlocks.CRAB_TRAP.asItem(), "Placing it in the water to help you catch aquatic products");
        NORMAL.put(ModItems.CRAB_CLAW.get(), "Increase touch length when holding");
        NORMAL.put(
            ModItems.CEMENT_BUCKETS.get(Color.GRAY).get(),
            "Engineering’s irreplaceable cornerstone"
        );
        for (Color color : Color.values()) {
            if (color != Color.GRAY) {
                NORMAL.put(
                    ModItems.CEMENT_BUCKETS.get(color).get(),
                    "Produced by dyeing Gray Cement"
                );
            }
        }
        NORMAL.put(
            ModBlocks.ROYAL_ANVIL.asItem(), """
                Never triggers Too Expensive
                Explosion proof, does not degrade from falling""");
        NORMAL.put(ModBlocks.ROYAL_GRINDSTONE.asItem(), "Removes curses and repair costs, Explosion proof");
        NORMAL.put(ModBlocks.ROYAL_SMITHING_TABLE.asItem(), "Does not consume Smithing Templates, Explosion proof");
        NORMAL.put(
            ModBlocks.TRANSCENDENCE_SMITHING_TABLE.asItem(),
            "Performs all smithing operations with no templates required\nExplosion, Wither and Ender Dragon proof"
        );
        NORMAL.put(ModBlocks.HEATER.asItem(), "Heating the block above, consumes 16 kW");
        NORMAL.put(
            ModBlocks.BURNING_HEATER.asItem(), """
                Consume fuel to heat the block above
                Each crafting consumes 240 seconds of burn time"""
        );
        NORMAL.put(ModBlocks.TRANSMISSION_POLE.asItem(), "Build a power grid with a transmission length of 8");
        NORMAL.put(ModBlocks.CHARGE_COLLECTOR.asItem(), "Collecting charges to generate power");
        NORMAL.put(ModBlocks.FE_COLLECTOR.asItem(), "Collecting FE to generate power");
        NORMAL.put(ModBlocks.PIEZOELECTRIC_CRYSTAL.asItem(), "Charge generated by an anvil fall on it");
        NORMAL.put(
            ModBlocks.MAGNET_BLOCK.asItem(),
            "Attracting the anvil below, when pushed and pulled by the piston, causes adjacent copper blocks to generate charges"
        );
        NORMAL.put(
            ModBlocks.HOLLOW_MAGNET_BLOCK.asItem(),
            "Attracting the anvil below, when pushed and pulled by the piston, causes adjacent copper blocks to generate charges"
        );
        NORMAL.put(
            ModBlocks.FERRITE_CORE_MAGNET_BLOCK.asItem(),
            "Attracting the anvil below, when pushed and pulled by the piston, causes adjacent copper blocks to generate charges"
        );
        NORMAL.put(
            ModBlocks.BATCH_CRAFTER.asItem(),
            "Received a redstone signal and crafted all internal items at once, with a power consumption of 4 kW"
        );
        NORMAL.put(
            ModBlocks.BATCH_CUTTER.asItem(),
            "Received a redstone signal and cut all internal items at once, with a power consumption of 4 kW"
        );
        NORMAL.put(ModItems.ROYAL_STEEL_INGOT.get(), "A piece of iron infused with gem magic");
        NORMAL.put(ModBlocks.ROYAL_STEEL_BLOCK.asItem(), "A large block of iron infused with gem magic, Explosion proof");
        NORMAL.put(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK.asItem(), "Royal Steel decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_ROYAL_STEEL_BLOCK.asItem(), "Royal Steel decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_ROYAL_STEEL_PILLAR.asItem(), "Royal Steel decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_ROYAL_STEEL_STAIRS.asItem(), "Royal Steel decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_ROYAL_STEEL_SLAB.asItem(), "Royal Steel decorative block, Explosion proof");
        NORMAL.put(ModBlocks.TEMPERING_GLASS.asItem(), "Royal Steel glass, Explosion proof, No tools required on collect");
        NORMAL.put(ModBlocks.REMOTE_TRANSMISSION_POLE.asItem(), "Build a power grid with a transmission length of 16");
        NORMAL.put(ModBlocks.HEAVY_IRON_BLOCK.asItem(), "Heavy Iron block, highly compressed iron, Explosion proof");
        NORMAL.put(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.POLISHED_HEAVY_IRON_SLAB.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.POLISHED_HEAVY_IRON_STAIRS.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_HEAVY_IRON_BLOCK.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_HEAVY_IRON_SLAB.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_HEAVY_IRON_STAIRS.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.HEAVY_IRON_PLATE.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.HEAVY_IRON_COLUMN.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.HEAVY_IRON_BEAM.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.HEAVY_IRON_WALL.asItem(), "Heavy Iron decorative block, Explosion proof");
        NORMAL.put(ModBlocks.HEAVY_IRON_DOOR.asItem(), "Heavy Iron door, Explosion proof");
        NORMAL.put(ModBlocks.HEAVY_IRON_TRAPDOOR.asItem(), "Heavy Iron trapdoor, Explosion proof");
        NORMAL.put(ModBlocks.ITEM_COLLECTOR.asItem(), "Adjust power consumption based on range and cooling, from 2kW to 32kW");
        NORMAL.put(
            ModBlocks.EMBER_ANVIL.asItem(), """
               Enhanced compatibility with a soul seemingly hidden deep within
               Anvil Looting can obtain player-only drops
               Wither proof, does not degrade from falling""");
        NORMAL.put(ModBlocks.EMBER_GRINDSTONE.asItem(), "Extracts enchantments onto books, Wither proof");
        NORMAL.put(ModBlocks.EMBER_SMITHING_TABLE.asItem(), "All-in-one combination smithing, Wither proof");
        NORMAL.put(ModBlocks.EMBER_METAL_BLOCK.asItem(), "A large block of heat-resistant Netherite tempered in fire for eons, Wither proof");
        NORMAL.put(ModBlocks.EMBER_GLASS.asItem(), "Ember Metal glass, Wither proof, No tools required on collect");
        NORMAL.put(ModBlocks.CUT_EMBER_METAL_BLOCK.asItem(), "Ember Metal decorative block, Wither proof");
        NORMAL.put(ModBlocks.CUT_EMBER_METAL_PILLAR.asItem(), "Ember Metal decorative block, Wither proof");
        NORMAL.put(ModBlocks.CUT_EMBER_METAL_SLAB.asItem(), "Ember Metal decorative block, Wither proof");
        NORMAL.put(ModBlocks.CUT_EMBER_METAL_STAIRS.asItem(), "Ember Metal decorative block, Wither proof");
        NORMAL.put(ModItems.TRANSCENDIUM_INGOT.get(), "A piece of strong-interaction matter sustained by magic, immune to most forms of destruction");
        NORMAL.put(ModBlocks.TRANSCENDIUM_BLOCK.asItem(), "A large block of strong-interaction matter sustained by magic, immune to most forms of destruction");
        NORMAL.put(
            ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.asItem(), """
               Extreme heat has broken its mass-energy balance; injecting mass will transform it into Transcendium
               May degrade into Netherite upon cooling""");
        NORMAL.put(ModBlocks.EMBER_DECO_BLOCK.asItem(), "Ember Metal decorative block, low content, not explosion proof");
        NORMAL.put(ModBlocks.EMBER_DECO_OUTLINE.asItem(), "Ember Metal decorative block, low content, not explosion proof");
        NORMAL.put(ModItems.EMBER_METAL_INGOT.get(), "A piece of heat-resistant Netherite tempered in fire for eons");
        NORMAL.put(ModItems.FROST_METAL_INGOT.get(), "A piece of cold-resistant Royal Steel tempered in extreme cold for eons");
        NORMAL.put(ModItems.MAGNET_INGOT.get(), "A piece of magnetized iron");
        NORMAL.put(ModItems.TUNGSTEN_INGOT.get(), "A piece of heat-resistant, dense metal, material for Ancient Debris");
        NORMAL.put(ModBlocks.TUNGSTEN_BLOCK.asItem(), "A large block of heat-resistant, dense metal that can be heated to extreme temperatures");
        NORMAL.put(ModItems.TITANIUM_INGOT.get(), "A piece of strong, lightweight metal");
        NORMAL.put(ModBlocks.TITANIUM_BLOCK.asItem(), "A large block of strong, lightweight metal");
        NORMAL.put(ModItems.ZINC_INGOT.get(), "A piece of lightweight metal");
        NORMAL.put(ModBlocks.ZINC_BLOCK.asItem(), "A large block of lightweight metal");
        NORMAL.put(ModItems.TIN_INGOT.get(), "A piece of soft, corrosion-resistant metal");
        NORMAL.put(ModBlocks.TIN_BLOCK.asItem(), "A large block of soft, corrosion-resistant metal");
        NORMAL.put(ModItems.LEAD_INGOT.get(), "A piece of dense, heavy metal");
        NORMAL.put(ModBlocks.LEAD_BLOCK.asItem(), "A large block of dense, heavy metal that absorbs radiation and slows the decay of radioactive blocks");
        NORMAL.put(ModItems.SILVER_INGOT.get(), "A piece of highly reflective metal");
        NORMAL.put(ModBlocks.SILVER_BLOCK.asItem(), "A large block of highly reflective metal");
        NORMAL.put(ModItems.URANIUM_INGOT.get(), "A piece of radioactive material — handle with care");
        NORMAL.put(ModBlocks.URANIUM_BLOCK.asItem(), "A large block of radioactive material that continuously releases heat but decays when multiple blocks are adjacent");
        NORMAL.put(ModItems.PLUTONIUM_INGOT.get(), "A piece of highly radioactive material — cannot be mined naturally, obtained from uranium transmutation");
        NORMAL.put(ModBlocks.PLUTONIUM_BLOCK.asItem(), "A large block of highly radioactive material obtained only by transmuting uranium; continuously releases heat but decays when multiple blocks are adjacent");
        NORMAL.put(ModItems.BRONZE_INGOT.get(), "A piece of durable copper-tin alloy");
        NORMAL.put(ModBlocks.BRONZE_BLOCK.asItem(), "A large block of durable copper-tin alloy");
        NORMAL.put(ModItems.BRASS_INGOT.get(), "A piece of corrosion-resistant copper-zinc alloy");
        NORMAL.put(ModBlocks.BRASS_BLOCK.asItem(), "A large block of corrosion-resistant copper-zinc alloy");
        NORMAL.put(ModBlocks.CUT_BRONZE_BLOCK.asItem(), "Bronze decorative block");
        NORMAL.put(ModBlocks.CUT_BRONZE_STAIRS.asItem(), "Bronze decorative block");
        NORMAL.put(ModBlocks.CUT_BRONZE_SLAB.asItem(), "Bronze decorative block");
        NORMAL.put(ModBlocks.CUT_BRONZE_PILLAR.asItem(), "Bronze decorative block");
        NORMAL.put(ModBlocks.CHISELED_BRONZE_BLOCK.asItem(), "Bronze decorative block");
        NORMAL.put(ModBlocks.CUT_BRASS_BLOCK.asItem(), "Brass decorative block");
        NORMAL.put(ModBlocks.CUT_BRASS_STAIRS.asItem(), "Brass decorative block");
        NORMAL.put(ModBlocks.CUT_BRASS_SLAB.asItem(), "Brass decorative block");
        NORMAL.put(ModBlocks.CUT_BRASS_PILLAR.asItem(), "Brass decorative block");
        NORMAL.put(ModBlocks.CHISELED_BRASS_BLOCK.asItem(), "Brass decorative block");
        NORMAL.put(ModItems.ROYAL_STEEL_NUGGET.get(), "A small piece of iron infused with gem magic");
        NORMAL.put(ModItems.EMBER_METAL_NUGGET.get(), "A small piece of heat-resistant Netherite tempered in fire for eons");
        NORMAL.put(ModItems.FROST_METAL_NUGGET.get(), "A small piece of cold-resistant Royal Steel tempered in extreme cold for eons");
        NORMAL.put(ModItems.TRANSCENDIUM_NUGGET.get(), "A small piece of strong-interaction matter sustained by magic, immune to most forms of destruction");
        NORMAL.put(ModItems.TUNGSTEN_NUGGET.get(), "A small piece of heat-resistant, high-density metal, material for Ancient Debris");
        NORMAL.put(ModItems.TITANIUM_NUGGET.get(), "A small piece of strong, lightweight metal");
        NORMAL.put(ModItems.ZINC_NUGGET.get(), "A small piece of lightweight metal");
        NORMAL.put(ModItems.TIN_NUGGET.get(), "A small piece of soft, corrosion-resistant metal");
        NORMAL.put(ModItems.LEAD_NUGGET.get(), "A small piece of dense, heavy metal");
        NORMAL.put(ModItems.SILVER_NUGGET.get(), "A small piece of highly reflective metal");
        NORMAL.put(ModItems.URANIUM_NUGGET.get(), "A small piece of radioactive material — handle with care");
        NORMAL.put(ModItems.PLUTONIUM_NUGGET.get(), "A small piece of highly radioactive material, cannot be mined naturally, obtained from uranium transmutation");
        NORMAL.put(ModItems.BRONZE_NUGGET.get(), "A small piece of durable copper-tin alloy");
        NORMAL.put(ModItems.BRASS_NUGGET.get(), "A small piece of corrosion-resistant copper-zinc alloy");
        NORMAL.put(ModItems.TIN_CAN.asItem(), "Tin cans can be combined with any food to obtain canned food");
        NORMAL.put(ModFoodItems.CANNED_FOOD.asItem(), "Stackable instant food");
        NORMAL.put(ModItems.IONOCRAFT.asItem(), "It will float when placed in the power grid");
        NORMAL.put(ModItems.LEVITATION_POWDER.asItem(), "Slight weightlessness");
        NORMAL.put(ModItems.NEGATIVE_MATTER.asItem(), "Negative matter is not antimatter, it is anti gravity matter");
        NORMAL.put(ModItems.NEGATIVE_MATTER_NUGGET.asItem(), "Negative matter is not antimatter, it is anti gravity matter");
        NORMAL.put(ModBlocks.NEGATIVE_MATTER_BLOCK.asItem(), "Negative matter is not antimatter, it is anti gravity matter");
        NORMAL.put(ModItems.VOID_MATTER.get(), "The primordial substance that creates all things, mined from the void, decays outside the void");
        NORMAL.put(ModItems.EXCITED_STATE_VOID_MATTER.get(), "The substance of black hole singularities, more unstable than ordinary void matter");
        NORMAL.put(ModItems.EARTH_CORE_SHARD.get(), "A fragment of a planet's heart, pulsing with geological might");
        NORMAL.put(ModItems.MULTIPHASE_MATTER.get(), "Matter that exists in multiple stable phases simultaneously, switchable under special conditions");
        NORMAL.put(ModItems.NEUTRONIUM_INGOT.asItem(), "Pass through most blocks except end dust, negative matter block, and bedrock");
        NORMAL.put(ModItems.STABLE_NEUTRONIUM_INGOT.asItem(), "No more passing through blocks");
        NORMAL.put(
            ModItems.CHARGED_NEUTRONIUM_INGOT.asItem(),
            "No longer passing through blocks, storing a large amount of electrical energy"
        );
        NORMAL.put(
            ModBlocks.TESLA_TOWER.asItem(),
            "Shocks mobs or lightning rods within 8 blocks, consumes 128 kW"
        );
        NORMAL.put(ModBlocks.ACTIVE_SILENCER.asItem(), "Eliminate selected nearby sounds");
        NORMAL.put(ModBlocks.COPPER_PRESSURE_PLATE.asItem(), "Redstone signal increases with pressing time, also a copper plate");
        NORMAL.put(ModBlocks.EXPOSED_COPPER_PRESSURE_PLATE.asItem(), "Redstone signal increases with pressing time, also a copper plate");
        NORMAL.put(ModBlocks.WEATHERED_COPPER_PRESSURE_PLATE.asItem(), "Redstone signal increases with pressing time, also a copper plate");
        NORMAL.put(ModBlocks.OXIDIZED_COPPER_PRESSURE_PLATE.asItem(), "Redstone signal increases with pressing time, also a copper plate");
        NORMAL.put(
            ModBlocks.ZINC_PRESSURE_PLATE.asItem(),
            "Output a redstone signal based on the highest percentage of health of the mobs above, also a zinc plate"
        );
        NORMAL.put(
            ModBlocks.TIN_PRESSURE_PLATE.asItem(),
            "Output a redstone signal based on the lowest percentage of health of the mobs above, also a tin plate"
        );
        NORMAL.put(
            ModBlocks.LEAD_PRESSURE_PLATE.asItem(),
            "Output redstone signal based on the number of mob species above, also a lead plate"
        );
        NORMAL.put(
            ModBlocks.SILVER_PRESSURE_PLATE.asItem(),
            "Output redstone signal based on the number of undead mobs above, also a silver plate"
        );
        NORMAL.put(
            ModBlocks.TUNGSTEN_PRESSURE_PLATE.asItem(),
            "Output redstone signal based on the number of fire-resistant entities above, also a tungsten plate"
        );
        NORMAL.put(
            ModBlocks.TITANIUM_PRESSURE_PLATE.asItem(),
            "Output a redstone signal based on the highest durability of the items above, also a titanium plate"
        );
        NORMAL.put(
            ModBlocks.URANIUM_PRESSURE_PLATE.asItem(),
            "Output a redstone signal based on the lowest durability of the items above, also a uranium plate"
        );
        NORMAL.put(
            ModBlocks.PLUTONIUM_PRESSURE_PLATE.asItem(),
            "Output a redstone signal based on the player in hand item durability, also a plutonium plate"
        );
        NORMAL.put(
            ModBlocks.BRONZE_PRESSURE_PLATE.asItem(),
            "Output a redstone signal based on player satiety above, also a bronze plate"
        );
        NORMAL.put(
            ModBlocks.BRASS_PRESSURE_PLATE.asItem(),
            "Output a redstone signal based on the inventory's filling degree of player above, also a brass plate"
        );
        NORMAL.put(ModItems.MULTITOOL_ITEM.get(), "Press [Alt] to switch tool modes");
        NORMAL.put(ModItems.SPECTRAL_SLINGSHOT.get(), "Load a weapon to fire its phantom");
        NORMAL.put(ModItems.RECOVERY_PEARL.get(), "Right-click to teleport to last death point");
        NORMAL.put(ModBlocks.HEAT_COLLECTOR.asItem(), "Generates power from heat");
        NORMAL.put(ModBlocks.VOID_ENERGY_COLLECTOR.asItem(), "Generates power from Void energy");
        NORMAL.put(ModBlocks.RUBY_LASER.asItem(), "Emits a laser beam when powered");
        NORMAL.put(ModBlocks.CREATIVE_LASER.asItem(), "Emits a laser beam");
        NORMAL.put(ModBlocks.RUBY_PRISM.asItem(), "Deflects or converges laser beams");
        NORMAL.put(ModBlocks.TRANSPARENT_CRAFTING_TABLE.asItem(), "Aesthetic, connectable Crafting Table");
        NORMAL.put(ModBlocks.MENGER_SPONGE.asItem(), "Absorbs infinite liquid");
        NORMAL.put(ModBlocks.SLIDING_RAIL.asItem(), "Frictionless surface for sliding entities and even blocks");
        NORMAL.put(ModBlocks.POWERED_SLIDING_RAIL.asItem(), "Accelerates items, entities, or blocks when powered");
        NORMAL.put(ModBlocks.DETECTOR_SLIDING_RAIL.asItem(), "Output signal when objects or blocks slide over");
        NORMAL.put(ModBlocks.ACTIVATOR_SLIDING_RAIL.asItem(), "Activates blocks sliding over it");
        NORMAL.put(ModBlocks.SLIDING_RAIL_STOP.asItem(), "Stops sliding items, entities, or blocks");
        NORMAL.put(ModBlocks.PROPEL_PISTON.asItem(), "Integrated piston worm, requires Capacitor or Laser power");
        NORMAL.put(ModBlocks.PULSE_GENERATOR.asItem(), "Customizes pulse delay and duration");
        NORMAL.put(ModBlocks.ADVANCED_COMPARATOR.asItem(), "Supports Hysteresis and Window comparison modes");
        NORMAL.put(ModItems.EMERALD_AMULET.get(), "Grants Hero of the Village");
        NORMAL.put(ModItems.TOPAZ_AMULET.get(), "Grants immunity to lightning damage");
        NORMAL.put(ModItems.RUBY_AMULET.get(), "Grants Fire Resistance");
        NORMAL.put(ModItems.SAPPHIRE_AMULET.get(), "Grants Conduit Power");
        NORMAL.put(ModItems.ANVIL_AMULET.get(), "Grants immunity to anvil damage");
        NORMAL.put(ModItems.FEATHER_AMULET.get(), "Grants immunity to fall damage");
        NORMAL.put(ModItems.CAT_AMULET.get(), "Scares away Creepers and Phantoms");
        NORMAL.put(ModItems.DOG_AMULET.get(), "Scares away Skeletons");
        NORMAL.put(ModItems.SILENCE_AMULET.get(), "Silences the wearer");
        NORMAL.put(
            ModItems.ABNORMAL_AMULET.get(),
            "Prevents damage from carrying Uranium, Plutonium, Floating Powder, Cursed Gold items"
        );
        NORMAL.put(ModItems.NATURE_AMULET.get(), "Combines Silence, Cat, Dog, and Feather Amulet effects");
        NORMAL.put(ModItems.GEM_AMULET.get(), "Combines effects of all four Gem Amulets");
        NORMAL.put(ModItems.CAPACITOR.asItem(), "8 MFE stored");
        NORMAL.put(ModItems.CAPACITOR_EMPTY.asItem(), "8 MFE capacity");
        NORMAL.put(ModItems.SUPER_CAPACITOR.asItem(), "160 MFE stored");
        NORMAL.put(ModItems.SUPER_CAPACITOR_EMPTY.asItem(), "160 MFE capacity");
        NORMAL.put(ModItems.HEAVY_HALBERD_CORE.get(), "Material for crafting the Heavy Halberd");
        NORMAL.put(ModItems.RESONATOR_CORE.get(), "Material for crafting the Resonator");
        NORMAL.put(ModBlocks.BLACK_HOLE.asItem(), "Dev Block with intense gravitational attraction");
        NORMAL.put(ModBlocks.WHITE_HOLE.asItem(), "Dev Block with intense gravitational repulsion");
        NORMAL.put(ModBlocks.CHARGER.asItem(), "Charges items, supports manual or automated input");
        NORMAL.put(ModBlocks.DISCHARGER.asItem(), "Discharges capacitors, supports manual or automated input");
        NORMAL.put(ModBlocks.LASER_RECEIVER.asItem(), "Receives lasers, generating power and a redstone signal based on the laser level");
        NORMAL.put(
            ModBlocks.FROST_ANVIL.asItem(), """
            Slower enchantment penalty growth, repairs any item with Frost Metal, free renaming
            Explosion proof, does not degrade from falling"""
        );
        NORMAL.put(
            ModBlocks.TRANSCENDENCE_ANVIL.asItem(), """
            Ignores enchantment level limits, and Anvil Looting produces additional drops
            Immune to most destruction methods, does not degrade from falling"""
        );
        NORMAL.put(
            ModBlocks.TRANSCENDENCE_GRINDSTONE.asItem(),
            """
            Removes curses and repair costs, selectively removes or transfers multiple enchantments
            Immune to most destruction methods"""
        );
        NORMAL.put(ModBlocks.TRANSCENDENCE_DECO_BLOCK.asItem(), "Transcendium decorative block – low content, not indestructible");
        NORMAL.put(ModBlocks.TRANSCENDENCE_DECO_OUTLINE.asItem(), "Transcendium decorative block – low content, not indestructible");
        NORMAL.put(ModBlocks.FROST_GRINDSTONE.asItem(), "Selectively removes individual enchantments, Explosion proof");
        NORMAL.put(ModBlocks.FROST_SMITHING_TABLE.asItem(), "Works with Permutation and Deformation smithing templates, Explosion proof");
        NORMAL.put(ModBlocks.FROST_METAL_BLOCK.asItem(), "A large block of cold-resistant Royal Steel tempered in extreme cold for eons, Explosion proof");
        NORMAL.put(ModBlocks.FROST_GLASS.asItem(), "Frost Metal glass, Explosion proof, No tools required on collect");
        NORMAL.put(ModBlocks.CUT_FROST_METAL_BLOCK.asItem(), "Frost Metal decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_FROST_METAL_PILLAR.asItem(), "Frost Metal decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_FROST_METAL_SLAB.asItem(), "Frost Metal decorative block, Explosion proof");
        NORMAL.put(ModBlocks.CUT_FROST_METAL_STAIRS.asItem(), "Frost Metal decorative block, Explosion proof");
        NORMAL.put(ModBlocks.FROST_DECO_BLOCK.asItem(), "Frost Metal decorative block, low content, not explosion proof");
        NORMAL.put(ModBlocks.FROST_DECO_OUTLINE.asItem(), "Frost Metal decorative block, low content, not explosion proof");
        NORMAL.put(ModBlocks.SPECTRAL_ANVIL.asItem(), "Creates phantom shadows when the upper magnet is demagnetized");
        NORMAL.put(ModBlocks.BLOCK_PLACER.asItem(), "Places blocks in front when powered by redstone");
        NORMAL.put(ModBlocks.STRUCTURE_SCANNER.asItem(), "Scans and stores structures in Structure Disk");
        NORMAL.put(ModItems.STRUCTURE_DISK.get(), "Stores structure data, used in blueprint mode of Smart Block Placer");
        NORMAL.put(ModBlocks.SMART_BLOCK_PLACER.asItem(), "Advanced block placer with 5x5x5 configurable placement area");
        NORMAL.put(ModBlocks.FISH_TANK.asItem(), "It is sturdier than it looks, used for anvil synthesis");
        NORMAL.put(ModBlocks.BLOCK_DEVOURER.asItem(), "Breaks 3×3 area of blocks in front when powered by redstone");
        NORMAL.put(ModBlocks.INDUCTION_LIGHT.asItem(), "Provides lighting and configurable special modes");
        NORMAL.put(ModBlocks.HELIOSTATS.asItem(), "Heats targeted blocks during the day");
        NORMAL.put(
            ModItems.IONOCRAFT_BACKPACK.asItem(), """
            Allows creative flight while equipped
            Requires power from the energy grid or capacitors in the inventory"""
        );
        NORMAL.put(ModBlocks.BLOCK_COMPARATOR.asItem(), "Outputs signal when side blocks are the same, right-click to switch to precise state detection mode");
        NORMAL.put(ModBlocks.ITEM_DETECTOR.asItem(), "Detects specific items behind (drops/containers) to output redstone signal");
        NORMAL.put(ModBlocks.IMPACT_PILE.asItem(), "Place on Bedrock or Deepslate and strike with falling anvil to create Moneral Fountain");
        NORMAL.put(ModBlocks.OVERSEER_BLOCK.asItem(), "Chunk loader that works on Royal Steel base");
        NORMAL.put(ModBlocks.SPACE_OVERCOMPRESSOR.asItem(), "Compresses items into Neutronium Ingots, compresses multiblock outputs into drops");
        NORMAL.put(ModBlocks.MASS_ENERGY_INVERTER.asItem(), "Doubles the mass growth of adjacent Space Overcompressors, consumes 1024 kW");
        NORMAL.put(ModBlocks.ACCELERATION_RING.asItem(), "Creates acceleration field for anvils, projectiles, or players with Anvil Hammer");
        NORMAL.put(ModBlocks.DEFLECTION_RING.asItem(), "Deflects passing objects 90°, detect speed with Comparator");
        NORMAL.put(ModItems.DRAGON_ROD.asItem(), "Portable block devourer with adjustable range");
        NORMAL.put(ModItems.EMBER_DRAGON_ROD.asItem(), "Portable block devourer with adjustable range");
        NORMAL.put(ModItems.FROST_DRAGON_ROD.asItem(), "Portable block devourer with adjustable range");
        NORMAL.put(ModItems.ROYAL_DRAGON_ROD.asItem(), "Portable block devourer with adjustable range");
        NORMAL.put(ModItems.TRANSCENDENCE_DRAGON_ROD.asItem(), "Portable block devourer with adjustable range");
        NORMAL.put(ModItems.FILTER.asItem(), "Matches items based on configuration, usable in any filter slot");
        NORMAL.put(ModItems.TOTEM_OF_RECOVERY.asItem(), "Teleports to spawn on death, grants a Recall Pearl to return to death point");
        NORMAL.put(ModItems.TOTEM_OF_RAGE.asItem(), "Grants invulnerability and berserk on fatal damage, death is inevitable after 1 minute");
        NORMAL.put(ModItems.COMRADE_AMULET.asItem(), "Signable by players via right-click, prevents damage from signed players");
        NORMAL.put(ModItems.PILL_BOX.asItem(), "Store pills for quick use");
        NORMAL.put(ModItems.AMULET_BOX.asItem(), "Stores multiple active amulets or totems");
        NORMAL.put(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem(), "Forge celestial bodies, build megastructures");
        NORMAL.put(
            ModItems.PIPE.get(), """
            Transports fluids between containers, gravity-driven flow
            Right-click with Glass Pane to turn it into Glass Pipe"""
        );
        NORMAL.put(
            ModItems.GLASS_PIPE.get(), """
            Creative mode only
            Use an Anvil Hammer to convert it into normal Pipe"""
        );
        NORMAL.put(ModBlocks.PUMP.asItem(), "Pumps fluids, consumes 32 kW");
        NORMAL.put(ModBlocks.CREATIVE_CRATE.asItem(), "Infinite item storage and supply");
        NORMAL.put(ModBlocks.CRATE.asItem(), "Stores items");
        NORMAL.put(ModBlocks.LARGE_CRATE.asItem(), "A large crate, stores more items");
        NORMAL.put(ModBlocks.SHULKER_CONTAINER.asItem(), "A space-folding container upgraded from a Large Crate");
        NORMAL.put(ModBlocks.HYPERDIMENSION_STORAGE_STATION.asItem(), "An infinite container upgraded from a Shulker Container");
        NORMAL.put(ModItems.HYPERDIMENSION_TERMINAL.asItem(), "A portable port of the binding Hyperdimension Storage Station");
        NORMAL.put(ModBlocks.CREATIVE_FLUID_TANK.asItem(), "Infinite fluid storage and supply");
        NORMAL.put(ModBlocks.FLUID_TANK.asItem(), "Stores fluids");
        NORMAL.put(ModBlocks.LARGE_FLUID_TANK.asItem(), "Stores multiple fluids");
        NORMAL.put(ModBlocks.DRAIN.asItem(), "Transfers fluid vertically, outputting downward and drawing from above");
        NORMAL.put(ModBlocks.CORRUPTED_BEACON.asItem(), "Releases the wither power within the beacon, its beam accelerates time flow or causes mutations");
        NORMAL.put(ModBlocks.LARGE_CAKE.asItem(), "A cake, a very big cake. 27 bites, each bite fills you up");
        NORMAL.put(ModBlocks.CONFINEMENT_CHAMBER.asItem(), "Contains elementary particles and unstable items, keeping them stable");
        NORMAL.put(ModBlocks.CONFINED_TIME_ANVILON.asItem(), "Confinement chamber for time-type Anvilon");
        NORMAL.put(ModBlocks.CONFINED_SPACE_ANVILON.asItem(), "Confinement chamber for space-type Anvilon");
        NORMAL.put(ModBlocks.CONFINED_MASS_ANVILON.asItem(), "Confinement chamber for mass-type Anvilon");
        NORMAL.put(ModBlocks.CONFINED_ENERGY_ANVILON.asItem(), "Confinement chamber for energy-type Anvilon");
        NORMAL.put(ModBlocks.CONFINED_NEUTRONIUM_INGOT_BLOCK.asItem(), "A confinement chamber containing a Charged Neutronium Ingot");
        NORMAL.put(ModBlocks.NEUTRON_IRRADIATOR.asItem(), "Perform neutron irradiation recipes, absorbs confined anvilons for block procedural process");
        NORMAL.put(ModBlocks.CAKE_BASE_BLOCK.asItem(), "A block of cake base, use a shovel as a spoon to eat it");
        NORMAL.put(ModBlocks.CREAM_BLOCK.asItem(), "A block of cream, use a shovel as a spoon to eat it");
        NORMAL.put(ModBlocks.BERRY_CREAM_BLOCK.asItem(), "A block of berry cream, use a shovel as a spoon to eat it");
        NORMAL.put(ModBlocks.CHOCOLATE_CREAM_BLOCK.asItem(), "A block of chocolate cream, use a shovel as a spoon to eat it");
        NORMAL.put(ModBlocks.CAKE_BLOCK.asItem(), "A block of cream cake, use a shovel as a spoon to eat it");
        NORMAL.put(ModBlocks.BERRY_CAKE_BLOCK.asItem(), "A block of berry cake, use a shovel as a spoon to eat it");
        NORMAL.put(ModBlocks.CHOCOLATE_CAKE_BLOCK.asItem(), "A block of chocolate cake, use a shovel as a spoon to eat it");
        NORMAL.put(ModBlocks.CONTROL_VALVE.asItem(), "Controls the type and flow rate of passing fluids and can be locked by redstone");
        NORMAL.put(ModBlocks.SPACETIME_SUPERCOMPUTER.asItem(), "Consumes power to run certain time commands");
        NORMAL.put(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.asItem(), "Amplifies the Celestial Forging Anvil so it can support larger celestial bodies and megastructures");
        NORMAL.put(ModBlocks.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE.asItem(), "Stores multiple item types, one stack each; switch modes with an Anvil Hammer to output items actively");
        NORMAL.put(ModBlocks.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.asItem(), "Stores multiple fluids, 80 B each; switch modes with an Anvil Hammer to output fluids actively");
        NORMAL.put(ModBlocks.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.asItem(), "Receives lasers; switch modes with an Anvil Hammer to emit lasers actively");
        NORMAL.put(ModBlocks.CELESTIAL_FORGING_ANVIL_INTERFACE_PLACEHOLDER.asItem(), "A decorative, nonfunctional Celestial Forging Anvil interface; right-click it with any of the three functional interfaces to replace it");
        NORMAL.put(ModBlocks.CELESTIAL_FORGING_ANVIL_PORTAL.asItem(), "Teleports players and entities between two portals");
        NORMAL.put(ModBlocks.LENS.asItem(), "Use special glass to enchant lasers");
        NORMAL.put(ModItems.CHECK_VALVE.get(), "Allows fluid to flow in only one direction");
        NORMAL.put(ModItems.DYSON_SPHERE_COMPONENT.get(), "Material for building a Dyson Sphere, used in the Celestial Forging Anvil");
        NORMAL.put(ModItems.PENROSE_SPHERE_COMPONENT.get(), "Material for building a Penrose Sphere, used in the Celestial Forging Anvil");
        NORMAL.put(ModItems.MATTER_DECOMPRESSOR_COMPONENT.get(), "Material for building a Matter Decompressor, used in the Celestial Forging Anvil");
        NORMAL.put(ModItems.WORMHOLE_STABILIZER_COMPONENT.get(), "Material for building a Wormhole Stabilizer, used in the Celestial Forging Anvil");
        NORMAL.put(
            ModItems.STELLAR_RING_COMPONENT.get(),
            "Material for building a Stellar Ring Collider, used in the Celestial Forging Anvil"
        );
        NORMAL.put(
            ModItems.MAGNETAR_COIL_COMPONENT.get(),
            "Material for building a Magnetar Coil, used in the Celestial Forging Anvil"
        );
        NORMAL.put(
            ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT.get(),
            "Material for building a Stellar Evolution Accelerator, used in the Celestial Forging Anvil"
        );
        NORMAL.put(ModBlocks.REDSTONE_WIRE.asItem(), "Transmit redstone signals more precisely");
        NORMAL.put(
            ModBlocks.AUTO_ENCHANTING_TABLE.asItem(),
            "Automatically enchant items with experience fluid, primers, or liquid enchantment"
        );
        NORMAL.put(ModBlocks.TRADING_STATION.asItem(), "Trading platform for players and villagers");
        NORMAL.put(ModBlocks.LARGE_LASER.asItem(), "Equivalent to 16 lasers, outputs 16 intensity levels of laser, consumes 256 kW");
        NORMAL.put(
            ModBlocks.SUGAR_BLOCK.asItem(), """
            A large block of sugar
            Also a piezoelectric crystal, but seems fragile"""
        );
        NORMAL.put(
            ModBlocks.FLINT_BLOCK.asItem(), """
            A large block of flint
            When pushed or pulled by a piston, it creates fire around it if an iron block is nearby"""
        );
        NORMAL.put(
            ModBlocks.GUNPOWER_BLOCK.asItem(), """
            A large block of gunpowder
            If struck by a falling anvil, it explodes and launches the anvil back up to the height it fell from"""
        );
        NORMAL.put(
            ModBlocks.ROTTEN_FLESH_BLOCK.asItem(), """
            A large block of rotten flesh
            It cushions fall damage, but landing on it will make you nauseous for 30 seconds
            Can also be smelted into Netherrack"""
        );
        NORMAL.put(ModBlocks.SINGULARITY_CRYSTAL.asItem(), "Data disk for storing extreme celestial data from the Celestial Forging Anvil");
        NORMAL.put(ModItems.LASER_GUN.get(), "Hold right-click to consume power and fire a laser that grows increasingly powerful");
        NORMAL.put(ModItems.CORRUPTED_BEACON_ACTIVATOR.get(), "Hold right-click to consume power and fire a corruption beam");
        NORMAL.put(ModItems.TESLA_GUN.get(), "Hold right-click to consume power and fire chain lightning that bounces between mobs");
        NORMAL.put(ModItems.ANVIL_RAILGUN.get(), "Hold right-click to consumes power to charge up and launch a high-speed anvil");
        NORMAL.put(ModItems.SPECTRAL_WEAPON_LAUNCHER.get(), "Hold right-click to consume power and fires spectral weapons");
        NORMAL.put(ModItems.SPECTRAL_SLINGSHOT.get(), "Hold right-click to fire spectral weapons");
        NORMAL.put(ModItems.ENERGY_WEAPON_PLATFORM.get(), "640 MFE stored, but will only inherit the result of Energy Weapon Making");
        NORMAL.put(
            ModBlocks.INFINITE_COLLECTOR.asItem(), """
            Generates power by collecting both heat and charge, no upper power limit
            Provide a baseline output of 256 kW"""
        );
        NORMAL.put(ModBlocks.LOAD_MONITOR.asItem(), "Monitor the grid load condition, can output a signal by redstone comparator");
        NORMAL.put(ModBlocks.CHUTE.asItem(), "An advanced Hopper, can transfer a full stack of items at a time");
        NORMAL.put(ModBlocks.MAGNETIC_CHUTE.asItem(), "An advanced Chute, with the ability to transport items vertically");
        NORMAL.put(ModBlocks.EXP_COLLECTOR.asItem(), "Collect nearby EXP orbs and convert them into EXP Fluid");
        NORMAL.put(ModBlocks.GIANT_ANVIL.asItem(), "An extremely huge anvil");
        NORMAL.put(ModBlocks.LARGE_CAULDRON.asItem(), "An extremely huge cauldron");
        NORMAL.put(ModBlocks.STAMPING_PLATFORM.asItem(), "Perform stamping recipes");
        NORMAL.put(ModBlocks.CRUSHING_TABLE.asItem(), "Perform crushing recipes");
        NORMAL.put(ModBlocks.SIFTING_TABLE.asItem(), "Perform sifting recipes");
        NORMAL.put(ModBlocks.UNPACKING_TABLE.asItem(), "Perform unpacking recipes");
        NORMAL.put(
            ModBlocks.JEWEL_CRAFTING_TABLE.asItem(), """
                A crafting station for rare items
                Amulets, Smithing Templates, Music Discs, Trial Keys, Totems, and more"""
        );
        NORMAL.put(ModFoodItems.CHOCOLATE.get(), "Tasty chocolate made with real cocoa butter, eat a bite to gain Speed");
        NORMAL.put(ModFoodItems.CHOCOLATE_BLACK.get(), "Tasty dark chocolate made with real cocoa butter, eat a bite to gain Haste");
        NORMAL.put(ModFoodItems.CHOCOLATE_WHITE.get(), "Tasty white chocolate made with real cocoa butter, eat a bite to gain Jump Boost");
        NORMAL.put(ModItems.SEEDS_PACK.get(), "Open to obtain a random seed or crop");
        NORMAL.put(ModItems.EXP_GEM.get(), "Right-click to extract the EXP contained inside");
        NORMAL.put(ModBlocks.CONTROLLABLE_SAND.asItem(), "Sand that can be controlled by redstone to rise or fall");
        NORMAL.put(ModBlocks.PLYWOOD_BLOCK.asItem(), "Solid wood? Not a chance");
        NORMAL.put(ModBlocks.PLYWOOD_STAIRS.asItem(), "Solid wood? Not a chance");
        NORMAL.put(ModBlocks.PLYWOOD_SLAB.asItem(), "Solid wood? Not a chance");
        NORMAL.put(ModBlocks.ANCIENT_SEA_REEF.asItem(), "A chunk of sea reef – looks like it's hiding some treasure");
        NORMAL.put(ModBlocks.VOID_MATTER_BLOCK.asItem(), "A chunk of void...");
        NORMAL.put(ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK.asItem(), "A chunk of void... exciting void... comes from a Black Hole");
        NORMAL.put(ModBlocks.CREATIVE_GENERATOR.asItem(), "Provide up to 65536 kW of power, can also be used as a load");
        NORMAL.put(ModFoodItems.COCOA_BUTTER.asItem(), "One hundred percent natural pure cocoa butter!");

        SHIFT.put(
            ModItems.LASER_GUN.get(), """
                The laser damages mobs and can also mine blocks
                Charging longer increases damage, but watch out for overheating!
                Enchanting the laser gun alters its beam behavior"""
        );
        SHIFT.put(
            ModItems.CORRUPTED_BEACON_ACTIVATOR.get(), """
                Fires a beam of corruption that pierces through targets, dealing damage and inflicting Wither
                It passes through glass and does not convert mobs
                Enchanting the Corrupted Beacon Exciter boosts the beam's damage"""
        );
        SHIFT.put(
            ModItems.TESLA_GUN.get(), """
                Fires chain lightning that arcs between up to 4 mobs, dealing reduced damage with each bounce
                The lightning can convert mobs, and lightning rods will also be targeted
                Each strike is followed by a cooldown, which can be shortened by enchanting the Tesla Gun"""
        );
        SHIFT.put(
            ModItems.ANVIL_RAILGUN.get(), """
                Hold right-click to load an anvil from your offhand into the railgun
                Then hold right-click again to charge up, and release to launch the anvil
                Longer charging results in higher speed and damage
                Enchanting the Anvil Railgun can boost damage, reduce charge time, or change its firing behavior"""
        );
        SHIFT.put(
            ModItems.SPECTRAL_WEAPON_LAUNCHER.get(), """
                Hold right-click to load your offhand weapon into the Spectral Weapon Launcher
                Once loaded, right-click again to fire a spectral copy of that weapon, dealing damage
                Each shot has a cooldown, which can be reduced by enchanting the launcher"""
        );
        SHIFT.put(
            ModItems.SPECTRAL_SLINGSHOT.get(), """
                Hold right-click to load your offhand weapon into the Spectral Slingshot
                Once loaded, right-click again to fire a spectral copy of that weapon, dealing damage"""
        );
        SHIFT.put(
            ModItems.ENERGY_WEAPON_PLATFORM.get(), """
                640 MFE stored, but will only inherit the result of Energy Weapon Making
                Can be crafted with different materials to create various energy weapons
                Consumes capacitors to restore power"""
        );
        SHIFT.put(
            ModItems.CHECK_VALVE.get(), """
                When holding a check valve, right-click one end of a pipe to convert that end into a check valve
                Can remove check valve by right-clicking it while holding a check valve, or with an empty hand
                Supplying a redstone signal reverses the flow direction of the check valve"""
        );
        SHIFT.put(
            ModBlocks.CELESTIAL_FORGING_ANVIL.asItem(), """
                Place an anvil to determine celestial parameters
                Once a suitable celestial body is found, lock it to build a megastructure
                Unlocking the celestial body will destroy the megastructure
                Different megastructures serve different purposes — check the megastructure button for details"""
        );
        SHIFT.put(
            ModBlocks.SPECTRAL_ANVIL.asItem(), """
                When the upper magnet is demagnetized, a phantom shadow is created and falls downward
                It can pass through transparent blocks, and no matter the actual height, the impact is always treated as a 2‑block fall"""
        );
        SHIFT.put(
            ModBlocks.BLOCK_PLACER.asItem(), """
                When powered by redstone, this block places a block in front of it
                If struck by a falling anvil, the placement distance increases — the farther the anvil falls, the farther the block is placed
                No internal inventory and must obtain blocks from dropped items or container inventories behind it"""
        );
        SHIFT.put(
            ModBlocks.STRUCTURE_SCANNER.asItem(), """
                Can store structures in Structure Disk and be used in the blueprint mode of Smart Block Placer
                Maximum can store 15×15×15 structure
                When powered by redstone, it will automatically scans and stores structures"""
        );
        SHIFT.put(
            ModBlocks.SMART_BLOCK_PLACER.asItem(), """
                Advanced block placer with 5x5x5 placement area, configurable via GUI
                Supports pickup mode (from containers or drops) and move mode (direct block movement)
                Requires power supply, consumes 8 kW
                Put Structure Disk to enable Blue Print Mode, consumes 64 kW"""
        );
        SHIFT.put(
            ModBlocks.FISH_TANK.asItem(), """
                Can be used as a substitute for the alchemy pot to perform related anvil synthesis
                Wearing it on your head provides a temporary underwater breathing effect
                Right-click the top with an item in hand to place the item inside
                Right-click the lower part of the fish tank with a tropical fish bucket in hand to release the tropical fish
                Can interact with Dispensers for fluid transfer"""
        );
        SHIFT.put(
            ModBlocks.BLOCK_DEVOURER.asItem(), """
                When powered by redstone, this block instantly breaks a 3×3 area of blocks in front of it
                If struck by a falling anvil, the breaking range increases — the farther the anvil falls, the larger the area it destroys
                No internal inventory, outputs items behind it — into containers, as dropped items, or at the break location if blocked
                Base world blocks such as stone, dirt, and deepslate drop only small amounts"""
        );
        SHIFT.put(
            ModBlocks.INDUCTION_LIGHT.asItem(), """
                Provides lighting with a power consumption of 1 kW
                Right‑click with Redstone to switch to Growth Acceleration Mode
                Right‑click with Glowstone to switch to Anti‑Monster Spawning Mode
                Right‑click with Void Matter to switch to Anti‑Animal Spawning Mode
                All three special modes consume 16 kW of power"""
        );
        SHIFT.put(
            ModBlocks.HELIOSTATS.asItem(), """
                Right‑click a Netherite Block or Tungsten Block with the handheld heliostat to set target block
                After placing the heliostat, it will heat the targeted block during the day, as well as the blocks above it
                Right‑click a targeted heliostat to inherit its target"""
        );
        SHIFT.put(
            ModBlocks.OVERSEER_BLOCK.asItem(),
            "Chunk loader on 3x3 Royal Steel base, higher base layers increase chunk load range (max 3 layers, 7x7 range)"
        );
        SHIFT.put(
            ModItems.DRAGON_ROD.asItem(),
            "Portable block devourer, left-click to mine, right-click to adjust range, larger range costs more durability"
        );
        SHIFT.put(
            ModItems.EMBER_DRAGON_ROD.asItem(),
            "Portable block devourer, left-click to mine, right-click to adjust range, larger range costs more durability"
        );
        SHIFT.put(
            ModItems.FROST_DRAGON_ROD.asItem(),
            "Portable block devourer, left-click to mine, right-click to adjust range, larger range costs more durability"
        );
        SHIFT.put(
            ModItems.ROYAL_DRAGON_ROD.asItem(),
            "Portable block devourer, left-click to mine, right-click to adjust range, larger range costs more durability"
        );
        SHIFT.put(
            ModItems.TRANSCENDENCE_DRAGON_ROD.asItem(),
            "Portable block devourer, left-click to mine, right-click to adjust range, larger range costs more durability"
        );
        SHIFT.put(
            ModItems.PILL_BOX.asItem(),
            "Store pills, right-click to take one pill each, and press [%s] to use them in the inventory"
        );
        SHIFT.put(
            ModItems.AMULET_BOX.asItem(), """
                Stores multiple active amulets or totems
                Right click to store the Totems of Undying on your inventory, and shift-right-click to retrieve the totems;
                When holding, consume the totems in the box when needed, and after consuming the totem, you may receive a secret gift"""
        );
        SHIFT.put(ModBlocks.PUMP.asItem(), """
                Provides 10 blocks of headlift on both input and output sides (including the pump itself)
                Also functions as check valve, allowing liquid to flow through only in the pump's direction
                A redstone signal disables the pump""");
        SHIFT.put(
            ModBlocks.CREATIVE_CRATE.asItem(), """
                Provides infinite items of a set type: place items inside to configure
                Items will not be consumed when taken out
                Destroys all input items when no item is configured
                Creative players left-click to clear the configuration
                Survival players left-click to take out items"""
        );
        SHIFT.put(
            ModBlocks.CRATE.asItem(), """
                Can contain 2048 items
                Breaking it drops the contents
                When it holds more than 1000 items, hold Shift to break it"""
        );
        SHIFT.put(
            ModBlocks.LARGE_CRATE.asItem(), """
                Can contain 65536 items
                Breaking it drops the contents
                When it holds more than 1000 items, hold Shift to break it
                Can update to Shulker Container"""
        );
        SHIFT.put(
            ModBlocks.SHULKER_CONTAINER.asItem(), """
                Can contain 65536 types of items, each type contain 65536 space of items by default
                Breaking it drops the container with its items stored inside
                Drop Space Overcompressors on top and strike with an anvil to expand capacity
                Each one doubles the storage space (up to 4 times)"""
        );
        SHIFT.put(
            ModBlocks.HYPERDIMENSION_STORAGE_STATION.asItem(), """
                Can contain infinite items
                Breaking it drops the container with its items stored inside"""
        );
        SHIFT.put(
            ModBlocks.CREATIVE_FLUID_TANK.asItem(), """
                Provides infinite fluid of a set type: fill fluid inside to configure
                Fluid will not be consumed when extracted
                Destroys all input fluid when no fluid is configured"""
        );
        SHIFT.put(
            ModBlocks.SPACETIME_SUPERCOMPUTER.asItem(), """
                Executes commands stored in the supercomputer on a timer
                Each execution consumes power from the grid
                Acts as the computational core of the Celestial Forging Anvil"""
        );
        SHIFT.put(
            ModBlocks.MASS_ENERGY_INVERTER.asItem(), """
                Injects 5 mass per tick into adjacent Space Overcompressors
                Doubles the mass gained by adjacent Space Overcompressors from anvil mass injection"""
        );
        SHIFT.put(
            ModBlocks.CELESTIAL_FORGING_ANVIL_PORTAL.asItem(), """
                Teleports players and entities between two portals
                Connected Celestial Forging Anvil wormholes can link two portals facing the same direction"""
        );
        SHIFT.put(
            ModBlocks.DRAIN.asItem(), """
                Draining: when more than 1 B is stored and there is space below, outputs fluid downward and fills the entire space from the bottom up
                Suction: when less than 3 B is stored and the same fluid is above, draws fluid from above and can empty the entire space above
                Does not interact with fluid at the same height; fluid can be stored for free only when it forms an infinite source"""
        );
        SHIFT.put(ModBlocks.REDSTONE_WIRE.asItem(), """
                It can be attached to any full face of a block,
                only inputs and outputs redstone signals at its breaks (ends)
                The redstone signal level does not decay within the wire,
                and the wire will not output the signal received from redstone dust back to redstone dust
                Right-click an existing wire to change its direction""");
        SHIFT.put(ModBlocks.AUTO_ENCHANTING_TABLE.asItem(), """
                Automatically enchants items with power from the grid
                Random mode: consumes experience fluid, enchants based on nearby bookshelf level (16 kW)
                Primer mode: uses a primer to list selectable enchantments, consumes experience fluid (64 kW)
                Liquid enchantment mode: consumes enchanted liquid enchantment to apply a chosen enchantment up to level 15 (64 kW plus 64 kW per existing enchantment)
                Redstone signal pauses the machine""");
        SHIFT.put(
            ModBlocks.TRADING_STATION.asItem(), """
                Can be set to trade with players or villagers
                Villagers will actively trade with stations that have valid offers and fair prices
                Has 12 slots for temporary item storage"""
        );
        SHIFT.put(
            ModBlocks.CHUTE.asItem(), """
                Can set item filter
                Shift and left‑click a slot to disable it
                Use scroll wheel to set slot stack limit
                Multiple Chutes connected turn into a Simple Chute
                Simple Chute has only one slot and cannot be locked by redstone"""
        );
        SHIFT.put(
            ModBlocks.MAGNETIC_CHUTE.asItem(), """
                The output items will be launched with speed
                Can set item filter
                Shift and left‑click a slot to disable it
                Use scroll wheel to set slot stack limit
                Multiple Magnetic Chutes connected turn into a Simple Magnetic Chute
                Simple Magnetic Chute has only one slot and cannot be locked by redstone"""
        );
        SHIFT.put(
            ModBlocks.GIANT_ANVIL.asItem(), """
                Can be used for Multiblock Crafting and Multiblock Conversion
                Works with ground blocks to trigger Ground Shaking
                Can also perform crafting actions with a Giant Cauldron"""
        );
        SHIFT.put(
            ModBlocks.LARGE_CAULDRON.asItem(), """
                Can perform all cauldron crafting recipes
                Compatible with multiple processing types via different base blocks
                Can store multiple fluids"""
        );
        SHIFT.put(
            ModBlocks.FLUID_TANK.asItem(), """
                Stores 16B of fluids
                Menger Sponges expand it to 12800B and make it infinite when full
                Can interact with Dispensers for fluid transfer"""
        );
        SHIFT.put(
            ModBlocks.LARGE_FLUID_TANK.asItem(), """
                Stores 512B shared by any number of fluids
                Menger Sponges unlock unlimited total storage
                Can store multiple fluids, each type of fluid that reaches 12800B will be converted to infinite
                Can interact with Dispensers for fluid transfer"""
        );
        SHIFT.put(
            ModItems.CAPACITOR.asItem(), """
                Can be consumed automatically
                or can be taken with a left-click in the inventory and then used by right-clicking on an electrical appliance to actively charge it"""
        );
        SHIFT.put(
            ModItems.SUPER_CAPACITOR.asItem(), """
                Can be consumed automatically
                or can be taken with a left-click in the inventory and then used by right-clicking on an electrical appliance to actively charge it"""
        );
        SHIFT.put(ModBlocks.CREATIVE_LASER.asItem(), "Adjustable laser level, lens and gamma mode, can be turned off by redstone");

        Map<Item, String> allTooltips = Maps.newHashMap();
        allTooltips.putAll(NORMAL);
        allTooltips.putAll(SHIFT);
        NEED_TOOLTIP_ITEMS = Collections.unmodifiableMap(allTooltips);
    }

    public static final Map<Item, String> NEED_TOOLTIP_ITEMS;

    /**
     * 为模组物品添加工具提示
     *
     * @param stack   需要添加工具提示的物品堆叠
     * @param tooltip 提示内容
     */
    public static void addTooltip(ItemStack stack, List<Component> tooltip) {
        final Item item = stack.getItem();
        final int initialTooltipSize = tooltip.size();
        if (stack.has(ModComponents.STORED_ENERGY)) {
            propertyTooltip(
                "stored_energy",
                tooltip,
                ChatFormatting.GRAY,
                UnitUtil.energyUnit(stack.getOrDefault(ModComponents.STORED_ENERGY, 0), Screen.hasShiftDown())
            );
        }
        if (stack.has(ModComponents.MULTIPHASE)) {
            propertyTooltip(
                "multiphase",
                tooltip,
                0xDD91FA,
                ModKeyMappings.SWITCH_PHASE.get().getKey().getDisplayName()
            );
        }
        if (stack.has(ModComponents.DEVOUR_PROTECT_CONTAINERS)) {
            boolean protectContainers = stack.getOrDefault(ModComponents.DEVOUR_PROTECT_CONTAINERS, false);
            propertyTooltip(
                "protect_containers",
                tooltip,
                0xDD91FA,
                Component.translatable(
                    protectContainers
                        ? "tooltip.anvilcraft.property.protect_containers.on"
                        : "tooltip.anvilcraft.property.protect_containers.off"
                ).withStyle(protectContainers ? ChatFormatting.GREEN : ChatFormatting.RED)
            );
        }
        if (stack.has(ModComponents.PROVIDENCE)) {
            if (Screen.hasShiftDown()) {
                propertyTooltip(
                    "providence.shifting", tooltip, 0xFFCB62, ComponentUtils.formatList(
                        List.of(
                            Component.translatable("enchantment.minecraft.fortune"),
                            Component.translatable("enchantment.minecraft.looting"),
                            Component.translatable("enchantment.anvilcraft.beheading"),
                            Component.translatable("enchantment.minecraft.thorns"),
                            Component.translatable("enchantment.minecraft.luck_of_the_sea")
                        ), ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR
                    )
                );
            } else {
                propertyTooltip(
                    "providence", tooltip, 0xFFCB62,
                    Minecraft.getInstance().options.keyShift.getKey().getDisplayName()
                );
            }
        }
        if (stack.has(ModComponents.ETERNAL)) {
            propertyTooltip("eternal", tooltip, 0xD3C5F6);
        }
        if (stack.has(ModComponents.FEROCIOUS)) {
            propertyTooltip("ferocious", tooltip, 0xDD1212);
        }
        if (stack.has(ModComponents.MERCILESS)) {
            propertyTooltip("merciless", tooltip, 0xB4F0F6);
        }
        if (stack.has(ModComponents.FIRE_REFORGING)) {
            propertyTooltip("fire_reforging", tooltip, ChatFormatting.GOLD);
        }
        if (SHIFT.containsKey(item) || item == ModBlocks.LENS.asItem()) {
            if (Screen.hasShiftDown()) {
                if (item == ModBlocks.LENS.asItem()) {
                    tooltip.add(1, Component.literal("Ember (yellow): drops smelted results directly; Core Shard Ore and Void Stone have no smelted form and remain unchanged").withColor(0xFFAA00));
                    tooltip.add(1, Component.literal("Frost (light blue): drops Experience Gems instead of ores, 10% chance per mined block; Core Shard Ore and Void Stone also convert to EXP").withColor(0xB4F0F6));
                    tooltip.add(1, Component.literal("Royal (cyan): drops raw ore blocks instead of raw materials, including Core Shard Ore and Void Stone").withColor(0x00FFBF));
                } else {
                    addShiftTooltip(tooltip, item);
                }
            } else {
                if (NORMAL.containsKey(item)) {
                    addNormalTooltip(tooltip, item);
                }
                int anvilCraftLines = tooltip.size() - initialTooltipSize;
                tooltip.add(
                    1 + anvilCraftLines,
                    Component.translatable(
                        "tooltip.anvilcraft.press_key",
                        Component.literal("[Shift]").withStyle(ChatFormatting.WHITE)
                    ).withStyle(ChatFormatting.DARK_GRAY)
                );
            }
        } else if (NORMAL.containsKey(item)) {
            addNormalTooltip(tooltip, item);
        }
        if (stack.is(ModBlocks.POWER_CONVERTER_SMALL.asItem())) {
            tooltip.add(
                1,
                Component.translatable("tooltip.anvilcraft.item.power_converter", PowerConverterSmallBlock.INPUT_TIME)
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        if (stack.is(ModBlocks.POWER_CONVERTER_MIDDLE.asItem())) {
            tooltip.add(
                1,
                Component.translatable("tooltip.anvilcraft.item.power_converter", PowerConverterMiddleBlock.INPUT_TIME)
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        if (stack.is(ModBlocks.POWER_CONVERTER_BIG.asItem())) {
            tooltip.add(
                1,
                Component.translatable("tooltip.anvilcraft.item.power_converter", PowerConverterBigBlock.INPUT_TIME)
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        if (stack.is(ModBlocks.POWER_CONVERTER_SUPER_BIG.asItem())) {
            tooltip.add(
                1,
                Component.translatable("tooltip.anvilcraft.item.power_converter", PowerConverterSuperBigBlock.INPUT_TIME)
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        if (stack.is(ModBlocks.POWER_CONVERTER_EXTREMELY_BIG.asItem())) {
            tooltip.add(
                1,
                Component.translatable("tooltip.anvilcraft.item.power_converter", PowerConverterExtremelyBigBlock.INPUT_TIME)
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        if (stack.is(ModItemTags.REINFORCED_CONCRETE)) {
            tooltip.add(
                1,
                Component.translatable("tooltip.anvilcraft.item.reinforced_concrete")
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        if (stack.is(ModItems.AMULET_BOX.asItem())) {
            BoxContents contents = stack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.item.amulet_box.fullness", contents.usage(), AmuletBoxItem.CAPACITY
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    public static Optional<TooltipComponent> getStorageTooltip(ItemStack stack) {
        StorageRef ref = stack.get(ModComponents.STORAGE);
        if (ref == null || ref.id().isEmpty()) {
            return Optional.empty();
        }
        UUID storageId = ref.id().get();
        StorageServerStub.StorageUsage usage = STORAGE_USAGE.get(storageId);
        if (
            usage == null
            || System.currentTimeMillis() - STORAGE_USAGE_TIMES.getOrDefault(storageId, 0L)
            > STORAGE_USAGE_REFRESH_INTERVAL
        ) {
            ItemTooltipManager.requestStorageUsage(storageId);
        }
        if (usage == null || usage.typeLimit() <= 0 || usage.typeLimit() == Integer.MAX_VALUE) {
            return Optional.empty();
        }
        return Optional.of(new StorageTooltip(usage.usedTypes(), usage.typeLimit(), usage.types()));
    }

    private static void requestStorageUsage(UUID storageId) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        if (!STORAGE_USAGE_PENDING.add(storageId)) {
            return;
        }
        StorageClientStub.loadUsage(storageId).whenComplete((usage, error) -> {
            STORAGE_USAGE_PENDING.remove(storageId);
            if (error != null || usage == null || usage.typeLimit() <= 0) {
                return;
            }
            STORAGE_USAGE.put(storageId, usage);
            STORAGE_USAGE_TIMES.put(storageId, System.currentTimeMillis());
        });
    }

    /**
     * 添加翻译后的tooltip，自动将 \n 拆分为多行
     */
    private static void addTranslatedTooltip(List<Component> tooltip, String key) {
        String text = I18n.get(key);
        String[] lines = text.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            tooltip.add(1, Component.literal(lines[i]).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addNormalTooltip(List<Component> tooltip, Item item) {
        addTranslatedTooltip(tooltip, getTranslationKey(item));
    }

    private static void addShiftTooltip(List<Component> tooltip, Item item) {
        if (item == ModItems.PILL_BOX.asItem()) {
            tooltip.add(
                1, Component.translatable(
                getTranslationKeyShift(item),
                Component.keybind("key.anvilcraft.use_pill_box")
            ).withStyle(ChatFormatting.GRAY));
            return;
        }
        addTranslatedTooltip(tooltip, getTranslationKeyShift(item));
    }

    public static String getTranslationKey(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return "tooltip.%s.item.%s".formatted(key.getNamespace(), key.getPath());
    }

    public static String getTranslationKeyShift(Item item) {
        return getTranslationKey(item) + ".shift";
    }

    public static Map<Item, String> getNormalMap() {
        return Collections.unmodifiableMap(NORMAL);
    }

    public static Map<Item, String> getShiftMap() {
        return Collections.unmodifiableMap(SHIFT);
    }

    private static void propertyTooltip(String propertyName, List<Component> tooltip, ChatFormatting color, Object... args) {
        int i = 0;
        for (int j = 0; j < tooltip.size(); j++) {
            if (tooltip.get(j).getContents() instanceof TranslatableContents t && t.getKey().contains("enchantment")
                && ListUtil.safelyGet(tooltip, j + 1)
                    .flatMap(tooltipI -> Util.castSafely(tooltipI.getContents(), TranslatableContents.class))
                    .map(TranslatableContents::getKey)
                    .filter(key -> key.contains("enchantment"))
                    .isEmpty()
            ) {
                i = j;
                break;
            }
        }
        tooltip.add(
            1 + i,
            Component.translatable("tooltip.anvilcraft.property.%s".formatted(propertyName), args).withStyle(color)
        );
    }

    private static void propertyTooltip(String propertyName, List<Component> tooltip, int color) {
        int i = 0;
        for (int j = 0; j < tooltip.size(); j++) {
            if (tooltip.get(j).getContents() instanceof TranslatableContents t && t.getKey().contains("enchantment")
                && ListUtil.safelyGet(tooltip, j + 1)
                    .flatMap(tooltipI -> Util.castSafely(tooltipI.getContents(), TranslatableContents.class))
                    .map(TranslatableContents::getKey)
                    .filter(key -> key.contains("enchantment"))
                    .isEmpty()
            ) {
                i = j;
                break;
            }
        }
        tooltip.add(
            1 + i,
            Component.translatable("tooltip.anvilcraft.property.%s".formatted(propertyName)).withColor(color)
        );
    }

    private static void propertyTooltip(String propertyName, List<Component> tooltip, int color, Object... args) {
        int i = 0;
        for (int j = 0; j < tooltip.size(); j++) {
            if (tooltip.get(j).getContents() instanceof TranslatableContents t && t.getKey().contains("enchantment")
                && ListUtil.safelyGet(tooltip, j + 1)
                    .flatMap(tooltipI -> Util.castSafely(tooltipI.getContents(), TranslatableContents.class))
                    .map(TranslatableContents::getKey)
                    .filter(key -> key.contains("enchantment"))
                    .isEmpty()
            ) {
                i = j;
                break;
            }
        }
        tooltip.add(
            1 + i,
            Component.translatable("tooltip.anvilcraft.property.%s".formatted(propertyName), args).withColor(color)
        );
    }
}
