package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;

public class EnergyWeaponMakeRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        EnergyWeaponMakeRecipe.builder()
            .requires(ModItems.SPECTRAL_SLINGSHOT, 1)
            .requires(ModBlocks.SPECTRAL_ANVIL, 8)
            .result(ModItems.SPECTRAL_WEAPON_LAUNCHER.asStack())
            .save(provider);
        EnergyWeaponMakeRecipe.builder()
            .requires(ModBlocks.ACCELERATION_RING, 4)
            .requires(ModBlocks.SLIDING_RAIL, 4)
            .result(ModItems.ANVIL_RAILGUN.asStack())
            .save(provider);
        EnergyWeaponMakeRecipe.builder()
            .requires(ModBlocks.CORRUPTED_BEACON, 1)
            .requires(ModBlocks.CURSED_GOLD_BLOCK, 9)
            .result(ModItems.CORRUPTED_BEACON_ACTIVATOR.asStack())
            .save(provider);
        EnergyWeaponMakeRecipe.builder()
            .requires(ModBlocks.TESLA_TOWER, 4)
            .result(ModItems.TESLA_GUN.asStack())
            .save(provider);
        EnergyWeaponMakeRecipe.builder()
            .requires(ModBlocks.RUBY_LASER, 16)
            .requires(ModBlocks.RUBY_PRISM, 4)
            .result(ModItems.LASER_GUN.asStack())
            .save(provider);
    }
}
