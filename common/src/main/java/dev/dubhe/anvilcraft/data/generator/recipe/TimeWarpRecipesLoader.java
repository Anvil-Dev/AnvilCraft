package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.data.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
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
        timeWarp(RecipeItem.of(ModItems.RESIN), RecipeItem.of(ModItems.AMBER));
        timeWarp(RecipeItem.of(Items.OBSIDIAN), RecipeItem.of(Items.CRYING_OBSIDIAN));
        timeWarp(RecipeItem.of(Items.CHARCOAL), RecipeItem.of(Items.COAL));
        timeWarp(RecipeItem.of(Items.SAND), RecipeItem.of(Items.DIRT));
        timeWarp(RecipeItem.of(Items.IRON_BLOCK), RecipeItem.of(Items.RAW_IRON, 3));
        timeWarp(RecipeItem.of(Items.GOLD_BLOCK), RecipeItem.of(Items.RAW_GOLD, 3));
        timeWarp(RecipeItem.of(Items.COPPER_BLOCK), RecipeItem.of(Items.RAW_COPPER, 3));
        timeWarp(RecipeItem.of(ModItems.GEODE), RecipeItem.of(Items.BUDDING_AMETHYST));
        timeWarp(RecipeItem.of(ModBlocks.CINERITE), RecipeItem.of(Items.TUFF));
        timeWarp(RecipeItem.of(ModBlocks.NETHER_DUST), RecipeItem.of(Items.SOUL_SOIL));
        timeWarp(RecipeItem.of(ModBlocks.END_DUST), RecipeItem.of(Items.END_STONE));
        timeWarp(RecipeItem.of(ModItems.LIME_POWDER, 8), RecipeItem.of(Items.CALCITE));
        timeWarp(RecipeItem.of(ModItems.NETHERITE_CRYSTAL_NUCLEUS), RecipeItem.of(Items.ANCIENT_DEBRIS));
        timeWarp(RecipeItem.of(ModBlocks.ZINC_BLOCK), RecipeItem.of(ModItems.RAW_ZINC, 3));
        timeWarp(RecipeItem.of(ModBlocks.TIN_BLOCK), RecipeItem.of(ModItems.RAW_TIN, 3));
        timeWarp(RecipeItem.of(ModBlocks.TITANIUM_BLOCK), RecipeItem.of(ModItems.RAW_TITANIUM, 3));
        timeWarp(RecipeItem.of(ModBlocks.TUNGSTEN_BLOCK), RecipeItem.of(ModItems.RAW_TUNGSTEN, 3));
        timeWarp(RecipeItem.of(ModBlocks.LEAD_BLOCK), RecipeItem.of(ModItems.RAW_LEAD, 3));
        timeWarp(RecipeItem.of(ModBlocks.SILVER_BLOCK), RecipeItem.of(ModItems.RAW_SILVER, 3));
        timeWarp(RecipeItem.of(ModBlocks.URANIUM_BLOCK), RecipeItem.of(ModItems.RAW_URANIUM, 3));
        timeWarp(RecipeItem.of(Items.ROSE_BUSH), RecipeItem.of(Items.WITHER_ROSE, 0.2f));
        timeWarpWithMeltGem(Items.EMERALD, Items.EMERALD_BLOCK);
        timeWarpWithMeltGem(ModItems.RUBY, ModBlocks.RUBY_BLOCK);
        timeWarpWithMeltGem(ModItems.TOPAZ, ModBlocks.TOPAZ_BLOCK);
        timeWarpWithMeltGem(ModItems.SAPPHIRE, ModBlocks.SAPPHIRE_BLOCK);
    }

    /**
     * 时移配方
     */
    public static void timeWarp(RecipeItem item, RecipeItem item1) {
        if (TimeWarpRecipesLoader.provider == null) return;
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.TIMEWARP)
            .icon(item1.getItem().asItem())
            .hasBlock(
                ModBlocks.CORRUPTED_BEACON.get(),
                new Vec3(0.0, -2.0, 0.0),
                Map.entry(CorruptedBeaconBlock.LIT, true)
            )
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("timewarp/"
                + BuiltInRegistries.ITEM.getKey(item1.getItem().asItem()).getPath()));
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
}
