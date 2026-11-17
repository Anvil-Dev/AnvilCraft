package dev.dubhe.anvilcraft.client.init;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyPresets;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class ModCreativeVariantGroups {
    private ModCreativeVariantGroups() {
    }

    public static void register() {
        BooleanSupplier enabled = () -> AnvilCraft.CLIENT_CONFIG.ui.enableCreativeVariantPicker;
        CreativeVariantPickerRegistry.register(enabled, ModCreativeVariantGroups.itemsOf(ModBlocks.REINFORCED_CONCRETES));
        CreativeVariantPickerRegistry.register(enabled, ModCreativeVariantGroups.itemsOf(ModBlocks.REINFORCED_CONCRETE_SLABS));
        CreativeVariantPickerRegistry.register(enabled, ModCreativeVariantGroups.itemsOf(ModBlocks.REINFORCED_CONCRETE_STAIRS));
        CreativeVariantPickerRegistry.register(enabled, ModCreativeVariantGroups.itemsOf(ModBlocks.REINFORCED_CONCRETE_WALLS));
        CreativeVariantPickerRegistry.register(enabled, ModCreativeVariantGroups.itemsOf(ModItems.CEMENT_BUCKETS));
    }

    /** 使用当前客户端世界的配方生成锻星砧预设列表。 */
    public static List<ItemStack> celestialPresets() {
        var level = Minecraft.getInstance().level;
        return level == null ? List.of() : CelestialBodyPresets.variants(level);
    }

    private static ItemLike[] itemsOf(Map<Color, ? extends ItemLike> family) {
        return family.values().toArray(new ItemLike[0]);
    }
}
