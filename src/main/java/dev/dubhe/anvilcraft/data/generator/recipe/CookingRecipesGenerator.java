package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipeBuilder;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

public abstract class CookingRecipesGenerator {
    private CookingRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .result(ModItems.FLOUR)
                .requires(Items.WHEAT)
                .component(Blocks.IRON_TRAPDOOR)
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WHEAT), FabricRecipeProvider.has(Items.WHEAT))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .result(ModItems.MEAT_STUFFING, 2)
                .requires(Items.BEEF)
                .component(Blocks.IRON_TRAPDOOR)
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BEEF), FabricRecipeProvider.has(Items.BEEF))
                .save(exporter, AnvilCraft.of("meat_stuffing_1"));
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .result(ModItems.MEAT_STUFFING, 2)
                .requires(Items.PORKCHOP)
                .component(Blocks.IRON_TRAPDOOR)
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.PORKCHOP), FabricRecipeProvider.has(Items.PORKCHOP))
                .save(exporter, AnvilCraft.of("meat_stuffing_2"));
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.WATER_CAULDRON)
                .component(BlockTags.CAMPFIRES)
                .result(ModItems.BEEF_MUSHROOM_STEW)
                .requires(ModItems.BEEF_MUSHROOM_STEW_RAW)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.BEEF_MUSHROOM_STEW_RAW), FabricRecipeProvider.has(ModItems.BEEF_MUSHROOM_STEW_RAW))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.WATER_CAULDRON)
                .component(BlockTags.CAMPFIRES)
                .result(ModItems.DUMPLING, 4)
                .requires(ModItems.DUMPLING_RAW, 4)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.DUMPLING_RAW), FabricRecipeProvider.has(ModItems.DUMPLING_RAW))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.WATER_CAULDRON)
                .component(BlockTags.CAMPFIRES)
                .result(ModItems.SWEET_DUMPLING, 4)
                .requires(ModItems.SWEET_DUMPLING_RAW, 4)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.SWEET_DUMPLING_RAW), FabricRecipeProvider.has(ModItems.SWEET_DUMPLING_RAW))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.CAULDRON)
                .component(BlockTags.CAMPFIRES)
                .result(ModItems.MEATBALLS, 4)
                .requires(ModItems.MEATBALLS_RAW, 4)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.MEATBALLS_RAW), FabricRecipeProvider.has(ModItems.MEATBALLS_RAW))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.CAULDRON)
                .component(BlockTags.CAMPFIRES)
                .result(ModItems.UTUSAN)
                .requires(ModItems.UTUSAN_RAW)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.UTUSAN_RAW), FabricRecipeProvider.has(ModItems.UTUSAN_RAW))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.CAULDRON)
                .component(BlockTags.CAMPFIRES)
                .result(ModItems.SHENGJIAN, 4)
                .requires(ModItems.SHENGJIAN_RAW, 4)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.SHENGJIAN_RAW), FabricRecipeProvider.has(ModItems.SHENGJIAN_RAW))
                .save(exporter);
    }
}
