package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

@SuppressWarnings("unused")
public class ModItemTags {
    public static final TagKey<Item> RESIN = ModItemTags.bindC("resin");
    public static final TagKey<Item> WHEAT_FLOUR = ModItemTags.bindC("flour/wheat");
    public static final TagKey<Item> WHEAT_DOUGH = ModItemTags.bindC("dough/wheat");
    public static final TagKey<Item> CREAM = ModItemTags.bindC("foods/cream");
    public static final TagKey<Item> FLOUR = ModItemTags.bindC("foods/flour");
    public static final TagKey<Item> DOUGH = ModItemTags.bindC("foods/dough");
    public static final TagKey<Item> RAW_MUTTON = ModItemTags.bindC("foods/raw_mutton");
    public static final TagKey<Item> RAW_BEEF = ModItemTags.bindC("foods/raw_beef");
    public static final TagKey<Item> RAW_CHICKEN = ModItemTags.bindC("foods/raw_chicken");
    public static final TagKey<Item> RAW_PORKCHOP = ModItemTags.bindC("foods/raw_porkchop");
    public static final TagKey<Item> RAW_RABBIT = ModItemTags.bindC("foods/raw_rabbit");

    public static final TagKey<Item> PLATES = ModItemTags.bindC("plates");
    public static final TagKey<Item> GOLD_PLATES = ModItemTags.bindC("plates/gold");
    public static final TagKey<Item> IRON_PLATES = ModItemTags.bindC("plates/iron");
    public static final TagKey<Item> COPPER_PLATES = ModItemTags.bindC("plates/copper");
    public static final TagKey<Item> TUNGSTEN_PLATES = ModItemTags.bindC("plates/tungsten");
    public static final TagKey<Item> TITANIUM_PLATES = ModItemTags.bindC("plates/titanium");
    public static final TagKey<Item> ZINC_PLATES = ModItemTags.bindC("plates/zinc");
    public static final TagKey<Item> TIN_PLATES = ModItemTags.bindC("plates/tin");
    public static final TagKey<Item> LEAD_PLATES = ModItemTags.bindC("plates/lead");
    public static final TagKey<Item> SILVER_PLATES = ModItemTags.bindC("plates/silver");
    public static final TagKey<Item> URANIUM_PLATES = ModItemTags.bindC("plates/uranium");
    public static final TagKey<Item> BRONZE_PLATES = ModItemTags.bindC("plates/bronze");
    public static final TagKey<Item> BRASS_PLATES = ModItemTags.bindC("plates/brass");

