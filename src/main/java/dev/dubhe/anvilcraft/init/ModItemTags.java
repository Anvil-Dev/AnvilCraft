package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ModItemTags {
    public static final TagKey<Item> FLOUR = bindC("flour");
    public static final TagKey<Item> WHEAT_FLOUR = bindC("flour/wheat");
    public static final TagKey<Item> DOUGH = bindC("dough");
    public static final TagKey<Item> WHEAT_DOUGH = bindC("dough/wheat");

    public static final TagKey<Item> PLATES = bindC("plates");
    public static final TagKey<Item> GOLD_PLATES = bindC("plates/gold");
    public static final TagKey<Item> IRON_PLATES = bindC("plates/iron");
    public static final TagKey<Item> COPPER_PLATES = bindC("plates/copper");
    public static final TagKey<Item> TUNGSTEN_PLATES = bindC("plates/tungsten");
    public static final TagKey<Item> TITANIUM_PLATES = bindC("plates/titanium");
    public static final TagKey<Item> ZINC_PLATES = bindC("plates/zinc");
    public static final TagKey<Item> TIN_PLATES = bindC("plates/tin");
    public static final TagKey<Item> LEAD_PLATES = bindC("plates/lead");
    public static final TagKey<Item> SILVER_PLATES = bindC("plates/silver");
    public static final TagKey<Item> URANIUM_PLATES = bindC("plates/uranium");
    public static final TagKey<Item> BRONZE_PLATES = bindC("plates/bronze");
    public static final TagKey<Item> BRASS_PLATES = bindC("plates/brass");

    public static final TagKey<Item> STORAGE_BLOCKS_TUNGSTEN = bindC("storage_blocks/tungsten");
    public static final TagKey<Item> STORAGE_BLOCKS_TITANIUM = bindC("storage_blocks/titanium");
    public static final TagKey<Item> STORAGE_BLOCKS_ZINC = bindC("storage_blocks/zinc");
    public static final TagKey<Item> STORAGE_BLOCKS_TIN = bindC("storage_blocks/tin");
    public static final TagKey<Item> STORAGE_BLOCKS_LEAD = bindC("storage_blocks/lead");
    public static final TagKey<Item> STORAGE_BLOCKS_SILVER = bindC("storage_blocks/silver");
    public static final TagKey<Item> STORAGE_BLOCKS_URANIUM = bindC("storage_blocks/uranium");
    public static final TagKey<Item> STORAGE_BLOCKS_BRONZE = bindC("storage_blocks/bronze");
    public static final TagKey<Item> STORAGE_BLOCKS_BRASS = bindC("storage_blocks/brass");
    public static final TagKey<Item> STORAGE_BLOCKS_VOID_MATTER = bindC("storage_blocks/void_matter");
    public static final TagKey<Item> STORAGE_BLOCKS_EARTH_CORE_SHARD = bindC("storage_blocks/earth_core_shard");
    public static final TagKey<Item> STORAGE_BLOCKS_MAGNET = bindC("storage_blocks/magnet");
    public static final TagKey<Item> STORAGE_BLOCKS_TOPAZ = bindC("storage_blocks/topaz");
    public static final TagKey<Item> STORAGE_BLOCKS_SAPPHIRE = bindC("storage_blocks/sapphire");
    public static final TagKey<Item> STORAGE_BLOCKS_RUBY = bindC("storage_blocks/ruby");
    public static final TagKey<Item> STORAGE_BLOCKS_AMBER = bindC("storage_blocks/amber");
    public static final TagKey<Item> STORAGE_BLOCKS_RESIN = bindC("storage_blocks/resin");

    public static final TagKey<Item> GEMS_TOPAZ = bindC("gems/topaz");
    public static final TagKey<Item> GEMS_SAPPHIRE = bindC("gems/sapphire");
    public static final TagKey<Item> GEMS_RUBY = bindC("gems/ruby");
    public static final TagKey<Item> GEMS_AMBER = bindC("gems/amber");

    public static final TagKey<Item> TUNGSTEN_INGOTS = bindC("ingots/tungsten");
    public static final TagKey<Item> TITANIUM_INGOTS = bindC("ingots/titanium");
    public static final TagKey<Item> ZINC_INGOTS = bindC("ingots/zinc");
    public static final TagKey<Item> TIN_INGOTS = bindC("ingots/tin");
    public static final TagKey<Item> LEAD_INGOTS = bindC("ingots/lead");
    public static final TagKey<Item> SILVER_INGOTS = bindC("ingots/silver");
    public static final TagKey<Item> URANIUM_INGOTS = bindC("ingots/uranium");
    public static final TagKey<Item> BRONZE_INGOTS = bindC("ingots/bronze");
    public static final TagKey<Item> BRASS_INGOTS = bindC("ingots/brass");
    public static final TagKey<Item> MAGNET_INGOTS = bindC("ingots/magnet");

    public static final TagKey<Item> TUNGSTEN_NUGGETS = bindC("nuggets/tungsten");
    public static final TagKey<Item> TITANIUM_NUGGETS = bindC("nuggets/titanium");
    public static final TagKey<Item> ZINC_NUGGETS = bindC("nuggets/zinc");
    public static final TagKey<Item> TIN_NUGGETS = bindC("nuggets/tin");
    public static final TagKey<Item> LEAD_NUGGETS = bindC("nuggets/lead");
    public static final TagKey<Item> SILVER_NUGGETS = bindC("nuggets/silver");
    public static final TagKey<Item> URANIUM_NUGGETS = bindC("nuggets/uranium");
    public static final TagKey<Item> BRONZE_NUGGETS = bindC("nuggets/bronze");
    public static final TagKey<Item> BRASS_NUGGETS = bindC("nuggets/brass");
    public static final TagKey<Item> COPPER_NUGGETS = bindC("nuggets/copper");
    public static final TagKey<Item> NETHERITE_NUGGETS = bindC("nuggets/netherite");

    public static final TagKey<Item> ORES = bindC("ores");
    public static final TagKey<Item> ZINC_ORES = bindC("ores/zinc");
    public static final TagKey<Item> TIN_ORES = bindC("ores/tin");
    public static final TagKey<Item> TITANIUM_ORES = bindC("ores/titanium");
    public static final TagKey<Item> TUNGSTEN_ORES = bindC("ores/tungsten");
    public static final TagKey<Item> LEAD_ORES = bindC("ores/lead");
    public static final TagKey<Item> SILVER_ORES = bindC("ores/silver");
    public static final TagKey<Item> URANIUM_ORES = bindC("ores/uranium");
    public static final TagKey<Item> VOID_MATTER_ORES = bindC("ores/void_matter");
    public static final TagKey<Item> EARTH_CORE_SHARD_ORES = bindC("ores/earth_core_shard");

    public static final TagKey<Item> STORAGE_BLOCKS_RAW_TUNGSTEN = bindC("storage_blocks/raw_tungsten");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_TITANIUM = bindC("storage_blocks/raw_titanium");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_ZINC = bindC("storage_blocks/raw_zinc");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_TIN = bindC("storage_blocks/raw_tin");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_LEAD = bindC("storage_blocks/raw_lead");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_SILVER = bindC("storage_blocks/raw_silver");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_URANIUM = bindC("storage_blocks/raw_uranium");

    public static final TagKey<Item> RAW_ORES = bindC("raw_materials");
    public static final TagKey<Item> RAW_ZINC = bindC("raw_materials/zinc");
    public static final TagKey<Item> RAW_TIN = bindC("raw_materials/tin");
    public static final TagKey<Item> RAW_TITANIUM = bindC("raw_materials/titanium");
    public static final TagKey<Item> RAW_TUNGSTEN = bindC("raw_materials/tungsten");
    public static final TagKey<Item> RAW_LEAD = bindC("raw_materials/lead");
    public static final TagKey<Item> RAW_SILVER = bindC("raw_materials/silver");
    public static final TagKey<Item> RAW_URANIUM = bindC("raw_materials/uranium");

    public static final TagKey<Item> BUCKETS = bindC("buckets");
    public static final TagKey<Item> OIL_BUCKETS = bindC("buckets/oil");
    public static final TagKey<Item> CEMENT_BUCKETS = bindC("buckets/cement");

    public static final TagKey<Item> VEGETABLES = bindC("vegetables");
    public static final TagKey<Item> SEEDS = bindC("seeds");
    public static final TagKey<Item> BERRIES = bindC("berries");
    public static final TagKey<Item> WRENCH = bindC("tools/wrench");

    public static final TagKey<Item> ROYAL_STEEL_PICKAXE_BASE = bind("royal_steel_pickaxe_base");
    public static final TagKey<Item> ROYAL_STEEL_AXE_BASE = bind("royal_steel_axe_base");
    public static final TagKey<Item> ROYAL_STEEL_HOE_BASE = bind("royal_steel_hoe_base");
    public static final TagKey<Item> ROYAL_STEEL_SHOVEL_BASE = bind("royal_steel_shovel_base");
    public static final TagKey<Item> ROYAL_STEEL_SWORD_BASE = bind("royal_steel_sword_base");
    public static final TagKey<Item> EMBER_METAL_PICKAXE_BASE = bind("ember_metal_pickaxe_base");
    public static final TagKey<Item> EMBER_METAL_AXE_BASE = bind("ember_metal_axe_base");
    public static final TagKey<Item> EMBER_METAL_HOE_BASE = bind("ember_metal_hoe_base");
    public static final TagKey<Item> EMBER_METAL_SHOVEL_BASE = bind("ember_metal_shovel_base");
    public static final TagKey<Item> EMBER_METAL_SWORD_BASE = bind("ember_metal_sword_base");
    public static final TagKey<Item> CAPACITOR = bind("capacitor");
    public static final TagKey<Item> GEMS = bind("gems");
    public static final TagKey<Item> GEM_BLOCKS = bind("gem_blocks");
    public static final TagKey<Item> DEAD_CORALS = bind("dead_corals");
    public static final TagKey<Item> VOID_RESISTANT = bind("void_resistant");
    public static final TagKey<Item> REINFORCED_CONCRETE = bind("reinforced_concrete");
    public static final TagKey<Item> SEEDS_PACK_CONTENT = bind("seeds_pack_content");
    public static final TagKey<Item> FIRE_STARTER = bind("fire_starter");
    public static final TagKey<Item> UNBROKEN_FIRE_STARTER = bind("unbroken_fire_starter");
    public static final TagKey<Item> NETHERITE_BLOCK = bind("netherite_block");
    public static final TagKey<Item> EXPLOSION_PROOF = bind("explosion_proof");
    public static final TagKey<Item> AMULET = bind("amulet");
    public static final TagKey<Item> ANVIL_HAMMER = bind("tools/anvil_hammer");

    public static final Object2ObjectMap<Color, TagKey<Item>> DYED_COLORS = initDyedTags();

    public static @NotNull TagKey<Item> bindC(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    public static @NotNull TagKey<Item> bind(String id) {
        return TagKey.create(Registries.ITEM, AnvilCraft.of(id));
    }

    public static Object2ObjectMap<Color, TagKey<Item>> initDyedTags() {
        Object2ObjectMap<Color, TagKey<Item>> map = new Object2ObjectOpenHashMap<>();
        for (Color color : Color.values()) {
            map.put(color, bindC("dyed/" + color));
        }
        return map;
    }

}
