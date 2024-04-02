package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class SmashRecipesGenerator {
    private SmashRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        smash(Items.HEART_OF_THE_SEA, ModItems.SEED_OF_THE_SEA.get(), 4, exporter);
        smash(Items.WET_SPONGE, ModItems.SPONGE_GEMMULE.get(), 2, exporter);
        smash(Items.NETHER_STAR, ModItems.NETHER_STAR_SHARD.get(), 4, exporter);
        smash(ModBlocks.HOLLOW_MAGNET_BLOCK.asItem(), ModItems.MAGNET_INGOT.get(), 8, exporter);
        smash(ModBlocks.MAGNET_BLOCK.asItem(), ModItems.MAGNET_INGOT.get(), 9, exporter);
        smash(Items.MELON, Items.MELON_SLICE, 9, exporter);
        smash(Items.SNOW_BLOCK, Items.SNOWBALL, 4, exporter);
        smash(Items.CLAY, Items.CLAY_BALL, 4, exporter);
        smash(Items.PRISMARINE, Items.PRISMARINE_SHARD, 4, exporter);
        smash(Items.PRISMARINE_BRICKS, Items.PRISMARINE_SHARD, 9, exporter);
        smash(Items.GLOWSTONE, Items.GLOWSTONE_DUST, 4, exporter);
        smash(Items.QUARTZ_BLOCK, Items.QUARTZ, 4, exporter);
        smash(Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE, 4, exporter);
        smash(Items.AMETHYST_BLOCK, Items.AMETHYST_SHARD, 4, exporter);
        smash(Items.HONEYCOMB_BLOCK, Items.HONEYCOMB, 4, exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.IRON_TRAPDOOR)
                .hasItemIngredient(Items.HONEY_BLOCK)
                .hasItemIngredient(4, Items.GLASS_BOTTLE)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), Items.HONEY_BOTTLE, 4)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEY_BLOCK), FabricRecipeProvider.has(Items.HONEY_BLOCK))
                .save(exporter, AnvilCraft.of("smash/" + BuiltInRegistries.ITEM.getKey(Items.HONEY_BLOCK).getPath() + "_2_" + BuiltInRegistries.ITEM.getKey(Items.HONEY_BOTTLE).getPath()));
    }

    public static void smash(Item item, @NotNull Item item1, int count, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.IRON_TRAPDOOR)
                .hasItemIngredient(item)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), item1, count)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("smash/" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_2_" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