    public static final TagKey<Item> STORAGE_BLOCKS_TUNGSTEN = ModItemTags.bindC("storage_blocks/tungsten");
    public static final TagKey<Item> STORAGE_BLOCKS_TITANIUM = ModItemTags.bindC("storage_blocks/titanium");
    public static final TagKey<Item> STORAGE_BLOCKS_ZINC = ModItemTags.bindC("storage_blocks/zinc");
    public static final TagKey<Item> STORAGE_BLOCKS_TIN = ModItemTags.bindC("storage_blocks/tin");
    public static final TagKey<Item> STORAGE_BLOCKS_LEAD = ModItemTags.bindC("storage_blocks/lead");
    public static final TagKey<Item> STORAGE_BLOCKS_SILVER = ModItemTags.bindC("storage_blocks/silver");
    public static final TagKey<Item> STORAGE_BLOCKS_URANIUM = ModItemTags.bindC("storage_blocks/uranium");
    public static final TagKey<Item> STORAGE_BLOCKS_PLUTONIUM = ModItemTags.bindC("storage_blocks/plutonium");
    public static final TagKey<Item> STORAGE_BLOCKS_BRONZE = ModItemTags.bindC("storage_blocks/bronze");
    public static final TagKey<Item> STORAGE_BLOCKS_BRASS = ModItemTags.bindC("storage_blocks/brass");
    public static final TagKey<Item> STORAGE_BLOCKS_VOID_MATTER = ModItemTags.bindC("storage_blocks/void_matter");
    public static final TagKey<Item> STORAGE_BLOCKS_EARTH_CORE_SHARD = ModItemTags.bindC("storage_blocks/earth_core_shard");
    public static final TagKey<Item> STORAGE_BLOCKS_MULTIPHASE_MATTER = ModItemTags.bindC("storage_blocks/multiphase_matter");
    public static final TagKey<Item> STORAGE_BLOCKS_MAGNET = ModItemTags.bindC("storage_blocks/magnet");
    public static final TagKey<Item> STORAGE_BLOCKS_TOPAZ = ModItemTags.bindC("storage_blocks/topaz");
    public static final TagKey<Item> STORAGE_BLOCKS_SAPPHIRE = ModItemTags.bindC("storage_blocks/sapphire");
    public static final TagKey<Item> STORAGE_BLOCKS_RUBY = ModItemTags.bindC("storage_blocks/ruby");
    public static final TagKey<Item> STORAGE_BLOCKS_EXP_GEM = ModItemTags.bindC("storage_blocks/exp_gem");
    public static final TagKey<Item> STORAGE_BLOCKS_AMBER = ModItemTags.bindC("storage_blocks/amber");
    public static final TagKey<Item> STORAGE_BLOCKS_RESIN = ModItemTags.bindC("storage_blocks/resin");
    public static final TagKey<Item> STORAGE_BLOCKS_TRANSCENDIUM = ModItemTags.bindC("storage_blocks/transcendium");
    public static final TagKey<Item> STORAGE_BLOCKS_FROST_METAL = ModItemTags.bindC("storage_blocks/frost_metal");

    public static final TagKey<Item> STORAGE_BLOCKS_SUGAR = ModItemTags.bindC("storage_blocks/sugar");
    public static final TagKey<Item> STORAGE_BLOCKS_GUNPOWDER = ModItemTags.bindC("storage_blocks/gunpowder");
    public static final TagKey<Item> STORAGE_BLOCKS_ROTTEN_FLESH = ModItemTags.bindC("storage_blocks/rotten_flesh");
    public static final TagKey<Item> STORAGE_BLOCKS_FLINT = ModItemTags.bindC("storage_blocks/flint");

    public static final TagKey<Item> GEMS_TOPAZ = ModItemTags.bindC("gems/topaz");
    public static final TagKey<Item> GEMS_SAPPHIRE = ModItemTags.bindC("gems/sapphire");
    public static final TagKey<Item> GEMS_RUBY = ModItemTags.bindC("gems/ruby");
    public static final TagKey<Item> GEMS_AMBER = ModItemTags.bindC("gems/amber");

    public static final TagKey<Item> TUNGSTEN_INGOTS = ModItemTags.bindC("ingots/tungsten");
    public static final TagKey<Item> TITANIUM_INGOTS = ModItemTags.bindC("ingots/titanium");
    public static final TagKey<Item> ZINC_INGOTS = ModItemTags.bindC("ingots/zinc");
    public static final TagKey<Item> TIN_INGOTS = ModItemTags.bindC("ingots/tin");
    public static final TagKey<Item> LEAD_INGOTS = ModItemTags.bindC("ingots/lead");
    public static final TagKey<Item> SILVER_INGOTS = ModItemTags.bindC("ingots/silver");
    public static final TagKey<Item> URANIUM_INGOTS = ModItemTags.bindC("ingots/uranium");
    public static final TagKey<Item> PLUTONIUM_INGOTS = ModItemTags.bindC("ingots/plutonium");
    public static final TagKey<Item> BRONZE_INGOTS = ModItemTags.bindC("ingots/bronze");
    public static final TagKey<Item> BRASS_INGOTS = ModItemTags.bindC("ingots/brass");
    public static final TagKey<Item> MAGNET_INGOTS = ModItemTags.bindC("ingots/magnet");
    public static final TagKey<Item> TRANSCENDIUM_INGOTS = ModItemTags.bindC("ingots/transcendium");
    public static final TagKey<Item> FROST_METAL_INGOTS = ModItemTags.bindC("ingots/frost_metal");

