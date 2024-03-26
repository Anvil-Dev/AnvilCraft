package dev.dubhe.anvilcraft.data.generator;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.recipe.*;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MyRecipesGenerator extends FabricRecipeProvider {
    public MyRecipesGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> exporter) {
        VanillaRecipesGenerator.buildRecipes(exporter);
        // 铁砧物品处理
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), BlockTags.CAMPFIRES)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.IRON_INGOT)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.EMERALD)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.DIAMOND)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.AMETHYST_SHARD)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.EMERALD), FabricRecipeProvider.has(Items.EMERALD))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DIAMOND), FabricRecipeProvider.has(Items.DIAMOND))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.AMETHYST_SHARD), FabricRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(exporter, AnvilCraft.of("craft_a_royal_steel_ingot"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.SMITHING_TABLE)
                .hasItem(Items.ANCIENT_DEBRIS)
                .spawnItem(ModItems.DEBRIS_SCRAP)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ANCIENT_DEBRIS), FabricRecipeProvider.has(Items.ANCIENT_DEBRIS))
                .save(exporter);
        ItemPredicate.Builder predicate = ItemPredicate.Builder.item().of(Items.ELYTRA)
                .hasDurability(MinMaxBounds.Ints.atLeast(Items.ELYTRA.getMaxDamage()));
        RecipePredicate hasItemIngredient = new HasItemIngredient(Vec3.ZERO, predicate.build());
        ItemStack result = Items.ELYTRA.getDefaultInstance();
        result.setDamageValue(432);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.SMITHING_TABLE)
                .addPredicates(hasItemIngredient)
                .spawnItem(ModItems.ELYTRA_FRAME)
                .spawnItem(Items.PHANTOM_MEMBRANE)
                .spawnItem(result)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ELYTRA), FabricRecipeProvider.has(Items.ELYTRA))
                .save(exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.SMITHING_TABLE)
                .hasItemIngredient(ModItems.KERNEL_OF_THE_SEA)
                .spawnItem(Items.HEART_OF_THE_SEA)
                .spawnItem(Items.PRISMARINE_SHARD)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.KERNEL_OF_THE_SEA), FabricRecipeProvider.has(ModItems.KERNEL_OF_THE_SEA))
                .save(exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.SOUL_SOIL)
                .hasItemIngredient(ModItems.NETHERITE_COIL)
                .setBlock(Blocks.ANCIENT_DEBRIS)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SOUL_SOIL), FabricRecipeProvider.has(Items.SOUL_SOIL))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.NETHERITE_COIL), FabricRecipeProvider.has(ModItems.NETHERITE_COIL))
                .save(exporter);
        SmashRecipesGenerator.buildRecipes(exporter);
        CookingRecipesGenerator.buildRecipes(exporter);
        CompressRecipesGenerator.buildRecipes(exporter);
        StampingRecipesGenerator.buildRecipes(exporter);
        BulgingAndCrystallizeRecipesGenerator.buildRecipes(exporter);
        // 铁砧方块处理
        SqueezeRecipesGenerator.buildRecipes(exporter);
        SmashBlockRecipesGenerator.buildRecipes(exporter);
        CompactionRecipesGenerator.buildRecipes(exporter);
    }

    public static @NotNull String hasItem(@NotNull Item item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
