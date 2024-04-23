package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class TimeWarpRecipesLoader {
    /**
     * 初始化时移配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        timeWarpWithWater(ModItems.SEA_HEART_SHELL_SHARD.get(), ModItems.SEA_HEART_SHELL.get(), provider);
        timeWarp(ModItems.RESIN.get(), ModItems.AMBER.get(), 1, provider);
        timeWarp(ModBlocks.RESIN_BLOCK.asItem(), ModBlocks.AMBER_BLOCK.asItem(), 1, provider);
        timeWarp(Items.OBSIDIAN, Items.CRYING_OBSIDIAN, 1, provider);
        timeWarp(Items.CHARCOAL, Items.COAL, 1, provider);
        timeWarp(Items.SAND, Items.DIRT, 1, provider);
        timeWarp(Items.IRON_BLOCK, Items.RAW_IRON, 3, provider);
        timeWarp(Items.GOLD_BLOCK, Items.RAW_GOLD, 3, provider);
        timeWarp(Items.COPPER_BLOCK, Items.RAW_COPPER, 3, provider);
        timeWarp(ModItems.GEODE.get(), Items.BUDDING_AMETHYST, 1, provider);
        timeWarp(ModBlocks.CINERITE.asItem(), Items.TUFF, 1, provider);
    }

    private static void timeWarp(Item item, Item item1, int count, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(item1)
            .hasBlock(ModBlocks.CORRUPTED_BEACON.get(), new Vec3(0.0, -2.0, 0.0))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1, count)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("timewarp/" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }

    private static void timeWarpWithWater(Item item, Item item1, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(item1)
            .hasBlock(ModBlocks.CORRUPTED_BEACON.get(), new Vec3(0.0, -2.0, 0.0))
            .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), Blocks.WATER_CAULDRON, 3)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("timewarp/" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
