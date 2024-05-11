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
    public static final TagKey<Item> GOLD_PLATES = bindC("gold_plates");
    public static final TagKey<Item> IRON_PLATES = bindC("iron_plates");
    public static final TagKey<Item> STONE = bindC("stone");
    public static final TagKey<Item> GLASS = bindC("silica_glass");
    public static final TagKey<Item> TUNGSTEN_NUGGETS = bindC("tungsten_nuggets");
    public static final TagKey<Item> TUNGSTEN_INGOTS = bindC("tungsten_ingots");
    public static final TagKey<Item> TITANIUM_NUGGETS = bindC("titanium_nuggets");
    public static final TagKey<Item> TITANIUM_INGOTS = bindC("titanium_ingots");
    public static final TagKey<Item> ZINC_NUGGETS = bindC("zinc_nuggets");
    public static final TagKey<Item> ZINC_INGOTS = bindC("zinc_ingots");
    public static final TagKey<Item> TIN_NUGGETS = bindC("tin_nuggets");
    public static final TagKey<Item> TIN_INGOTS = bindC("tin_ingots");
    public static final TagKey<Item> LEAD_NUGGETS = bindC("lead_nuggets");
    public static final TagKey<Item> LEAD_INGOTS = bindC("lead_ingots");
    public static final TagKey<Item> SILVER_NUGGETS = bindC("silver_nuggets");
    public static final TagKey<Item> SILVER_INGOTS = bindC("silver_ingots");
    public static final TagKey<Item> COPPER_NUGGETS = bindC("copper_nuggets");
    public static final TagKey<Item> BRONZE_INGOTS = bindC("bronze_ingots");
    public static final TagKey<Item> BRONZE_NUGGETS = bindC("bronze_nuggets");
    public static final TagKey<Item> BRASS_INGOTS = bindC("brass_ingots");
    public static final TagKey<Item> BRASS_NUGGETS = bindC("brass_nuggets");
    public static final TagKey<Item> QUARTZ_BLOCKS = bindC("quartz_blocks");
    public static final TagKey<Item> AMETHYST_BLOCKS = bindC("amethyst_blocks");

    public static final TagKey<Item> FLOUR_FORGE = bindForge("flour");
    public static final TagKey<Item> WHEAT_FLOUR_FORGE = bindForge("flour/wheat");
    public static final TagKey<Item> DOUGH_FORGE = bindForge("dough");
    public static final TagKey<Item> WHEAT_DOUGH_FORGE = bindForge("dough/wheat");
    public static final TagKey<Item> PLATES_FORGE = bindForge("plates");
    public static final TagKey<Item> GOLD_PLATES_FORGE = bindForge("plates/gold");
    public static final TagKey<Item> IRON_PLATES_FORGE = bindForge("plates/iron");
    public static final TagKey<Item> STONE_FORGE = bindForge("stone");
    public static final TagKey<Item> GLASS_FORGE = bindForge("glass/silica");
    public static final TagKey<Item> TUNGSTEN_NUGGETS_FORGE = bindForge("nuggets/tungsten");
    public static final TagKey<Item> TUNGSTEN_INGOTS_FORGE = bindForge("ingots/tungsten");
    public static final TagKey<Item> TITANIUM_NUGGETS_FORGE = bindForge("nuggets/titanium");
    public static final TagKey<Item> TITANIUM_INGOTS_FORGE = bindForge("ingots/titanium");
    public static final TagKey<Item> ZINC_NUGGETS_FORGE = bindForge("nuggets/zinc");
    public static final TagKey<Item> ZINC_INGOTS_FORGE = bindForge("ingots/zinc");
    public static final TagKey<Item> TIN_NUGGETS_FORGE = bindForge("nuggets/tin");
    public static final TagKey<Item> TIN_INGOTS_FORGE = bindForge("ingots/tin");
    public static final TagKey<Item> LEAD_NUGGETS_FORGE = bindForge("nuggets/lead");
    public static final TagKey<Item> LEAD_INGOTS_FORGE = bindForge("ingots/lead");
    public static final TagKey<Item> SILVER_NUGGETS_FORGE = bindForge("nuggets/silver");
    public static final TagKey<Item> SILVER_INGOTS_FORGE = bindForge("ingots/silver");
    public static final TagKey<Item> BRONZE_INGOTS_FORGE = bindForge("ingots/bronze");
    public static final TagKey<Item> BRONZE_NUGGETS_FORGE = bindForge("nuggets/bronze");
    public static final TagKey<Item> BRASS_INGOTS_FORGE = bindForge("ingots/brass");
    public static final TagKey<Item> BRASS_NUGGETS_FORGE = bindForge("nuggets/brass");
    public static final TagKey<Item> COPPER_NUGGETS_FORGE = bindForge("nuggets/copper");
    public static final TagKey<Item> QUARTZ_BLOCKS_FORGE = bindForge("storage_blocks/quartz");
    public static final TagKey<Item> AMETHYST_BLOCKS_FORGE = bindForge("storage_blocks/amethyst");


    public static final TagKey<Item> ROYAL_STEEL_PICKAXE_BASE = bind("royal_steel_pickaxe_base");
    public static final TagKey<Item> ROYAL_STEEL_AXE_BASE = bind("royal_steel_axe_base");
    public static final TagKey<Item> ROYAL_STEEL_HOE_BASE = bind("royal_steel_hoe_base");
    public static final TagKey<Item> ROYAL_STEEL_SHOVEL_BASE = bind("royal_steel_shovel_base");
    public static final TagKey<Item> ROYAL_STEEL_SWORD_BASE = bind("royal_steel_sword_base");
    public static final TagKey<Item> CAPACITOR = bind("capacitor");
    public static final TagKey<Item> GEMS = bind("gems");
    public static final TagKey<Item> GEM_BLOCKS = bind("gem_blocks");
    public static final TagKey<Item> DEAD_TUBE = bind("dead_tube");


    private static @NotNull TagKey<Item> bindC(String id) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", id));
    }

    private static @NotNull TagKey<Item> bindForge(String id) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("forge", id));
    }

    private static @NotNull TagKey<Item> bind(String id) {
        return TagKey.create(Registries.ITEM, AnvilCraft.of(id));
    }
}
