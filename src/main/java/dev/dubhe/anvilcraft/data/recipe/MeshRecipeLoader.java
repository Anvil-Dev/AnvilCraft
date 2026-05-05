package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import net.minecraft.world.item.Items;

public class MeshRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        MeshRecipe.builder()
            .requires(Items.GRAVEL)
            .result(Items.GRAVEL, 0.5F)
            .result(Items.FLINT, 0.25F)
            .result(Items.IRON_NUGGET, 0.2F)
            .save(provider);

        MeshRecipe.builder()
            .requires(Items.SAND)
            .result(Items.SAND, 0.5F)
            .result(Items.CLAY_BALL, 0.25F)
            .result(Items.GOLD_NUGGET, 0.05F)
            .save(provider);

        MeshRecipe.builder()
            .requires(Items.RED_SAND)
            .result(Items.RED_SAND, 0.5F)
            .result(Items.GLOWSTONE_DUST, 0.1F)
            .result(ModItems.COPPER_NUGGET, 0.2F)
            .save(provider);

        MeshRecipe.builder()
            .requires(Items.SOUL_SAND)
            .result(Items.SOUL_SAND, 0.5F)
            .result(Items.NETHER_WART, 0.005F)
            .save(provider);

        MeshRecipe.builder()
            .requires(ModBlocks.NETHER_DUST)
            .result(ModBlocks.NETHER_DUST, 0.5F)
            .result(Items.REDSTONE, 0.1F)
            .result(ModItems.TUNGSTEN_NUGGET, 0.1F)
            .save(provider);

        MeshRecipe.builder()
            .requires(ModBlocks.END_DUST)
            .result(ModBlocks.END_DUST, 0.5F)
            .result(Items.CHORUS_FLOWER, 0.005F)
            .result(ModItems.TITANIUM_NUGGET, 0.1F)
            .result(ModItems.LEVITATION_POWDER, 0.1F)
            .save(provider);

        MeshRecipe.builder()
            .requires(ModBlocks.CINERITE)
            .result(ModBlocks.CINERITE, 0.5F)
            .result(Items.LAPIS_LAZULI, 0.1F)
            .result(Items.GUNPOWDER, 0.1F)
            .result(ModItems.ZINC_NUGGET, 0.1F)
            .result(ModItems.LEAD_NUGGET, 0.1F)
            .result(ModItems.TIN_NUGGET, 0.1F)
            .result(ModItems.SILVER_NUGGET, 0.1F)
            .save(provider);

        MeshRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND)
            .result(ModBlocks.QUARTZ_SAND, 0.5F)
            .result(Items.QUARTZ)
            .save(provider);
    }
}
