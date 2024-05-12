package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.data.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
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
        timeWarp(new RecipeItem(ModItems.RESIN), new RecipeItem(ModItems.AMBER));
        timeWarp(new RecipeItem(Items.OBSIDIAN), new RecipeItem(Items.CRYING_OBSIDIAN));
        timeWarp(new RecipeItem(Items.CHARCOAL), new RecipeItem(Items.COAL));
        timeWarp(new RecipeItem(Items.SAND), new RecipeItem(Items.DIRT));
        timeWarp(new RecipeItem(Items.IRON_BLOCK), new RecipeItem(Items.RAW_IRON, 3));
        timeWarp(new RecipeItem(Items.GOLD_BLOCK), new RecipeItem(Items.RAW_GOLD, 3));
        timeWarp(new RecipeItem(Items.COPPER_BLOCK), new RecipeItem(Items.RAW_COPPER, 3));
        timeWarp(new RecipeItem(ModItems.GEODE), new RecipeItem(Items.BUDDING_AMETHYST));
        timeWarp(new RecipeItem(ModBlocks.CINERITE), new RecipeItem(Items.TUFF));
        timeWarp(new RecipeItem(ModBlocks.NETHER_DUST), new RecipeItem(Items.SOUL_SOIL));
        timeWarp(new RecipeItem(ModBlocks.END_DUST), new RecipeItem(Items.END_STONE));
        timeWarp(new RecipeItem(ModItems.LIME_POWDER, 8), new RecipeItem(Items.CALCITE));
        timeWarp(new RecipeItem(ModItems.NETHERITE_CRYSTAL_NUCLEUS), new RecipeItem(Items.ANCIENT_DEBRIS));

        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.WITHER_ROSE)
            .hasBlock(
                    ModBlocks.CORRUPTED_BEACON.get(),
                    new Vec3(0.0, -2.0, 0.0),
                    Map.entry(CorruptedBeaconBlock.LIT, true)
            )
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.ROSE_BUSH)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), 0.2, Items.WITHER_ROSE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ROSE_BUSH), AnvilCraftDatagen.has(Items.ROSE_BUSH))
            .save(provider, AnvilCraft.of("timewarp/"
                    + BuiltInRegistries.ITEM.getKey(Items.WITHER_ROSE).getPath()));

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
            .icon(item1)
            .hasBlock(
                ModBlocks.CORRUPTED_BEACON.get(),
                new Vec3(0.0, -2.0, 0.0),
                Map.entry(CorruptedBeaconBlock.LIT, true)
            )
            .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), Blocks.WATER_CAULDRON, 3)
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
