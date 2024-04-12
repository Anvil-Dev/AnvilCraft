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
    public static final TagKey<Item> ROYAL_STEEL_PICKAXE_BASE = bindMod("royal_steel_pickaxe_base");


    private static @NotNull TagKey<Item> bindC(String id) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", id));
    }

    @SuppressWarnings("SameParameterValue")
    private static @NotNull TagKey<Item> bindMod(String id) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("anvilcraft", id));
    }

    private static @NotNull TagKey<Item> bind(String id) {
        return TagKey.create(Registries.ITEM, AnvilCraft.of(id));
    }
}
