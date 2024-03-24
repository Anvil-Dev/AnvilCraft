package dev.dubhe.anvilcraft.data.generator;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.recipe.*;
import dev.dubhe.anvilcraft.data.recipe.TagIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipeBuilder;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.nbt.IntTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class MyRecipesGenerator extends FabricRecipeProvider {
    public MyRecipesGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> exporter) {
        VanillaRecipesGenerator.buildRecipes(exporter);
        // 铁砧物品处理
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.CAULDRON)
                .component(BlockTags.CAMPFIRES)
                .result(ModItems.ROYAL_STEEL_INGOT)
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
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.SMITHING_TABLE)
                .requires(Items.ANCIENT_DEBRIS)
                .result(ModItems.DEBRIS_SCRAP, 1)
                .result(Items.ANCIENT_DEBRIS, 1)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ANCIENT_DEBRIS), FabricRecipeProvider.has(Items.ANCIENT_DEBRIS))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .save(exporter);
        TagIngredient ingredient = TagIngredient.of(TagIngredient.Value.of(Items.ELYTRA).with("Damage", IntTag.valueOf(0)));
        ItemStack result = Items.ELYTRA.getDefaultInstance();
        result.setDamageValue(432);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.SMITHING_TABLE)
                .requires(ingredient)
                .result(ModItems.ELYTRA_FRAME, 1)
                .result(Items.PHANTOM_MEMBRANE, 1)
                .result(result)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ELYTRA), FabricRecipeProvider.has(Items.ELYTRA))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.WATER_CAULDRON)
                .requires(ModItems.SEED_OF_THE_SEA)
                .result(ModItems.FRUIT_OF_THE_SEA)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.SEED_OF_THE_SEA), FabricRecipeProvider.has(ModItems.SEED_OF_THE_SEA))
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.WATER_CAULDRON)
                .requires(ModItems.SPONGE_GEMMULE)
                .result(Items.WET_SPONGE)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.SPONGE_GEMMULE), FabricRecipeProvider.has(ModItems.SPONGE_GEMMULE))
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.POWDER_SNOW_CAULDRON)
                .requires(ModItems.SEED_OF_THE_SEA)
                .result(ModItems.TEAR_OF_THE_SEA)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.SEED_OF_THE_SEA), FabricRecipeProvider.has(ModItems.SEED_OF_THE_SEA))
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.CAULDRON)
                .requires(ModItems.FRUIT_OF_THE_SEA)
                .result(ModItems.KERNEL_OF_THE_SEA)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.FRUIT_OF_THE_SEA), FabricRecipeProvider.has(ModItems.FRUIT_OF_THE_SEA))
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.CAULDRON)
                .requires(Items.PRISMARINE_SHARD)
                .requires(ModItems.TEAR_OF_THE_SEA)
                .result(ModItems.BLADE_OF_THE_SEA)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.PRISMARINE_SHARD), FabricRecipeProvider.has(Items.PRISMARINE_SHARD))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.TEAR_OF_THE_SEA), FabricRecipeProvider.has(ModItems.TEAR_OF_THE_SEA))
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.SMITHING_TABLE)
                .requires(ModItems.KERNEL_OF_THE_SEA)
                .result(Items.PRISMARINE_SHARD)
                .result(Items.HEART_OF_THE_SEA)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.KERNEL_OF_THE_SEA), FabricRecipeProvider.has(ModItems.KERNEL_OF_THE_SEA))
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.UP)
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC)
                .component(Blocks.CAULDRON)
                .requires(Items.SOUL_SOIL)
                .requires(ModItems.NETHERITE_COIL)
                .result(Items.ANCIENT_DEBRIS)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SOUL_SOIL), FabricRecipeProvider.has(Items.SOUL_SOIL))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.NETHERITE_COIL), FabricRecipeProvider.has(ModItems.NETHERITE_COIL))
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .save(exporter);
        SmashRecipesGenerator.buildRecipes(exporter);
        CookingRecipesGenerator.buildRecipes(exporter);
        CompressRecipesGenerator.buildRecipes(exporter);
        StampingRecipesGenerator.buildRecipes(exporter);
        BulgingRecipesGenerator.buildRecipes(exporter);
        // 铁砧方块处理
        SqueezeRecipesGenerator.buildRecipes(exporter);
        SmashBlockRecipesGenerator.buildRecipes(exporter);
        CompactionRecipesGenerator.buildRecipes(exporter);
    }

    public static @NotNull String hasItem(@NotNull Item item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
