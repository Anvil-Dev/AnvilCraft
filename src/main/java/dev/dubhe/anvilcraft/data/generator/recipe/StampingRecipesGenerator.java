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
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public abstract class StampingRecipesGenerator {
    private StampingRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        stamping(Items.IRON_INGOT, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, exporter);
        stamping(Items.GOLD_INGOT, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, exporter);
        stamping(Items.SUGAR_CANE, Items.PAPER, exporter);
        stamping(Items.SNOWBALL, Items.SNOW, exporter);
        stamping(ModItems.PULP, Items.PAPER, exporter);
        stamping(Items.MILK_BUCKET, ModItems.CREAM, exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(ModBlocks.STAMPING_PLATFORM)
                .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), Items.COCOA_BEANS)
                .spawnItem(new Vec3(0.0, -0.75, 0.0), ModItems.COCOA_BUTTER)
                .spawnItem(new Vec3(0.0, -0.75, 0.0), ModItems.COCOA_POWDER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.COCOA_BEANS), FabricRecipeProvider.has(Items.COCOA_BEANS))
                .save(exporter, AnvilCraft.of("stamping/cocoa"));
    }

    public static void stamping(Item item, Item item1, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(ModBlocks.STAMPING_PLATFORM)
                .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), item)
                .spawnItem(new Vec3(0.0, -0.75, 0.0), item1)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("stamping/" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_2_" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
