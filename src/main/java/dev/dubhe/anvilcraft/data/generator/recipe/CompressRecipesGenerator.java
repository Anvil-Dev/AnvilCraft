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

public abstract class CompressRecipesGenerator {
    private CompressRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        ItemAnvilRecipeBuilder.item(RecipeCategory.TOOLS, ModItems.INTERACT_MACHINE)
                .requires(Items.ANVIL)
                .requires(Items.DISPENSER)
                .component(Blocks.CAULDRON)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ANVIL), FabricRecipeProvider.has(Items.ANVIL))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DISPENSER), FabricRecipeProvider.has(Items.DISPENSER))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.TOOLS, ModItems.AUTO_CRAFTER)
                .requires(Items.ANVIL)
                .requires(Items.CRAFTING_TABLE, 8)
                .component(Blocks.CAULDRON)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ANVIL), FabricRecipeProvider.has(Items.ANVIL))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DISPENSER), FabricRecipeProvider.has(Items.DISPENSER))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.TOOLS, Items.BONE_BLOCK)
                .requires(Items.BONE, 3)
                .component(Blocks.CAULDRON)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BONE), FabricRecipeProvider.has(Items.BONE))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.CAULDRON)
                .component(BlockTags.CAMPFIRES)
                .result(ModItems.ROYAL_STEEL_INGOT.getDefaultInstance())
                .requires(Items.IRON_INGOT)
                .requires(Items.EMERALD)
                .requires(Items.DIAMOND)
                .requires(Items.AMETHYST_SHARD)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.EMERALD), FabricRecipeProvider.has(Items.EMERALD))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DIAMOND), FabricRecipeProvider.has(Items.DIAMOND))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.AMETHYST_SHARD), FabricRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(exporter, AnvilCraft.of("craft_a_royal_steel_ingot"));
    }
}