    public static final TagKey<Item> TUNGSTEN_NUGGETS = ModItemTags.bindC("nuggets/tungsten");
    public static final TagKey<Item> TITANIUM_NUGGETS = ModItemTags.bindC("nuggets/titanium");
    public static final TagKey<Item> ZINC_NUGGETS = ModItemTags.bindC("nuggets/zinc");
    public static final TagKey<Item> TIN_NUGGETS = ModItemTags.bindC("nuggets/tin");
    public static final TagKey<Item> LEAD_NUGGETS = ModItemTags.bindC("nuggets/lead");
    public static final TagKey<Item> SILVER_NUGGETS = ModItemTags.bindC("nuggets/silver");
    public static final TagKey<Item> URANIUM_NUGGETS = ModItemTags.bindC("nuggets/uranium");
    public static final TagKey<Item> PLUTONIUM_NUGGETS = ModItemTags.bindC("nuggets/plutonium");
    public static final TagKey<Item> BRONZE_NUGGETS = ModItemTags.bindC("nuggets/bronze");
    public static final TagKey<Item> BRASS_NUGGETS = ModItemTags.bindC("nuggets/brass");
    public static final TagKey<Item> COPPER_NUGGETS = ModItemTags.bindC("nuggets/copper");
    public static final TagKey<Item> NETHERITE_NUGGETS = ModItemTags.bindC("nuggets/netherite");
    public static final TagKey<Item> TRANSCENDIUM_NUGGETS = ModItemTags.bindC("nuggets/transcendium");
    public static final TagKey<Item> FROST_METAL_NUGGETS = ModItemTags.bindC("nuggets/frost_metal");

    public static final TagKey<Item> ZINC_ORES = ModItemTags.bindC("ores/zinc");
    public static final TagKey<Item> TIN_ORES = ModItemTags.bindC("ores/tin");
    public static final TagKey<Item> TITANIUM_ORES = ModItemTags.bindC("ores/titanium");
    public static final TagKey<Item> TUNGSTEN_ORES = ModItemTags.bindC("ores/tungsten");
    public static final TagKey<Item> LEAD_ORES = ModItemTags.bindC("ores/lead");
    public static final TagKey<Item> SILVER_ORES = ModItemTags.bindC("ores/silver");
    public static final TagKey<Item> URANIUM_ORES = ModItemTags.bindC("ores/uranium");
    public static final TagKey<Item> VOID_MATTER_ORES = ModItemTags.bindC("ores/void_matter");
    public static final TagKey<Item> EARTH_CORE_SHARD_ORES = ModItemTags.bindC("ores/earth_core_shard");

    public static final TagKey<Item> STORAGE_BLOCKS_RAW_TUNGSTEN = ModItemTags.bindC("storage_blocks/raw_tungsten");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_TITANIUM = ModItemTags.bindC("storage_blocks/raw_titanium");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_ZINC = ModItemTags.bindC("storage_blocks/raw_zinc");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_TIN = ModItemTags.bindC("storage_blocks/raw_tin");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_LEAD = ModItemTags.bindC("storage_blocks/raw_lead");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_SILVER = ModItemTags.bindC("storage_blocks/raw_silver");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_URANIUM = ModItemTags.bindC("storage_blocks/raw_uranium");

