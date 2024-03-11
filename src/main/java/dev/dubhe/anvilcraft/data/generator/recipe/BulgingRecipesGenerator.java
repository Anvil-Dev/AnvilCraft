package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipeBuilder;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

import java.util.function.Consumer;

public abstract class BulgingRecipesGenerator {
    private BulgingRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        bulging(Items.DIRT, Items.CLAY, exporter);
        bulging(Items.CRIMSON_FUNGUS, Items.NETHER_WART_BLOCK, exporter);
        bulging(Items.WARPED_FUNGUS, Items.WARPED_WART_BLOCK, exporter);
        bulging(Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE, exporter);
        bulging(Items.BRAIN_CORAL, Items.BRAIN_CORAL_BLOCK, exporter);
        bulging(Items.BUBBLE_CORAL, Items.BUBBLE_CORAL_BLOCK, exporter);
        bulging(Items.FIRE_CORAL, Items.FIRE_CORAL_BLOCK, exporter);
        bulging(Items.HORN_CORAL, Items.HORN_CORAL_BLOCK, exporter);
        bulging(Items.TUBE_CORAL, Items.TUBE_CORAL_BLOCK, exporter);
    }

    public static void bulging(Item item, Item item1, Consumer<FinishedRecipe> exporter) {
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, item1)
                .requires(item)
                .component(Component.of(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)))
                //.result(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("bulging/" + BuiltInRegistries.ITEM.getKey(item1).getPath() + "_3"));
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, item1)
                .requires(item)
                .component(Component.of(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2)))
                //.result(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("bulging/" + BuiltInRegistries.ITEM.getKey(item1).getPath() + "_2"));
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, item1)
                .requires(item)
                .component(Component.of(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1)))
                //.result(Blocks.CAULDRON.defaultBlockState())
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("bulging/" + BuiltInRegistries.ITEM.getKey(item1).getPath() + "_1"));
    }
}
