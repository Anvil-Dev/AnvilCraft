package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

import java.util.function.Consumer;

public abstract class StampingRecipesGenerator {
    private StampingRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        stamping(Items.IRON_INGOT, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, exporter);
        stamping(Items.GOLD_INGOT, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, exporter);
        stamping(Items.SUGAR_CANE, Items.PAPER, exporter);
        stamping(Items.SNOWBALL, Items.SNOW, exporter);
        stamping(ModItems.DOUGH, ModItems.FLATDOUGH, exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP))
                .hasItemIngredient(Items.COCOA_BEANS)
                .spawnItem(ModItems.COCOA_BUTTER)
                .spawnItem(ModItems.COCOA_POWDER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.COCOA_BEANS), FabricRecipeProvider.has(Items.COCOA_BEANS))
                .save(exporter, AnvilCraft.of("stamping/cocoa"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP))
                .hasItemIngredient(Items.MILK_BUCKET)
                .spawnItem(ModItems.CREAM)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MILK_BUCKET), FabricRecipeProvider.has(Items.MILK_BUCKET))
                .save(exporter, AnvilCraft.of("stamping/cream"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP))
                .hasItemIngredient(Items.MELON_SEEDS)
                .spawnItem(ModItems.GREASE)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MELON_SEEDS), FabricRecipeProvider.has(Items.MELON_SEEDS))
                .save(exporter, AnvilCraft.of("stamping/melon_seeds_2_grease"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP))
                .hasItemIngredient(Items.PUMPKIN_SEEDS)
                .spawnItem(ModItems.GREASE)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.PUMPKIN_SEEDS), FabricRecipeProvider.has(Items.PUMPKIN_SEEDS))
                .save(exporter, AnvilCraft.of("stamping/pumpkin_seeds_2_grease"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP))
                .hasItemIngredient(Items.COOKED_PORKCHOP)
                .spawnItem(ModItems.GREASE, 8)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.COOKED_PORKCHOP), FabricRecipeProvider.has(Items.COOKED_PORKCHOP))
                .save(exporter, AnvilCraft.of("stamping/cooked_porkchop_2_grease"));
    }

    public static void stamping(Item item, Item item1, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP))
                .hasItemIngredient(item)
                .spawnItem(item1)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("stamping/" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
