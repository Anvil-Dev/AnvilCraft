package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.RecipeBlock;
import dev.dubhe.anvilcraft.data.recipe.RecipeItem;
import dev.dubhe.anvilcraft.data.recipe.RecipeResult;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class TimeWarpRecipesLoader {
    public static RegistrateRecipeProvider provider = null;
    
    /**
     * 初始化时移配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        TimeWarpRecipesLoader.provider = provider;
        timeWarpWithWater(ModItems.SEA_HEART_SHELL_SHARD, ModItems.SEA_HEART_SHELL);
        timeWarpWithMeltGem(Items.EMERALD, Items.EMERALD_BLOCK);
        timeWarpWithMeltGem(ModItems.RUBY, ModBlocks.RUBY_BLOCK);
        timeWarpWithMeltGem(ModItems.TOPAZ, ModBlocks.TOPAZ_BLOCK);
        timeWarpWithMeltGem(ModItems.SAPPHIRE, ModBlocks.SAPPHIRE_BLOCK);
        timeWarp(RecipeItem.of(ModItems.RESIN), RecipeResult.of(ModItems.AMBER));
        timeWarp(RecipeItem.of(Items.OBSIDIAN), RecipeResult.of(Items.CRYING_OBSIDIAN));
        timeWarp(RecipeItem.of(Items.CHARCOAL), RecipeResult.of(Items.COAL));
        timeWarp(RecipeItem.of(Items.SAND), RecipeResult.of(Items.DIRT));
        timeWarp(RecipeItem.of(Items.IRON_BLOCK), RecipeResult.of(RecipeItem.of(Items.RAW_IRON, 3)));
        timeWarp(RecipeItem.of(Items.GOLD_BLOCK), RecipeResult.of(RecipeItem.of(Items.RAW_GOLD, 3)));
        timeWarp(RecipeItem.of(Items.COPPER_BLOCK), RecipeResult.of(RecipeItem.of(Items.RAW_COPPER, 3)));
        timeWarp(RecipeItem.of(ModItems.GEODE), RecipeResult.of(Items.BUDDING_AMETHYST));
        timeWarp(RecipeItem.of(ModBlocks.CINERITE), RecipeResult.of(Items.TUFF));
        timeWarp(RecipeItem.of(ModBlocks.NETHER_DUST), RecipeResult.of(Items.SOUL_SOIL));
        timeWarp(RecipeItem.of(ModBlocks.END_DUST), RecipeResult.of(Items.END_STONE));
        timeWarp(RecipeItem.of(ModItems.LIME_POWDER, 8), RecipeResult.of(Items.CALCITE));
        timeWarp(RecipeItem.of(ModItems.NETHERITE_CRYSTAL_NUCLEUS), RecipeResult.of(Items.ANCIENT_DEBRIS));
        timeWarp(RecipeItem.of(ModBlocks.ZINC_BLOCK), RecipeResult.of(RecipeItem.of(ModItems.RAW_ZINC, 3)));
        timeWarp(RecipeItem.of(ModBlocks.TIN_BLOCK), RecipeResult.of(RecipeItem.of(ModItems.RAW_TIN, 3)));
        timeWarp(RecipeItem.of(ModBlocks.TITANIUM_BLOCK), RecipeResult.of(RecipeItem.of(ModItems.RAW_TITANIUM, 3)));
        timeWarp(RecipeItem.of(ModBlocks.TUNGSTEN_BLOCK), RecipeResult.of(RecipeItem.of(ModItems.RAW_TUNGSTEN, 3)));
        timeWarp(RecipeItem.of(ModBlocks.LEAD_BLOCK), RecipeResult.of(RecipeItem.of(ModItems.RAW_LEAD, 3)));
        timeWarp(RecipeItem.of(ModBlocks.SILVER_BLOCK), RecipeResult.of(RecipeItem.of(ModItems.RAW_SILVER, 3)));
        timeWarp(RecipeItem.of(ModBlocks.URANIUM_BLOCK), RecipeResult.of(RecipeItem.of(ModItems.RAW_URANIUM, 3)));
        timeWarp(RecipeItem.of(Items.ROSE_BUSH), RecipeResult.of(RecipeItem.of(Items.WITHER_ROSE, 0.2f)));
        timeWarpToOilCauldron(
                RecipeItem.of(Items.ROTTEN_FLESH, 64),
                RecipeItem.of(Items.ROTTEN_FLESH, 64),
                RecipeItem.of(Items.ROTTEN_FLESH, 64)
        );
        timeWarpToOilCauldron(
                RecipeItem.of(Items.SPIDER_EYE, 64),
                RecipeItem.of(Items.SPIDER_EYE, 64),
                RecipeItem.of(Items.SPIDER_EYE, 64)
        );
        timeWarpToOilCauldron(RecipeItem.of(Items.CHICKEN, 64));
        timeWarpToOilCauldron(RecipeItem.of(ItemTags.FISHES, 64));
        timeWarp(
                RecipeBlock.of(Blocks.CAULDRON),
                new RecipeItem[]{RecipeItem.of(Items.BEEF, 64)},
                new RecipeResult[]{RecipeResult.of(
                        RecipeBlock.of(
                                ModBlocks.OIL_CAULDRON.getDefaultState().setValue(LayeredCauldronBlock.LEVEL, 3)),
                        Items.CAULDRON)},
                "");
        timeWarp(
                RecipeBlock.of(Blocks.CAULDRON),
                new RecipeItem[]{RecipeItem.of(Items.PORKCHOP, 64)},
                new RecipeResult[]{RecipeResult.of(
                        RecipeBlock.of(
                                ModBlocks.OIL_CAULDRON.getDefaultState().setValue(LayeredCauldronBlock.LEVEL, 3))
                                    .setKey(ModBlocks.OIL_CAULDRON.getId().getPath()),
                        Items.CAULDRON)},
                "");
        timeWarp(
                RecipeBlock.of(Blocks.CAULDRON),
                new RecipeItem[]{RecipeItem.of(Items.MUTTON, 64)},
                new RecipeResult[]{RecipeResult.of(
                        RecipeBlock.of(
                                ModBlocks.OIL_CAULDRON.getDefaultState().setValue(LayeredCauldronBlock.LEVEL, 3))
                                    .setKey(ModBlocks.OIL_CAULDRON.getId().getPath()),
                        Items.CAULDRON)},
                "");
        timeWarp(
                RecipeBlock.of(Blocks.CAULDRON),
                new RecipeItem[]{RecipeItem.of(Items.RABBIT, 64)},
                new RecipeResult[]{RecipeResult.of(
                        RecipeBlock.of(
                                ModBlocks.OIL_CAULDRON.getDefaultState().setValue(LayeredCauldronBlock.LEVEL, 3))
                                    .setKey(ModBlocks.OIL_CAULDRON.getId().getPath()),
                        Items.CAULDRON)},
                "");
        timeWarpToEmberMetalNugget(RecipeItem.of(ModItemTags.NETHERITE_BLOCK));
    }

    /**
     * 时移配方
     */
    public static void timeWarp(
            RecipeBlock recipeBlock,
            RecipeItem[] items,
            RecipeResult[] recipeResults,
            String index
    ) {
        if (TimeWarpRecipesLoader.provider == null) return;
        AnvilRecipe.Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.TIMEWARP)
            .icon(recipeResults[0].icon())
            .hasBlock(
                ModBlocks.CORRUPTED_BEACON.get(),
                new Vec3(0.0, -2.0, 0.0),
                Map.entry(CorruptedBeaconBlock.LIT, true)
            )
            .hasBlock(new Vec3(0.0, -1.0, 0.0), recipeBlock);
        for (RecipeItem recipeItem : items) {
            builder = builder.hasItemIngredient(new Vec3(0.0, -1.0, 0.0), recipeItem);
        }
        for (RecipeResult recipeResult : recipeResults) {
            builder = recipeResult.isItem()
                    ? builder.spawnItem(
                            new Vec3(0.0, -1.0, 0.0).add(recipeResult.offset()),
                            recipeResult.recipeItem())
                    : builder.setBlock(
                            new Vec3(0.0, -1.0, 0.0).add(recipeResult.offset()),
                            recipeResult.recipeBlock());
        }
        builder
                .unlockedBy(AnvilCraftDatagen.hasItem(items[0]), AnvilCraftDatagen.has(items[0]))
                .save(provider, AnvilCraft.of("timewarp/"
                        + recipeResults[0].getKey() + "_from_" + items[0].getKey()
                        + (index.isEmpty() ? "" : "_" + index)));
    }

    public static void timeWarp(RecipeItem item, RecipeResult recipeResult) {
        timeWarp(RecipeBlock.of(Blocks.CAULDRON), new RecipeItem[]{item}, new RecipeResult[]{recipeResult}, "");
    }

    /**
     * 需水时移配方
     */
    public static void timeWarpWithWater(ItemLike item, ItemLike item1) {
        if (TimeWarpRecipesLoader.provider == null) return;
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.TIMEWARP)
            .icon(item1)
            .hasBlock(
                ModBlocks.CORRUPTED_BEACON.get(),
                new Vec3(0.0, -2.0, 0.0),
                Map.entry(CorruptedBeaconBlock.LIT, true)
            )
            .hasFluidCauldronFull(new Vec3(0.0, -1.0, 0.0), Blocks.WATER_CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("timewarp/" + BuiltInRegistries.ITEM.getKey(item1.asItem()).getPath()));
    }

    /**
     * 需要熔融宝石的时移配方
     */
    public static void timeWarpWithMeltGem(ItemLike item, ItemLike item1) {
        if (TimeWarpRecipesLoader.provider == null) return;
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.TIMEWARP)
            .icon(item1)
            .hasBlock(
                ModBlocks.CORRUPTED_BEACON.get(),
                new Vec3(0.0, -2.0, 0.0),
                Map.entry(CorruptedBeaconBlock.LIT, true)
            )
            .hasBlockIngredient(
                new Vec3(0, -1, 0),
                ModBlocks.MELT_GEM_CAULDRON.get()
            )
            .hasItemIngredient(new Vec3(0, -1, 0), item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1)
            .setBlock(new Vec3(0, -1, 0), Blocks.CAULDRON)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("timewarp/" + BuiltInRegistries.ITEM.getKey(item1.asItem()).getPath()));
    }

    private static void timeWarpToOilCauldron(RecipeItem... recipeItems) {
        timeWarp(
                RecipeBlock.of(Blocks.CAULDRON),
                recipeItems,
                new RecipeResult[]{RecipeResult.of(
                        RecipeBlock.of(
                                ModBlocks.OIL_CAULDRON.getDefaultState()
                                        .setValue(LayeredCauldronBlock.LEVEL, 1))
                                .setKey(ModBlocks.OIL_CAULDRON.getId().getPath()),
                        Items.CAULDRON)},
                "0");
        timeWarp(
                RecipeBlock.of(ModBlocks.OIL_CAULDRON.get(), Map.entry(LayeredCauldronBlock.LEVEL, 1)),
                recipeItems,
                new RecipeResult[]{RecipeResult.of(
                        RecipeBlock.of(
                                ModBlocks.OIL_CAULDRON.getDefaultState()
                                        .setValue(LayeredCauldronBlock.LEVEL, 2))
                                .setKey(ModBlocks.OIL_CAULDRON.getId().getPath()),
                        Items.CAULDRON)},
                "1");
        timeWarp(
                RecipeBlock.of(ModBlocks.OIL_CAULDRON.get(), Map.entry(LayeredCauldronBlock.LEVEL, 2))
                        .setKey(ModBlocks.OIL_CAULDRON.getId().getPath()),
                recipeItems,
                new RecipeResult[]{RecipeResult.of(
                        RecipeBlock.of(
                                ModBlocks.OIL_CAULDRON.getDefaultState()
                                        .setValue(LayeredCauldronBlock.LEVEL, 3))
                                .setKey(ModBlocks.OIL_CAULDRON.getId().getPath()),
                        Items.CAULDRON)},
                "2");
    }

    private static void timeWarpToEmberMetalNugget(RecipeItem recipeItem) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .type(AnvilRecipeType.TIMEWARP)
                .icon(ModItems.EMBER_METAL_NUGGET)
                .hasBlock(
                        ModBlocks.CORRUPTED_BEACON.get(),
                        new Vec3(0.0, -2.0, 0.0),
                        Map.entry(CorruptedBeaconBlock.LIT, true)
                )
                .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), ModBlocks.FIRE_CAULDRON.get(), 1)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), recipeItem)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.EMBER_METAL_NUGGET, 4)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), 0.5d, ModItems.EMBER_METAL_NUGGET, 4)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), 0.25d, ModItems.EMBER_METAL_NUGGET, 4)
                .unlockedBy(AnvilCraftDatagen.hasItem(recipeItem), AnvilCraftDatagen.has(recipeItem))
                .save(provider, AnvilCraft.of("timewarp/"
                        + "ember_metal_nugget_from_"
                        + recipeItem.getKey()));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .type(AnvilRecipeType.TIMEWARP)
                .icon(ModItems.EMBER_METAL_NUGGET)
                .hasBlock(
                        ModBlocks.CORRUPTED_BEACON.get(),
                        new Vec3(0.0, -2.0, 0.0),
                        Map.entry(CorruptedBeaconBlock.LIT, true)
                )
                .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), ModBlocks.FIRE_CAULDRON.get(), 1)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), recipeItem)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItems.EARTH_CORE_SHARD)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.EMBER_METAL_NUGGET, 9)
                .unlockedBy(AnvilCraftDatagen.hasItem(recipeItem), AnvilCraftDatagen.has(recipeItem))
                .save(provider, AnvilCraft.of("timewarp/"
                        + "ember_metal_nugget_from_"
                        + recipeItem.getKey())
                        + "_1");
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .type(AnvilRecipeType.TIMEWARP)
                .icon(ModItems.EMBER_METAL_NUGGET)
                .hasBlock(
                        ModBlocks.CORRUPTED_BEACON.get(),
                        new Vec3(0.0, -2.0, 0.0),
                        Map.entry(CorruptedBeaconBlock.LIT, true)
                )
                .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), ModBlocks.FIRE_CAULDRON.get(), 1)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), recipeItem)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 2, ModItems.EARTH_CORE_SHARD)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.EMBER_METAL_NUGGET, 12)
                .unlockedBy(AnvilCraftDatagen.hasItem(recipeItem), AnvilCraftDatagen.has(recipeItem))
                .save(provider, AnvilCraft.of("timewarp/"
                        + "ember_metal_nugget_from_"
                        + recipeItem.getKey())
                        + "_2");
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .type(AnvilRecipeType.TIMEWARP)
                .icon(ModItems.EMBER_METAL_NUGGET)
                .hasBlock(
                        ModBlocks.CORRUPTED_BEACON.get(),
                        new Vec3(0.0, -2.0, 0.0),
                        Map.entry(CorruptedBeaconBlock.LIT, true)
                )
                .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), ModBlocks.FIRE_CAULDRON.get(), 1)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), recipeItem)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 3, ModItems.EARTH_CORE_SHARD)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.EMBER_METAL_NUGGET, 15)
                .unlockedBy(AnvilCraftDatagen.hasItem(recipeItem), AnvilCraftDatagen.has(recipeItem))
                .save(provider, AnvilCraft.of("timewarp/"
                        + "ember_metal_nugget_from_"
                        + recipeItem.getKey())
                        + "_3");
    }
}
