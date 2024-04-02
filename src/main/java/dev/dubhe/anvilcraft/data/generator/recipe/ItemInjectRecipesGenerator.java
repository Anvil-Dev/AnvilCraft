package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class ItemInjectRecipesGenerator {
    public ItemInjectRecipesGenerator() {

    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        itemInject(Items.RAW_GOLD_BLOCK, Blocks.DEEPSLATE, Blocks.DEEPSLATE_GOLD_ORE, exporter);
        itemInject(Items.RAW_IRON_BLOCK, Blocks.DEEPSLATE, Blocks.DEEPSLATE_IRON_ORE, exporter);
        itemInject(Items.RAW_COPPER_BLOCK, 3, Blocks.DEEPSLATE, Blocks.DEEPSLATE_COPPER_ORE, exporter);
        itemInject(Items.GOLD_INGOT, 2, Blocks.NETHERRACK, Blocks.NETHER_GOLD_ORE, exporter);
        itemInject(Items.GOLD_INGOT, Blocks.BLACKSTONE, Blocks.GILDED_BLACKSTONE, exporter);
    }

    public static void itemInject(@NotNull Item item, @NotNull Block block, @NotNull Block resBlock, @NotNull Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(new Vec3(0.0, -1.0, 0.0), block)
                .hasItemIngredient(item)
                .setBlock(new Vec3(0.0, -1.0, 0.0), resBlock)
                .unlockedBy(MyRecipesGenerator.hasItem(block.asItem()), FabricRecipeProvider.has(block.asItem()))
                .save(exporter, AnvilCraft.of("item_inject/" + BuiltInRegistries.BLOCK.getKey(resBlock).getPath()));
    }

    public static void itemInject(@NotNull Item item, int count, @NotNull Block block, @NotNull Block resBlock, @NotNull Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(new Vec3(0.0, -1.0, 0.0), block)
                .hasItemIngredient(count, item)
                .setBlock(new Vec3(0.0, -1.0, 0.0), resBlock)
                .unlockedBy(MyRecipesGenerator.hasItem(block.asItem()), FabricRecipeProvider.has(block.asItem()))
                .save(exporter, AnvilCraft.of("item_inject/" + BuiltInRegistries.BLOCK.getKey(resBlock).getPath()));
    }

}
