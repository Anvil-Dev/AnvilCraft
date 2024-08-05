package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
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
    public static final TagKey<Item> FOODS = bindC("foods");
    public static final TagKey<Item> PLATES = bindC("plates");
    public static final TagKey<Item> STONE = bindC("stone");
    public static final TagKey<Item> GLASS = bindC("silica_glass");

    public static final TagKey<Item> GOLD_PLATES = bindC("gold_plates");
    public static final TagKey<Item> IRON_PLATES = bindC("iron_plates");
    public static final TagKey<Item> COPPER_PLATES = bindC("copper_plates");
    public static final TagKey<Item> TUNGSTEN_PLATES = bindC("tungsten_plates");
    public static final TagKey<Item> TITANIUM_PLATES = bindC("titanium_plates");
    public static final TagKey<Item> ZINC_PLATES = bindC("zinc_plates");
    public static final TagKey<Item> TIN_PLATES = bindC("tin_plates");
    public static final TagKey<Item> LEAD_PLATES = bindC("lead_plates");
    public static final TagKey<Item> SILVER_PLATES = bindC("silver_plates");
    public static final TagKey<Item> URANIUM_PLATES = bindC("uranium_plates");
    public static final TagKey<Item> BRONZE_PLATES = bindC("bronze_plates");
    public static final TagKey<Item> BRASS_PLATES = bindC("brass_plates");

    public static final TagKey<Item> TUNGSTEN_INGOTS = bindC("tungsten_ingots");
    public static final TagKey<Item> TITANIUM_INGOTS = bindC("titanium_ingots");
    public static final TagKey<Item> ZINC_INGOTS = bindC("zinc_ingots");
    public static final TagKey<Item> TIN_INGOTS = bindC("tin_ingots");
    public static final TagKey<Item> LEAD_INGOTS = bindC("lead_ingots");
    public static final TagKey<Item> SILVER_INGOTS = bindC("silver_ingots");
    public static final TagKey<Item> URANIUM_INGOTS = bindC("uranium_ingots");
    public static final TagKey<Item> BRONZE_INGOTS = bindC("bronze_ingots");
    public static final TagKey<Item> BRASS_INGOTS = bindC("brass_ingots");

    public static final TagKey<Item> TUNGSTEN_NUGGETS = bindC("tungsten_nuggets");
    public static final TagKey<Item> TITANIUM_NUGGETS = bindC("titanium_nuggets");
    public static final TagKey<Item> ZINC_NUGGETS = bindC("zinc_nuggets");
    public static final TagKey<Item> TIN_NUGGETS = bindC("tin_nuggets");
    public static final TagKey<Item> LEAD_NUGGETS = bindC("lead_nuggets");
    public static final TagKey<Item> SILVER_NUGGETS = bindC("silver_nuggets");
    public static final TagKey<Item> URANIUM_NUGGETS = bindC("uranium_nuggets");
    public static final TagKey<Item> COPPER_NUGGETS = bindC("copper_nuggets");
    public static final TagKey<Item> BRONZE_NUGGETS = bindC("bronze_nuggets");
    public static final TagKey<Item> BRASS_NUGGETS = bindC("brass_nuggets");

    public static final TagKey<Item> QUARTZ_BLOCKS = bindC("quartz_blocks");
    public static final TagKey<Item> AMETHYST_BLOCKS = bindC("amethyst_blocks");

    public static final TagKey<Item> ORES = bindC("ores");
    public static final TagKey<Item> ZINC_ORES = bindC("zinc_ores");
    public static final TagKey<Item> TIN_ORES = bindC("tin_ores");
    public static final TagKey<Item> TITANIUM_ORES = bindC("titanium_ores");
    public static final TagKey<Item> TUNGSTEN_ORES = bindC("tungsten_ores");
    public static final TagKey<Item> LEAD_ORES = bindC("lead_ores");
    public static final TagKey<Item> SILVER_ORES = bindC("silver_ores");
    public static final TagKey<Item> URANIUM_ORES = bindC("uranium_ores");

    public static final TagKey<Item> RAW_ORES = bindC("raw_ores");
    public static final TagKey<Item> RAW_ZINC = bindC("raw_zinc_ores");
    public static final TagKey<Item> RAW_TIN = bindC("raw_tin_ores");
    public static final TagKey<Item> RAW_TITANIUM = bindC("raw_titanium_ores");
    public static final TagKey<Item> RAW_TUNGSTEN = bindC("raw_tungsten_ores");
    public static final TagKey<Item> RAW_LEAD = bindC("raw_lead_ores");
    public static final TagKey<Item> RAW_SILVER = bindC("raw_silver_ores");
    public static final TagKey<Item> RAW_URANIUM = bindC("raw_uranium_ores");

    public static final TagKey<Item> VEGETABLES = bindC("vegetables");
    public static final TagKey<Item> SEEDS = bindC("seeds");
    public static final TagKey<Item> BERRIES = bindC("berries");
    public static final TagKey<Item> WRENCHES = bindC("wrenches");


    public static final TagKey<Item> FLOUR_FORGE = bindForge("flour");
    public static final TagKey<Item> WHEAT_FLOUR_FORGE = bindForge("flour/wheat");
    public static final TagKey<Item> DOUGH_FORGE = bindForge("dough");
    public static final TagKey<Item> WHEAT_DOUGH_FORGE = bindForge("dough/wheat");
    public static final TagKey<Item> PLATES_FORGE = bindForge("plates");
    public static final TagKey<Item> STONE_FORGE = bindForge("stone");
    public static final TagKey<Item> GLASS_FORGE = bindForge("glass/silica");

    public static final TagKey<Item> GOLD_PLATES_FORGE = bindForge("plates/gold");
    public static final TagKey<Item> IRON_PLATES_FORGE = bindForge("plates/iron");
    public static final TagKey<Item> COPPER_PLATES_FORGE = bindForge("plates/copper");
    public static final TagKey<Item> TUNGSTEN_PLATES_FORGE = bindForge("plates/tungsten");
    public static final TagKey<Item> TITANIUM_PLATES_FORGE = bindForge("plates/titanium");
    public static final TagKey<Item> ZINC_PLATES_FORGE = bindForge("plates/zinc");
    public static final TagKey<Item> TIN_PLATES_FORGE = bindForge("plates/tin");
    public static final TagKey<Item> LEAD_PLATES_FORGE = bindForge("plates/lead");
    public static final TagKey<Item> SILVER_PLATES_FORGE = bindForge("plates/silver");
    public static final TagKey<Item> URANIUM_PLATES_FORGE = bindForge("plates/uranium");
    public static final TagKey<Item> BRONZE_PLATES_FORGE = bindForge("plates/bronze");
    public static final TagKey<Item> BRASS_PLATES_FORGE = bindForge("plates/brass");

    public static final TagKey<Item> TUNGSTEN_INGOTS_FORGE = bindForge("ingots/tungsten");
    public static final TagKey<Item> TITANIUM_INGOTS_FORGE = bindForge("ingots/titanium");
    public static final TagKey<Item> ZINC_INGOTS_FORGE = bindForge("ingots/zinc");
    public static final TagKey<Item> TIN_INGOTS_FORGE = bindForge("ingots/tin");
    public static final TagKey<Item> LEAD_INGOTS_FORGE = bindForge("ingots/lead");
    public static final TagKey<Item> SILVER_INGOTS_FORGE = bindForge("ingots/silver");
    public static final TagKey<Item> URANIUM_INGOTS_FORGE = bindForge("ingots/uranium");
    public static final TagKey<Item> BRONZE_INGOTS_FORGE = bindForge("ingots/bronze");
    public static final TagKey<Item> BRASS_INGOTS_FORGE = bindForge("ingots/brass");

    public static final TagKey<Item> TUNGSTEN_NUGGETS_FORGE = bindForge("nuggets/tungsten");
    public static final TagKey<Item> TITANIUM_NUGGETS_FORGE = bindForge("nuggets/titanium");
    public static final TagKey<Item> ZINC_NUGGETS_FORGE = bindForge("nuggets/zinc");
    public static final TagKey<Item> TIN_NUGGETS_FORGE = bindForge("nuggets/tin");
    public static final TagKey<Item> LEAD_NUGGETS_FORGE = bindForge("nuggets/lead");
    public static final TagKey<Item> SILVER_NUGGETS_FORGE = bindForge("nuggets/silver");
    public static final TagKey<Item> URANIUM_NUGGETS_FORGE = bindForge("nuggets/uranium");
    public static final TagKey<Item> BRONZE_NUGGETS_FORGE = bindForge("nuggets/bronze");
    public static final TagKey<Item> BRASS_NUGGETS_FORGE = bindForge("nuggets/brass");
    public static final TagKey<Item> COPPER_NUGGETS_FORGE = bindForge("nuggets/copper");

    public static final TagKey<Item> QUARTZ_BLOCKS_FORGE = bindForge("storage_blocks/quartz");
    public static final TagKey<Item> AMETHYST_BLOCKS_FORGE = bindForge("storage_blocks/amethyst");

    public static final TagKey<Item> ORES_FORGE = bindForge("ores");
    public static final TagKey<Item> ZINC_ORES_FORGE = bindForge("ores/zinc");
    public static final TagKey<Item> TIN_ORES_FORGE = bindForge("ores/tin");
    public static final TagKey<Item> TITANIUM_ORES_FORGE = bindForge("ores/titanium");
    public static final TagKey<Item> TUNGSTEN_ORES_FORGE = bindForge("ores/tungsten");
    public static final TagKey<Item> LEAD_ORES_FORGE = bindForge("ores/lead");
    public static final TagKey<Item> SILVER_ORES_FORGE = bindForge("ores/silver");
    public static final TagKey<Item> URANIUM_ORES_FORGE = bindForge("ores/uranium");

    public static final TagKey<Item> RAW_ORES_FORGE = bindForge("raw_materials");
    public static final TagKey<Item> RAW_ZINC_FORGE = bindForge("raw_materials/zinc");
    public static final TagKey<Item> RAW_TIN_FORGE = bindForge("raw_materials/tin");
    public static final TagKey<Item> RAW_TITANIUM_FORGE = bindForge("raw_materials/titanium");
    public static final TagKey<Item> RAW_TUNGSTEN_FORGE = bindForge("raw_materials/tungsten");
    public static final TagKey<Item> RAW_LEAD_FORGE = bindForge("raw_materials/lead");
    public static final TagKey<Item> RAW_SILVER_FORGE = bindForge("raw_materials/silver");
    public static final TagKey<Item> RAW_URANIUM_FORGE = bindForge("raw_materials/uranium");

    public static final TagKey<Item> VEGETABLES_FORGE = bindForge("vegetables");
    public static final TagKey<Item> SEEDS_FORGE = bindForge("seeds");
    public static final TagKey<Item> BERRIES_FORGE = bindForge("berries");
    public static final TagKey<Item> WRENCH_FORGE = bindForge("tools/wrench");


    public static final TagKey<Item> ROYAL_STEEL_PICKAXE_BASE = bind("royal_steel_pickaxe_base");
    public static final TagKey<Item> ROYAL_STEEL_AXE_BASE = bind("royal_steel_axe_base");
    public static final TagKey<Item> ROYAL_STEEL_HOE_BASE = bind("royal_steel_hoe_base");
    public static final TagKey<Item> ROYAL_STEEL_SHOVEL_BASE = bind("royal_steel_shovel_base");
    public static final TagKey<Item> ROYAL_STEEL_SWORD_BASE = bind("royal_steel_sword_base");
    public static final TagKey<Item> CAPACITOR = bind("capacitor");
    public static final TagKey<Item> GEMS = bind("gems");
    public static final TagKey<Item> GEM_BLOCKS = bind("gem_blocks");
    public static final TagKey<Item> DEAD_TUBE = bind("dead_tube");
    public static final TagKey<Item> VOID_RESISTANT = bind("void_resistant");
    public static final TagKey<Item> REINFORCED_CONCRETE = bind("reinforced_concrete");
    public static final TagKey<Item> SEEDS_PACK_CONTENT = bind("seeds_pack_content");
    public static final TagKey<Item> FIRE_STARTER = bind("fire_starter");
    public static final TagKey<Item> UNBROKEN_FIRE_STARTER = bind("unbroken_fire_starter");
    public static final TagKey<Item> NETHERITE_BLOCK = bind("netherite_block");
    public static final TagKey<Item> EXPLOSION_PROOF = bind("explosion_proof");

    public static @NotNull TagKey<Item> bindC(String id) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", id));
    }

    public static @NotNull TagKey<Item> bindForge(String id) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("forge", id));
    }

    public static @NotNull TagKey<Item> bind(String id) {
        return TagKey.create(Registries.ITEM, AnvilCraft.of(id));
    }
}
