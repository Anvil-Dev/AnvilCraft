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
    public static final TagKey<Item> PICKAXES = bindC("pickaxes");
    public static final TagKey<Item> AXES = bindC("axes");
    public static final TagKey<Item> HOES = bindC("hoes");
    public static final TagKey<Item> SHOVELS = bindC("shovels");
    public static final TagKey<Item> SWORDS = bindC("swords");
    public static final TagKey<Item> FOODS = bindC("foods");
    public static final TagKey<Item> PLATES = bindC("plates");
    public static final TagKey<Item> GOLD_PLATES = bindC("gold_plates");
    public static final TagKey<Item> IRON_PLATES = bindC("iron_plates");
    public static final TagKey<Item> STONE = bindC("stone");
    public static final TagKey<Item> GLASS = bindC("silica_glass");

    public static final TagKey<Item> FLOUR_FORGE = bindForge("flour");
    public static final TagKey<Item> WHEAT_FLOUR_FORGE = bindForge("flour/wheat");
    public static final TagKey<Item> DOUGH_FORGE = bindForge("dough");
    public static final TagKey<Item> WHEAT_DOUGH_FORGE = bindForge("dough/wheat");
    public static final TagKey<Item> PICKAXES_FORGE = bindForge("pickaxes");
    public static final TagKey<Item> AXES_FORGE = bindForge("axes");
    public static final TagKey<Item> HOES_FORGE = bindForge("hoes");
    public static final TagKey<Item> SHOVELS_FORGE = bindForge("shovels");
    public static final TagKey<Item> SWORDS_FORGE = bindForge("swords");
    public static final TagKey<Item> FOODS_FORGE = bindForge("foods");
    public static final TagKey<Item> PLATES_FORGE = bindForge("plates");
    public static final TagKey<Item> GOLD_PLATES_FORGE = bindForge("gold_plates");
    public static final TagKey<Item> IRON_PLATES_FORGE = bindForge("iron_plates");
    public static final TagKey<Item> STONE_FORGE = bindForge("stone");
    public static final TagKey<Item> GLASS_FORGE = bindForge("silica_glass");

    public static final TagKey<Item> ROYAL_STEEL_PICKAXE_BASE = bind("royal_steel_pickaxe_base");
    public static final TagKey<Item> ROYAL_STEEL_AXE_BASE = bind("royal_steel_axe_base");
    public static final TagKey<Item> ROYAL_STEEL_HOE_BASE = bind("royal_steel_hoe_base");
    public static final TagKey<Item> ROYAL_STEEL_SHOVEL_BASE = bind("royal_steel_shovel_base");
    public static final TagKey<Item> ROYAL_STEEL_SWORD_BASE = bind("royal_steel_sword_base");
    public static final TagKey<Item> CAPACITOR = bind("capacitor");
    public static final TagKey<Item> GEMS = bind("gems");
    public static final TagKey<Item> GEM_BLOCKS = bind("gem_blocks");


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
