package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
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
    /**
     * 初始化时移配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        timeWarpWithWater(ModItems.SEA_HEART_SHELL_SHARD, ModItems.SEA_HEART_SHELL, provider);
        timeWarp(ModItems.RESIN, ModItems.AMBER, 1, provider);
        timeWarp(Items.OBSIDIAN, Items.CRYING_OBSIDIAN, 1, provider);
        timeWarp(Items.CHARCOAL, Items.COAL, 1, provider);
        timeWarp(Items.SAND, Items.DIRT, 1, provider);
        timeWarp(Items.IRON_BLOCK, Items.RAW_IRON, 3, provider);
        timeWarp(Items.GOLD_BLOCK, Items.RAW_GOLD, 3, provider);
        timeWarp(Items.COPPER_BLOCK, Items.RAW_COPPER, 3, provider);
        timeWarp(ModItems.GEODE, Items.BUDDING_AMETHYST, 1, provider);
        timeWarp(ModBlocks.CINERITE, Items.TUFF, 1, provider);
        timeWarp(ModBlocks.NETHER_DUST, Items.SOUL_SOIL, 1, provider);
        timeWarp(ModBlocks.END_DUST, Items.END_STONE, 1, provider);

        timeWarpWithMeltGem(Items.EMERALD, Items.EMERALD_BLOCK, provider);
        timeWarpWithMeltGem(ModItems.RUBY, ModBlocks.RUBY_BLOCK, provider);
        timeWarpWithMeltGem(ModItems.TOPAZ, ModBlocks.TOPAZ_BLOCK, provider);
        timeWarpWithMeltGem(ModItems.SAPPHIRE, ModBlocks.SAPPHIRE_BLOCK, provider);
    }

    /**
     * 时移配方
     */
    public static void timeWarp(ItemLike item, ItemLike item1, int count, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(item1)
            .hasBlock(
                ModBlocks.CORRUPTED_BEACON.get(),
                new Vec3(0.0, -2.0, 0.0),
                Map.entry(CorruptedBeaconBlock.LIT, true)
            )
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1, count)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("timewarp/" + BuiltInRegistries.ITEM.getKey(item1.asItem()).getPath()));
    }

    /**
     * 需水时移配方
     */
    public static void timeWarpWithWater(ItemLike item, ItemLike item1, RegistrateRecipeProvider provider) {
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
    public static void timeWarpWithMeltGem(ItemLike item, ItemLike item1, RegistrateRecipeProvider provider) {
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