    public static final TagKey<Item> RAW_ZINC = ModItemTags.bindC("raw_materials/zinc");
    public static final TagKey<Item> RAW_TIN = ModItemTags.bindC("raw_materials/tin");
    public static final TagKey<Item> RAW_TITANIUM = ModItemTags.bindC("raw_materials/titanium");
    public static final TagKey<Item> RAW_TUNGSTEN = ModItemTags.bindC("raw_materials/tungsten");
    public static final TagKey<Item> RAW_LEAD = ModItemTags.bindC("raw_materials/lead");
    public static final TagKey<Item> RAW_SILVER = ModItemTags.bindC("raw_materials/silver");
    public static final TagKey<Item> RAW_URANIUM = ModItemTags.bindC("raw_materials/uranium");

    public static final TagKey<Item> EXP_BUCKETS = ModItemTags.bindC("buckets/exp_fluid");
    public static final TagKey<Item> OIL_BUCKETS = ModItemTags.bindC("buckets/oil");
    public static final TagKey<Item> CEMENT_BUCKETS = ModItemTags.bindC("buckets/cement");

    public static final TagKey<Item> AMETHYST_TOOL_MATERIALS = ModItemTags.bindC("amethyst_tool_materials");
    public static final TagKey<Item> ROYAL_STEEL_TOOL_MATERIALS = ModItemTags.bindC("royal_steel_tool_materials");
    public static final TagKey<Item> FROST_METAL_TOOL_MATERIALS = ModItemTags.bindC("frost_metal_tool_materials");
    public static final TagKey<Item> EMBER_METAL_TOOL_MATERIALS = ModItemTags.bindC("ember_metal_tool_materials");
    public static final TagKey<Item> TRANSCENDIUM_TOOL_MATERIALS = ModItemTags.bindC("transcendium_tool_materials");

