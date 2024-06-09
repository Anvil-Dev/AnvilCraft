package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SievingRecipesLoader {
    /**
     * 筛矿配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        sieving(provider, ModBlocks.NETHER_DUST,
            Map.entry(Items.REDSTONE, 0.1),
            Map.entry(ModItems.TUNGSTEN_NUGGET, 0.1)
        );
        sieving(provider, ModBlocks.END_DUST,
            Map.entry(Items.CHORUS_FLOWER, 0.005),
            Map.entry(ModItems.TITANIUM_NUGGET, 0.1),
            Map.entry(ModItems.LEVITATION_POWDER, 0.1)
        );
        sieving(provider, Items.SOUL_SAND,
            Map.entry(Items.NETHER_WART, 0.005)
        );
        sieving(provider, Items.GRAVEL,
            Map.entry(Items.FLINT, 0.25),
            Map.entry(Items.IRON_NUGGET, 0.05)
        );
        sieving(provider, Items.SAND,
            Map.entry(Items.CLAY_BALL, 0.25),
            Map.entry(Items.GOLD_NUGGET, 0.05)
        );
        sieving(provider, Items.RED_SAND,
            Map.entry(Items.GLOWSTONE_DUST, 0.1),
            Map.entry(ModItems.COPPER_NUGGET, 0.2)
        );
        sieving(provider, ModBlocks.CINERITE,
            Map.entry(Items.LAPIS_LAZULI, 0.1),
            Map.entry(ModItems.ZINC_NUGGET, 0.2)
        );
        sieving(provider, ModBlocks.QUARTZ_SAND,
            Map.entry(Items.QUARTZ, 1.0),
            Map.entry(ModItems.TIN_NUGGET, 0.2)
        );
        sieving(provider, ModBlocks.DEEPSLATE_CHIPS,
                Map.entry(ModItems.LIME_POWDER, 0.1),
                Map.entry(ModItems.LEAD_NUGGET, 0.1)
        );
        sieving(provider, ModBlocks.BLACK_SAND,
                Map.entry(Items.GUNPOWDER, 0.1),
                Map.entry(ModItems.SILVER_NUGGET, 0.1)
        );
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.SCAFFOLDING)
            .hasItemLeaves(Vec3.ZERO, new Vec3(0.0, -1.0, 0.0), 0.5, 0.2)
            .save(provider, AnvilCraft.of("sieving/leaves"));
    }

    /**
     * 筛矿
     */
    @SafeVarargs
    public static void sieving(
        RegistrateRecipeProvider provider, ItemLike item, Map.Entry<ItemLike, Double> @NotNull ... items
    ) {
        AnvilRecipe.Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .type(AnvilRecipeType.SIEVING)
                .hasBlock(Blocks.SCAFFOLDING)
                .hasItemIngredient(item)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), 0.5, item);
        for (Map.Entry<ItemLike, Double> entry : items) {
            builder.spawnItem(new Vec3(0.0, -1.0, 0.0), entry.getValue(), entry.getKey());
        }
        builder.save(provider, AnvilCraft.of("sieving/" + BuiltInRegistries.ITEM.getKey(item.asItem()).getPath()));
    }
}
