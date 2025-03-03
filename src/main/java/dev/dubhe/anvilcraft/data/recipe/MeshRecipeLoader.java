package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.MeshRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;

public class MeshRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        mesh(provider, Items.GRAVEL, Items.GRAVEL, 0.5f);
        mesh(provider, Items.GRAVEL, Items.FLINT, 0.25f);
        mesh(provider, Items.GRAVEL, Items.IRON_NUGGET, 0.2f);

        mesh(provider, Items.SAND, Items.SAND, 0.5f);
        mesh(provider, Items.SAND, Items.CLAY_BALL, 0.25f);
        mesh(provider, Items.SAND, Items.GOLD_NUGGET, 0.05f);

        mesh(provider, Items.RED_SAND, Items.RED_SAND, 0.5f);
        mesh(provider, Items.RED_SAND, Items.GLOWSTONE_DUST, 0.1f);
        mesh(provider, Items.RED_SAND, ModItems.COPPER_NUGGET, 0.2f);

        mesh(provider, Items.SOUL_SAND, Items.SOUL_SAND, 0.5f);
        mesh(provider, Items.SOUL_SAND, Items.NETHER_WART, 0.005f);

        mesh(provider, ModBlocks.NETHER_DUST, ModBlocks.NETHER_DUST, 0.5f);
        mesh(provider, ModBlocks.NETHER_DUST, Items.REDSTONE, 0.1f);
        mesh(provider, ModBlocks.NETHER_DUST, ModItems.TUNGSTEN_NUGGET, 0.1f);

        mesh(provider, ModBlocks.END_DUST, ModBlocks.END_DUST, 0.5f);
        mesh(provider, ModBlocks.END_DUST, Items.CHORUS_FLOWER, 0.005f);
        mesh(provider, ModBlocks.END_DUST, ModItems.TITANIUM_NUGGET, 0.1f);
        mesh(provider, ModBlocks.END_DUST, ModItems.LEVITATION_POWDER, 0.1f);

        mesh(provider, ModBlocks.CINERITE, ModBlocks.CINERITE, 0.5f);
        mesh(provider, ModBlocks.CINERITE, Items.LAPIS_LAZULI, 0.1f);
        mesh(provider, ModBlocks.CINERITE, Items.GUNPOWDER, 0.1f);
        mesh(provider, ModBlocks.CINERITE, ModItems.ZINC_NUGGET, 0.1f);
        mesh(provider, ModBlocks.CINERITE, ModItems.LEAD_NUGGET, 0.1f);
        mesh(provider, ModBlocks.CINERITE, ModItems.TIN_NUGGET, 0.1f);
        mesh(provider, ModBlocks.CINERITE, ModItems.SILVER_NUGGET, 0.1f);

        mesh(provider, ModBlocks.QUARTZ_SAND, ModBlocks.QUARTZ_SAND, 0.5f);
        mesh(provider, ModBlocks.QUARTZ_SAND, Items.QUARTZ, 1.0f);

    }

    private static void mesh(RegistrateRecipeProvider provider, ItemLike input, ItemLike result, float chance) {
        MeshRecipe.builder()
            .input(Ingredient.of(input))
            .result(new ItemStack(result))
            .resultAmount(BinomialDistributionGenerator.binomial(1, chance))
            .save(
                provider,
                AnvilCraft.of("mesh/%s/%s"
                    .formatted(
                        BuiltInRegistries.ITEM
                            .getKey(input.asItem())
                            .getPath(),
                        BuiltInRegistries.ITEM
                            .getKey(result.asItem())
                            .getPath())));
    }
}
