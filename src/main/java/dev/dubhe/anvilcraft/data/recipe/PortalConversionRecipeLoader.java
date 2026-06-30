package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.api.portal.PortalType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class PortalConversionRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        PortalConversionRecipeLoader.spectral(provider, Blocks.DAMAGED_ANVIL, 0.01F);
        PortalConversionRecipeLoader.spectral(provider, Blocks.CHIPPED_ANVIL, 0.02F);
        PortalConversionRecipeLoader.spectral(provider, Blocks.ANVIL, 0.03F);
        PortalConversionRecipeLoader.spectral(provider, ModBlocks.ROYAL_ANVIL, 0.5F);
        PortalConversionRecipeLoader.spectral(provider, ModBlocks.FROST_ANVIL, 1.0F);
        PortalConversionRecipeLoader.spectral(provider, ModBlocks.EMBER_ANVIL, 1.0F);
        PortalConversionRecipeLoader.spectral(provider, ModBlocks.TRANSCENDENCE_ANVIL, 1.0F);
    }

    @SuppressWarnings("deprecation")
    private static void spectral(RegistrumRecipeProvider provider, Block anvil, float chance) {
        PortalConversionRecipe.builder()
            .type(PortalType.END_PORTAL)
            .input(anvil)
            .result(ModBlocks.SPECTRAL_ANVIL.getDefaultState(), chance)
            .save(provider, anvil.builtInRegistryHolder().key().location().getPath());
    }

    private static void spectral(RegistrumRecipeProvider provider, BlockEntry<? extends Block> anvil, float chance) {
        PortalConversionRecipe.builder()
            .type(PortalType.END_PORTAL)
            .input(anvil)
            .result(ModBlocks.SPECTRAL_ANVIL.getDefaultState(), chance)
            .save(provider, anvil.getId().getPath());
    }
}