    public static final TagKey<Item> ROYAL_STEEL_PICKAXE_BASE = ModItemTags.bind("royal_steel_pickaxe_base");
    public static final TagKey<Item> ROYAL_STEEL_AXE_BASE = ModItemTags.bind("royal_steel_axe_base");
    public static final TagKey<Item> ROYAL_STEEL_HOE_BASE = ModItemTags.bind("royal_steel_hoe_base");
    public static final TagKey<Item> ROYAL_STEEL_SHOVEL_BASE = ModItemTags.bind("royal_steel_shovel_base");
    public static final TagKey<Item> ROYAL_STEEL_SWORD_BASE = ModItemTags.bind("royal_steel_sword_base");
    public static final TagKey<Item> FROST_METAL_PICKAXE_BASE = ModItemTags.bind("frost_metal_pickaxe_base");
    public static final TagKey<Item> FROST_METAL_AXE_BASE = ModItemTags.bind("frost_metal_axe_base");
    public static final TagKey<Item> FROST_METAL_HOE_BASE = ModItemTags.bind("frost_metal_hoe_base");
    public static final TagKey<Item> FROST_METAL_SHOVEL_BASE = ModItemTags.bind("frost_metal_shovel_base");
    public static final TagKey<Item> FROST_METAL_SWORD_BASE = ModItemTags.bind("frost_metal_sword_base");
    public static final TagKey<Item> EMBER_METAL_PICKAXE_BASE = ModItemTags.bind("ember_metal_pickaxe_base");
    public static final TagKey<Item> EMBER_METAL_AXE_BASE = ModItemTags.bind("ember_metal_axe_base");
    public static final TagKey<Item> EMBER_METAL_HOE_BASE = ModItemTags.bind("ember_metal_hoe_base");
    public static final TagKey<Item> EMBER_METAL_SHOVEL_BASE = ModItemTags.bind("ember_metal_shovel_base");
    public static final TagKey<Item> EMBER_METAL_SWORD_BASE = ModItemTags.bind("ember_metal_sword_base");
    public static final TagKey<Item> CAPACITOR = ModItemTags.bind("capacitor");
    public static final TagKey<Item> GEMS = ModItemTags.bind("gems");
    public static final TagKey<Item> GEM_BLOCKS = ModItemTags.bind("gem_blocks");
    public static final TagKey<Item> DEAD_CORALS = ModItemTags.bind("dead_corals");
    public static final TagKey<Item> DEAD_CORAL_BLOCKS = ModItemTags.bind("dead_coral_blocks");
    public static final TagKey<Item> VOID_RESISTANT = ModItemTags.bind("void_resistant");
    public static final TagKey<Item> REINFORCED_CONCRETE = ModItemTags.bind("reinforced_concrete");
    public static final TagKey<Item> SEEDS_PACK_CONTENT = ModItemTags.bind("seeds_pack_content");
    public static final TagKey<Item> FIRE_STARTER = ModItemTags.bind("fire_starter");
    public static final TagKey<Item> UNBROKEN_FIRE_STARTER = ModItemTags.bind("unbroken_fire_starter");
    public static final TagKey<Item> NETHERITE_BLOCK = ModItemTags.bind("netherite_block");
    public static final TagKey<Item> EXPLOSION_PROOF = ModItemTags.bind("explosion_proof");
    public static final TagKey<Item> AMULET = ModItemTags.bind("amulet");
    public static final TagKey<Item> ANVIL_HAMMER = ModItemTags.bind("tools/anvil_hammer");
    public static final TagKey<Item> TEMPLATES = ModItemTags.bind("templates");
    public static final TagKey<Item> MULTIPLE_TO_ONE_SMITHING_TEMPLATES = ModItemTags.bind("multiple_to_one_smithing_templates");
    public static final TagKey<Item> DRAGON_ROD = ModItemTags.bind("tools/dragon_rod");
    public static final TagKey<Item> HEAVY_HALBERD = ModItemTags.bind("tools/heavy_halberd");
    public static final TagKey<Item> RESONATOR = ModItemTags.bind("tools/resonator");
    public static final TagKey<Item> DISINTEGRATION_SUPPORTED = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("minecraft", "enchantable/anvilcraft_disintegration")
    );
    public static final TagKey<Item> SMELTING_SUPPORTED = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("minecraft", "enchantable/anvilcraft_smelting")
    );
    public static final TagKey<Item> UNCHARGED_NEUTRONIUM_INGOTS = ModItemTags.bind("uncharged_neutronium_ingots");
    public static final TagKey<Item> HEATABLE_BLOCKS = ModItemTags.bind("heatable_blocks");
    public static final TagKey<Item> LEVITATIONALS = ModItemTags.bind("levitationals");
    public static final TagKey<Item> RADIATIONS = ModItemTags.bind("radiations");
    public static final TagKey<Item> DISALLOW_HAND_INSERT_INTO_TANK = ModItemTags.bind("disallow_hand_insert_into_tank");

    public static final TagKey<Item> COMPRESS_ITEM = ModItemTags.bind("compress_item");
    public static final TagKey<Item> SUPER_HEATING_BOOST_PRODUCTION = ModItemTags.bind("super_heating_boost_production");

    public static final TagKey<Item> CURIOS_HEAD = ModItemTags.bindCurios("head");
    public static final TagKey<Item> CURIOS_IONOCRAFT_BACKPACK = ModItemTags.bindCurios("ionocraft_backpack");
    public static final TagKey<Item> CURIOS_CHARM = ModItemTags.bindCurios("charm");

    public static final TagKey<Item> TOTEM = ModItemTags.bind("totem");

    public static final Object2ObjectMap<Color, TagKey<Item>> DYED_COLORS = ModItemTags.initDyedTags();

    public static TagKey<Item> bindC(String id) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", id));
    }

    public static TagKey<Item> bindCurios(String id) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("curios", id));
    }

    public static TagKey<Item> bind(String id) {
        return TagKey.create(Registries.ITEM, AnvilCraft.of(id));
    }

    public static Object2ObjectMap<Color, TagKey<Item>> initDyedTags() {
        Object2ObjectMap<Color, TagKey<Item>> map = new Object2ObjectOpenHashMap<>();
        for (Color color : Color.values()) {
            map.put(color, ModItemTags.bindC("dyed/" + color));
        }
        return map;
    }

}
