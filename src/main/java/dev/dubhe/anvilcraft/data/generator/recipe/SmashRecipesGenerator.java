package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipeBuilder;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class SmashRecipesGenerator {
    private SmashRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        smash(Items.HEART_OF_THE_SEA, ModItems.SEED_OF_THE_SEA, 4, exporter);
        smash(Items.WET_SPONGE, ModItems.SPONGE_GEMMULE, 2, exporter);
        smash(Items.NETHER_STAR, ModItems.NETHER_STAR_SHARD, 4, exporter);
        smash(ModItems.HOLLOW_MAGNET_BLOCK, ModItems.MAGNET_INGOT, 8, exporter);
        smash(ModItems.MAGNET_BLOCK, ModItems.MAGNET_INGOT, 9, exporter);
    }

    public static void smash(Item item, @NotNull Item item1, int count, Consumer<FinishedRecipe> exporter) {
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .requires(item)
                .result(item1, count)
                .component(Blocks.IRON_TRAPDOOR)
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("smash/" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_2_" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
