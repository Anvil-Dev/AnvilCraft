package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import net.minecraft.world.item.Items;

public class MeshRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        MeshRecipe.builder()
            .requires(Items.GRAVEL)
            .result(Items.GRAVEL, 0.5f)
            .result(Items.FLINT, 0.25f)
            .result(Items.IRON_NUGGET, 0.2f)
            .save(provider);

        MeshRecipe.builder()
            .requires(Items.SAND)
            .result(Items.SAND, 0.5f)
            .result(Items.CLAY_BALL, 0.25f)
            .result(Items.GOLD_NUGGET, 0.05f)
            .save(provider);

        MeshRecipe.builder()
            .requires(Items.RED_SAND)
            .result(Items.RED_SAND, 0.5f)
            .result(Items.GLOWSTONE_DUST, 0.1f)
            .result(ModItems.COPPER_NUGGET, 0.2f)
            .save(provider);

        MeshRecipe.builder()
            .requires(Items.SOUL_SAND)
            .result(Items.SOUL_SAND, 0.5f)
            .result(Items.NETHER_WART, 0.005f)
            .save(provider);

        MeshRecipe.builder()
            .requires(ModBlocks.NETHER_DUST)
            .result(ModBlocks.NETHER_DUST, 0.5f)
            .result(Items.REDSTONE, 0.1f)
            .result(ModItems.TUNGSTEN_NUGGET, 0.1f)
            .save(provider);

        MeshRecipe.builder()
            .requires(ModBlocks.END_DUST)
            .result(ModBlocks.END_DUST, 0.5f)
            .result(Items.CHORUS_FLOWER, 0.005f)
            .result(ModItems.TITANIUM_NUGGET, 0.1f)
            .result(ModItems.LEVITATION_POWDER, 0.1f)
            .save(provider);

        MeshRecipe.builder()
            .requires(ModBlocks.CINERITE)
            .result(ModBlocks.CINERITE, 0.5f)
            .result(Items.LAPIS_LAZULI, 0.1f)
            .result(Items.GUNPOWDER, 0.1f)
            .result(ModItems.ZINC_NUGGET, 0.1f)
            .result(ModItems.LEAD_NUGGET, 0.1f)
            .result(ModItems.TIN_NUGGET, 0.1f)
            .result(ModItems.SILVER_NUGGET, 0.1f)
            .save(provider);

        MeshRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND)
            .result(ModBlocks.QUARTZ_SAND, 0.5f)
            .result(Items.QUARTZ)
            .save(provider);
    }
}
