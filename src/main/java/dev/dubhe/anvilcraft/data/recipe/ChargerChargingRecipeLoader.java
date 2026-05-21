package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ChargerChargingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        HolderGetter<Item> items = provider.getItems();
        ChargerChargingRecipe.builder(items)
            .requires(Items.IRON_INGOT)
            .result(ModItems.MAGNET_INGOT)
            .power(-4)
            .time(20 * 2)
            .save(provider);
        ChargerChargingRecipe.builder(items)
            .requires(ModItems.CAPACITOR_EMPTY)
            .result(ModItems.CAPACITOR)
            .power(-70)
            .time(20 * 60)
            .save(provider);
        ChargerChargingRecipe.builder(items)
            .requires(ModItems.CAPACITOR)
            .result(ModItems.CAPACITOR_EMPTY)
            .power(64)
            .time(20 * 60)
            .save(provider);
        ChargerChargingRecipe.builder(items)
            .requires(ModItems.SUPER_CAPACITOR_EMPTY)
            .result(ModItems.SUPER_CAPACITOR)
            .power(-700)
            .time(20 * 120)
            .save(provider);
        ChargerChargingRecipe.builder(items)
            .requires(ModItems.SUPER_CAPACITOR)
            .result(ModItems.SUPER_CAPACITOR_EMPTY)
            .power(640)
            .time(20 * 120)
            .save(provider);
        ChargerChargingRecipe.builder(items)
            .requires(ModItemTags.UNCHARGED_NEUTRONIUM_INGOTS)
            .result(ModItems.CHARGED_NEUTRONIUM_INGOT)
            .power(-4000)
            .time(20 * 300)
            .save(provider);
        ChargerChargingRecipe.builder(items)
            .requires(ModItems.CHARGED_NEUTRONIUM_INGOT)
            .result(ModItems.NEUTRONIUM_INGOT)
            .power(3200)
            .time(20 * 300)
            .save(provider);
    }
}
