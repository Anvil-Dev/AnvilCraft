package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.anvilcraft.lib.data.provider.RegistratorRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.RecipeItem;
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

public class SievingRecipesLoader {
    private static RegistratorRecipeProvider provider = null;
    
    /**
     * 筛矿配方
     *
     * @param provider 提供器
     */
    public static void init(RegistratorRecipeProvider provider) {
        SievingRecipesLoader.provider = provider;
        sieving(ModBlocks.NETHER_DUST,
            RecipeItem.of(Items.REDSTONE, 0.1),
            RecipeItem.of(ModItems.TUNGSTEN_NUGGET, 0.1)
        );
        sieving(ModBlocks.END_DUST,
            RecipeItem.of(Items.CHORUS_FLOWER, 0.005),
            RecipeItem.of(ModItems.TITANIUM_NUGGET, 0.1),
            RecipeItem.of(ModItems.LEVITATION_POWDER, 0.1)
        );
        sieving(Items.SOUL_SAND,
            RecipeItem.of(Items.NETHER_WART, 0.005)
        );
        sieving(Items.GRAVEL,
            RecipeItem.of(Items.FLINT, 0.25),
            RecipeItem.of(Items.IRON_NUGGET, 0.05)
        );
        sieving(Items.SAND,
            RecipeItem.of(Items.CLAY_BALL, 0.25),
            RecipeItem.of(Items.GOLD_NUGGET, 0.05)
        );
        sieving(Items.RED_SAND,
            RecipeItem.of(Items.GLOWSTONE_DUST, 0.1),
            RecipeItem.of(ModItems.COPPER_NUGGET, 0.2)
        );
        sieving(ModBlocks.CINERITE,
            RecipeItem.of(Items.LAPIS_LAZULI, 0.1),
            RecipeItem.of(ModItems.ZINC_NUGGET, 0.2)
        );
        sieving(ModBlocks.QUARTZ_SAND,
            RecipeItem.of(Items.QUARTZ, 1.0),
            RecipeItem.of(ModItems.TIN_NUGGET, 0.2)
        );
        sieving(ModBlocks.DEEPSLATE_CHIPS,
                RecipeItem.of(ModItems.LIME_POWDER, 0.1),
                RecipeItem.of(ModItems.LEAD_NUGGET, 0.1)
        );
        sieving(ModBlocks.BLACK_SAND,
                RecipeItem.of(Items.GUNPOWDER, 0.1),
                RecipeItem.of(ModItems.SILVER_NUGGET, 0.1)
        );
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.SIEVING)
            .hasBlock(Blocks.SCAFFOLDING)
            .hasItemLeaves(Vec3.ZERO, new Vec3(0.0, -1.0, 0.0), 0.5, 0.2)
            .save(provider, AnvilCraft.of("sieving/leaves"));
    }

    /**
     * 筛矿
     *
     * @param item 输入物品
     * @param outputs 输出物品
     */
    public static void sieving(
            ItemLike item, RecipeItem... outputs
    ) {
        AnvilRecipe.Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .type(AnvilRecipeType.SIEVING)
                .hasBlock(Blocks.SCAFFOLDING)
                .hasItemIngredient(item)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), 0.5, item);
        for (RecipeItem output : outputs) {
            builder.spawnItem(new Vec3(0.0, -1.0, 0.0), output);
        }
        builder.save(provider, AnvilCraft.of("sieving/" + BuiltInRegistries.ITEM.getKey(item.asItem()).getPath()));
    }
}
