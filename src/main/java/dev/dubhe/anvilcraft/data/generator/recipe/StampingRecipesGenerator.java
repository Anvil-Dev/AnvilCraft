package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipeBuilder;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
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
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, ModItems.COCOA_BUTTER)
                .result(ModItems.COCOA_POWDER)
                .requires(Items.COCOA_BEANS)
                .component(Component.of(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP)))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.COCOA_BEANS), FabricRecipeProvider.has(Items.COCOA_BEANS))
                .save(exporter, AnvilCraft.of("stamping/cocoa"));
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, ModItems.CREAM, 4)
                .result(Items.BUCKET)
                .requires(Items.MILK_BUCKET)
                .component(Component.of(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP)))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MILK_BUCKET), FabricRecipeProvider.has(Items.MILK_BUCKET))
                .save(exporter, AnvilCraft.of("stamping/cream"));
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, ModItems.GREASE)
                .requires(Items.MELON_SEEDS)
                .component(Component.of(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP)))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MELON_SEEDS), FabricRecipeProvider.has(Items.MELON_SEEDS))
                .save(exporter, AnvilCraft.of("stamping/melon_seeds_2_grease"));
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, ModItems.GREASE)
                .requires(Items.PUMPKIN_SEEDS)
                .component(Component.of(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP)))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.PUMPKIN_SEEDS), FabricRecipeProvider.has(Items.PUMPKIN_SEEDS))
                .save(exporter, AnvilCraft.of("stamping/pumpkin_seeds_2_grease"));
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, ModItems.GREASE,8)
                .requires(Items.COOKED_PORKCHOP)
                .component(Component.of(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP)))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.COOKED_PORKCHOP), FabricRecipeProvider.has(Items.COOKED_PORKCHOP))
                .save(exporter, AnvilCraft.of("stamping/cooked_porkchop_2_grease"));
    }

    public static void stamping(Item item, Item item1, Consumer<FinishedRecipe> exporter) {
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, item1)
                .requires(item)
                .component(Component.of(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP)))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("stamping/" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
