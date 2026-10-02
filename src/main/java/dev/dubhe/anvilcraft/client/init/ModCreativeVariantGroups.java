package dev.dubhe.anvilcraft.client.init;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.world.level.ItemLike;

import java.util.function.BooleanSupplier;

public final class ModCreativeVariantGroups {
    private ModCreativeVariantGroups() {
    }

    public static void register() {
        BooleanSupplier enabled = () -> AnvilCraft.CLIENT_CONFIG.creativeVariantPickerEnabled;
        CreativeVariantPickerRegistry.register(enabled, itemsOf(ModBlocks.REINFORCED_CONCRETES));
        CreativeVariantPickerRegistry.register(enabled, itemsOf(ModBlocks.REINFORCED_CONCRETE_SLABS));
        CreativeVariantPickerRegistry.register(enabled, itemsOf(ModBlocks.REINFORCED_CONCRETE_STAIRS));
        CreativeVariantPickerRegistry.register(enabled, itemsOf(ModBlocks.REINFORCED_CONCRETE_WALLS));
        CreativeVariantPickerRegistry.register(enabled, itemsOf(ModItems.CEMENT_BUCKETS));
    }

    private static ItemLike[] itemsOf(Object2ObjectMap<Color, ? extends ItemLike> family) {
        ItemLike[] items = new ItemLike[Color.values().length];
        int index = 0;
        for (Color color : Color.values()) {
            items[index++] = family.get(color);
        }
        return items;
    }
}
