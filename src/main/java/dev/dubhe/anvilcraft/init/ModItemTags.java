package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

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
    public static final TagKey<Item> PROTOCOL = bind("protocol");
    public static final TagKey<Item> RAW_FOODS = bind("raw_foods");

    private static @NotNull TagKey<Item> bindC(String id) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", id));
    }

    private static @NotNull TagKey<Item> bind(String id) {
        return TagKey.create(Registries.ITEM, AnvilCraft.of(id));
    }
}
