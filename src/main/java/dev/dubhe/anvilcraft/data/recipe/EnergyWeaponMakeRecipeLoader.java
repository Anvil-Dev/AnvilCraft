package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;
import net.minecraft.world.item.ItemStackTemplate;

public class EnergyWeaponMakeRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        EnergyWeaponMakeRecipe.builder()
            .requires(ModItems.SPECTRAL_SLINGSHOT, 1)
            .requires(ModBlocks.SPECTRAL_ANVIL, 8)
            .result(new ItemStackTemplate(ModItems.SPECTRAL_WEAPON_LAUNCHER))
            .save(provider);
        EnergyWeaponMakeRecipe.builder()
            .requires(ModBlocks.ACCELERATION_RING, 4)
            .requires(ModBlocks.SLIDING_RAIL, 4)
            .result(new ItemStackTemplate(ModItems.ANVIL_RAILGUN))
            .save(provider);
    }
}
