package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import net.minecraft.world.item.Items;

public class ChargerChargingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ChargerChargingRecipe.builder()
            .requires(Items.IRON_INGOT)
            .result(ModItems.MAGNET_INGOT)
            .power(-4)
            .time(20 * 2)
            .save(provider);
        ChargerChargingRecipe.builder()
            .requires(ModItems.CAPACITOR_EMPTY)
            .result(ModItems.CAPACITOR)
            .power(-70)
            .time(20 * 60)
            .save(provider);
        ChargerChargingRecipe.builder()
            .requires(ModItems.CAPACITOR)
            .result(ModItems.CAPACITOR_EMPTY)
            .power(64)
            .time(20 * 60)
            .save(provider);
        ChargerChargingRecipe.builder()
            .requires(ModItems.SUPER_CAPACITOR_EMPTY)
            .result(ModItems.SUPER_CAPACITOR)
            .power(-700)
            .time(20 * 120)
            .save(provider);
        ChargerChargingRecipe.builder()
            .requires(ModItems.SUPER_CAPACITOR)
            .result(ModItems.SUPER_CAPACITOR_EMPTY)
            .power(640)
            .time(20 * 120)
            .save(provider);
        ChargerChargingRecipe.builder()
            .requires(ModItemTags.UNCHARGED_NEUTRONIUM_INGOTS)
            .result(ModItems.CHARGED_NEUTRONIUM_INGOT)
            .power(-4000)
            .time(20 * 300)
            .save(provider);
        ChargerChargingRecipe.builder()
            .requires(ModItems.CHARGED_NEUTRONIUM_INGOT)
            .result(ModItems.NEUTRONIUM_INGOT)
            .power(3200)
            .time(20 * 300)
            .save(provider);
    }
}
