package dev.dubhe.anvilcraft.data;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.anvil.block.BlockAnvilRecipeBuilder;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipeBuilder;
import dev.dubhe.anvilcraft.data.recipe.crafting_table.ShapedTagRecipeBuilder;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.*;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MyRecipesGenerator extends FabricRecipeProvider {
    public MyRecipesGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> exporter) {
        // 工作台配方
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FERRITE_CORE_MAGNET_BLOCK)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', ModItems.MAGNET_INGOT)
                .define('B', Items.IRON_INGOT)
                .unlockedBy(hasItem(ModItems.MAGNET_INGOT), FabricRecipeProvider.has(ModItems.MAGNET_INGOT))
                .unlockedBy(hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MAGNET)
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" A ")
                .define('A', Items.REDSTONE)
                .define('B', ModItems.MAGNET_INGOT)
                .define('C', Items.ENDER_PEARL)
                .unlockedBy(hasItem(Items.REDSTONE), FabricRecipeProvider.has(Items.REDSTONE))
                .unlockedBy(hasItem(ModItems.MAGNET_INGOT), FabricRecipeProvider.has(ModItems.MAGNET_INGOT))
                .unlockedBy(hasItem(Items.ENDER_PEARL), FabricRecipeProvider.has(Items.ENDER_PEARL))
                .save(exporter);
        ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.AMETHYST_PICKAXE)
                .pattern("AAA")
                .pattern(" B ")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.BAMBOO)
                .unlockedBy(hasItem(Items.AMETHYST_SHARD), FabricRecipeProvider.has(Items.AMETHYST_SHARD))
                .unlockedBy(hasItem(Items.BAMBOO), FabricRecipeProvider.has(Items.BAMBOO))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NETHERITE_CORE)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', ModItems.DEBRIS_SCRAP)
                .define('B', ModItems.NETHER_STAR_SHARD)
                .unlockedBy(hasItem(ModItems.DEBRIS_SCRAP), FabricRecipeProvider.has(ModItems.DEBRIS_SCRAP))
                .unlockedBy(hasItem(ModItems.NETHER_STAR_SHARD), FabricRecipeProvider.has(ModItems.NETHER_STAR_SHARD))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NETHERITE_COIL)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                .define('B', ModItems.NETHERITE_CORE)
                .unlockedBy(hasItem(Items.LIGHT_WEIGHTED_PRESSURE_PLATE), FabricRecipeProvider.has(Items.LIGHT_WEIGHTED_PRESSURE_PLATE))
                .unlockedBy(hasItem(ModItems.NETHERITE_CORE), FabricRecipeProvider.has(ModItems.NETHERITE_CORE))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ELYTRA_MEMBRANE)
                .pattern("AB")
                .pattern("BB")
                .define('A', ModItems.ELYTRA_FRAME)
                .define('B', Items.PHANTOM_MEMBRANE)
                .unlockedBy(hasItem(ModItems.ELYTRA_FRAME), FabricRecipeProvider.has(ModItems.ELYTRA_FRAME))
                .unlockedBy(hasItem(Items.PHANTOM_MEMBRANE), FabricRecipeProvider.has(Items.PHANTOM_MEMBRANE))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.ELYTRA)
                .pattern("ABA")
                .pattern("C C")
                .pattern("C C")
                .define('A', ModItems.ELYTRA_MEMBRANE)
                .define('B', Items.STRING)
                .define('C', Items.FEATHER)
                .unlockedBy(hasItem(ModItems.ELYTRA_MEMBRANE), FabricRecipeProvider.has(ModItems.ELYTRA_MEMBRANE))
                .unlockedBy(hasItem(Items.STRING), FabricRecipeProvider.has(Items.STRING))
                .unlockedBy(hasItem(Items.FEATHER), FabricRecipeProvider.has(Items.FEATHER))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.TRIDENT)
                .pattern(" AA")
                .pattern(" BA")
                .pattern("B  ")
                .define('A', ModItems.BLADE_OF_THE_SEA)
                .define('B', Items.PRISMARINE_SHARD)
                .unlockedBy(hasItem(ModItems.BLADE_OF_THE_SEA), FabricRecipeProvider.has(ModItems.BLADE_OF_THE_SEA))
                .unlockedBy(hasItem(Items.PRISMARINE_SHARD), FabricRecipeProvider.has(Items.PRISMARINE_SHARD))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.PROTOCOL_EMPTY)
                .requires(Items.PAPER)
                .requires(Items.FEATHER)
                .requires(Items.GLOW_INK_SAC)
                .unlockedBy(hasItem(Items.PAPER), FabricRecipeProvider.has(Items.PAPER))
                .unlockedBy(hasItem(Items.FEATHER), FabricRecipeProvider.has(Items.FEATHER))
                .unlockedBy(hasItem(Items.GLOW_INK_SAC), FabricRecipeProvider.has(Items.GLOW_INK_SAC))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PROTOCOL_ABSORB)
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .define('C', ModItems.PROTOCOL_EMPTY)
                .unlockedBy(hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .unlockedBy(hasItem(Items.HEAVY_WEIGHTED_PRESSURE_PLATE), FabricRecipeProvider.has(Items.HEAVY_WEIGHTED_PRESSURE_PLATE))
                .unlockedBy(hasItem(ModItems.PROTOCOL_EMPTY), FabricRecipeProvider.has(ModItems.PROTOCOL_EMPTY))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PROTOCOL_REPAIR)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.GLASS_BOTTLE)
                .define('B', ModItems.PROTOCOL_EMPTY)
                .unlockedBy(hasItem(Items.GLASS_BOTTLE), FabricRecipeProvider.has(Items.GLASS_BOTTLE))
                .unlockedBy(hasItem(ModItems.PROTOCOL_EMPTY), FabricRecipeProvider.has(ModItems.PROTOCOL_EMPTY))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PROTOCOL_RESTOCK)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.IRON_INGOT)
                .define('B', ModItems.PROTOCOL_EMPTY)
                .unlockedBy(hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .unlockedBy(hasItem(ModItems.PROTOCOL_EMPTY), FabricRecipeProvider.has(ModItems.PROTOCOL_EMPTY))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PROTOCOL_PROTECT)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.SHIELD)
                .define('B', ModItems.PROTOCOL_EMPTY)
                .unlockedBy(hasItem(Items.SHIELD), FabricRecipeProvider.has(Items.SHIELD))
                .unlockedBy(hasItem(ModItems.PROTOCOL_EMPTY), FabricRecipeProvider.has(ModItems.PROTOCOL_EMPTY))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.COCOA_BUTTER)
                .requires(ModItems.COCOA_POWDER)
                .requires(ModItems.COCOA_POWDER)
                .requires(ModItems.COCOA_LIQUOR)
                .unlockedBy(hasItem(ModItems.COCOA_POWDER), FabricRecipeProvider.has(ModItems.COCOA_POWDER))
                .unlockedBy(hasItem(ModItems.COCOA_LIQUOR), FabricRecipeProvider.has(ModItems.COCOA_LIQUOR))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHOCOLATE)
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', ModItems.COCOA_LIQUOR)
                .define('B', ModItems.COCOA_BUTTER)
                .define('C', ModItems.CREAM)
                .define('D', Items.SUGAR)
                .unlockedBy(hasItem(ModItems.COCOA_LIQUOR), FabricRecipeProvider.has(ModItems.COCOA_LIQUOR))
                .unlockedBy(hasItem(ModItems.COCOA_BUTTER), FabricRecipeProvider.has(ModItems.COCOA_BUTTER))
                .unlockedBy(hasItem(ModItems.CREAM), FabricRecipeProvider.has(ModItems.CREAM))
                .unlockedBy(hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHOCOLATE_WHITE)
                .pattern("AAA")
                .pattern("BCB")
                .pattern("AAA")
                .define('A', ModItems.COCOA_BUTTER)
                .define('B', ModItems.CREAM)
                .define('C', Items.SUGAR)
                .unlockedBy(hasItem(ModItems.COCOA_BUTTER), FabricRecipeProvider.has(ModItems.COCOA_BUTTER))
                .unlockedBy(hasItem(ModItems.CREAM), FabricRecipeProvider.has(ModItems.CREAM))
                .unlockedBy(hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHOCOLATE_BLACK)
                .pattern("AAA")
                .pattern("BCB")
                .pattern("AAA")
                .define('A', ModItems.COCOA_LIQUOR)
                .define('B', ModItems.COCOA_BUTTER)
                .define('C', Items.SUGAR)
                .unlockedBy(hasItem(ModItems.COCOA_LIQUOR), FabricRecipeProvider.has(ModItems.COCOA_LIQUOR))
                .unlockedBy(hasItem(ModItems.COCOA_BUTTER), FabricRecipeProvider.has(ModItems.COCOA_BUTTER))
                .unlockedBy(hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_STEEL_BLOCK)
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy(hasItem(ModItems.ROYAL_STEEL_INGOT), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ROYAL_STEEL_INGOT, 9)
                .requires(ModItems.ROYAL_STEEL_BLOCK)
                .unlockedBy(hasItem(ModItems.ROYAL_STEEL_BLOCK), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_BLOCK))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_ANVIL)
                .pattern("AAA")
                .pattern(" B ")
                .pattern("BBB")
                .define('A', ModItems.ROYAL_STEEL_BLOCK)
                .define('B', ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy(hasItem(ModItems.ROYAL_STEEL_BLOCK), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_BLOCK))
                .unlockedBy(hasItem(ModItems.ROYAL_STEEL_INGOT), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.CREAMY_BREAD_ROLL)
                .requires(Items.BREAD)
                .requires(ModItems.CREAM)
                .requires(Items.SUGAR)
                .unlockedBy(hasItem(Items.BREAD), FabricRecipeProvider.has(Items.BREAD))
                .unlockedBy(hasItem(ModItems.CREAM), FabricRecipeProvider.has(ModItems.CREAM))
                .unlockedBy(hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.SWEET_DUMPLING_RAW)
                .requires(Items.COCOA_BEANS)
                .requires(ModItems.FLATDOUGH)
                .requires(Items.SUGAR)
                .unlockedBy(hasItem(Items.COCOA_BEANS), FabricRecipeProvider.has(Items.COCOA_BEANS))
                .unlockedBy(hasItem(ModItems.FLATDOUGH), FabricRecipeProvider.has(ModItems.FLATDOUGH))
                .unlockedBy(hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.DUMPLING_RAW)
                .requires(ModItems.MEAT_STUFFING)
                .requires(ModItems.FLATDOUGH)
                .unlockedBy(hasItem(ModItems.MEAT_STUFFING), FabricRecipeProvider.has(ModItems.MEAT_STUFFING))
                .unlockedBy(hasItem(ModItems.FLATDOUGH), FabricRecipeProvider.has(ModItems.FLATDOUGH))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.SHENGJIAN_RAW)
                .requires(ModItems.MEAT_STUFFING)
                .requires(ModItems.FLATDOUGH)
                .requires(ModItems.GREASE)
                .unlockedBy(hasItem(ModItems.MEAT_STUFFING), FabricRecipeProvider.has(ModItems.MEAT_STUFFING))
                .unlockedBy(hasItem(ModItems.FLATDOUGH), FabricRecipeProvider.has(ModItems.FLATDOUGH))
                .unlockedBy(hasItem(ModItems.GREASE), FabricRecipeProvider.has(ModItems.GREASE))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.MEATBALLS_RAW)
                .requires(ModItems.MEAT_STUFFING)
                .requires(ModItems.FLOUR)
                .requires(ModItems.GREASE)
                .unlockedBy(hasItem(ModItems.MEAT_STUFFING), FabricRecipeProvider.has(ModItems.MEAT_STUFFING))
                .unlockedBy(hasItem(ModItems.FLOUR), FabricRecipeProvider.has(ModItems.FLOUR))
                .unlockedBy(hasItem(ModItems.GREASE), FabricRecipeProvider.has(ModItems.GREASE))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.BEEF_MUSHROOM_STEW_RAW)
                .requires(Items.BEEF)
                .requires(Items.BROWN_MUSHROOM)
                .requires(Items.RED_MUSHROOM)
                .requires(Items.BOWL)
                .unlockedBy(hasItem(Items.BEEF), FabricRecipeProvider.has(Items.BEEF))
                .unlockedBy(hasItem(Items.BROWN_MUSHROOM), FabricRecipeProvider.has(Items.BROWN_MUSHROOM))
                .unlockedBy(hasItem(Items.RED_MUSHROOM), FabricRecipeProvider.has(Items.RED_MUSHROOM))
                .unlockedBy(hasItem(Items.BOWL), FabricRecipeProvider.has(Items.BOWL))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.UTUSAN_RAW)
                .requires(Items.SPIDER_EYE)
                .requires(Items.PUFFERFISH)
                .requires(Items.POISONOUS_POTATO)
                .requires(Items.LILY_OF_THE_VALLEY)
                .requires(Items.WITHER_ROSE)
                .unlockedBy(hasItem(Items.SPIDER_EYE), FabricRecipeProvider.has(Items.SPIDER_EYE))
                .unlockedBy(hasItem(Items.PUFFERFISH), FabricRecipeProvider.has(Items.PUFFERFISH))
                .unlockedBy(hasItem(Items.POISONOUS_POTATO), FabricRecipeProvider.has(Items.POISONOUS_POTATO))
                .unlockedBy(hasItem(Items.LILY_OF_THE_VALLEY), FabricRecipeProvider.has(Items.LILY_OF_THE_VALLEY))
                .unlockedBy(hasItem(Items.WITHER_ROSE), FabricRecipeProvider.has(Items.WITHER_ROSE))
                .save(exporter);
        // 烹饪配方
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD, Items.BREAD, 0.35f, 600)
                .unlockedBy(hasItem(ModItems.DOUGH), FabricRecipeProvider.has(ModItems.DOUGH))
                .save(exporter, AnvilCraft.of("campfire_cooking_bread"));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD, Items.BREAD, 0.35f, 100)
                .unlockedBy(hasItem(ModItems.DOUGH), FabricRecipeProvider.has(ModItems.DOUGH))
                .save(exporter, AnvilCraft.of("smoking_bread"));
        SimpleCookingRecipeBuilder.generic(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD, Items.BREAD, 0.35f, 600, RecipeSerializer.SMOKING_RECIPE)
                .unlockedBy(hasItem(ModItems.DOUGH), FabricRecipeProvider.has(ModItems.DOUGH))
                .save(exporter, AnvilCraft.of("generic_cooking_bread"));
        // 切石机配方
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_STAIRS, ModBlocks.CUT_ROYAL_STEEL_BLOCK);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_SLAB, ModBlocks.CUT_ROYAL_STEEL_BLOCK, 2);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_BLOCK, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_STAIRS, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_SLAB, ModBlocks.ROYAL_STEEL_BLOCK, 8);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK, ModBlocks.ROYAL_STEEL_BLOCK, 4);

        // 铁砧物品处理
        ItemAnvilRecipeBuilder.item(RecipeCategory.TOOLS, ModItems.INTERACT_MACHINE)
                .requires(Items.ANVIL)
                .requires(Items.DISPENSER)
                .component(Blocks.CAULDRON)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(hasItem(Items.ANVIL), FabricRecipeProvider.has(Items.ANVIL))
                .unlockedBy(hasItem(Items.DISPENSER), FabricRecipeProvider.has(Items.DISPENSER))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.TOOLS, ModItems.CRAFTING_MACHINE)
                .requires(Items.ANVIL)
                .requires(Items.CRAFTING_TABLE)
                .component(Blocks.CAULDRON)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(hasItem(Items.ANVIL), FabricRecipeProvider.has(Items.ANVIL))
                .unlockedBy(hasItem(Items.DISPENSER), FabricRecipeProvider.has(Items.DISPENSER))
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
                .unlockedBy(hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .unlockedBy(hasItem(Items.EMERALD), FabricRecipeProvider.has(Items.EMERALD))
                .unlockedBy(hasItem(Items.DIAMOND), FabricRecipeProvider.has(Items.DIAMOND))
                .unlockedBy(hasItem(Items.AMETHYST_SHARD), FabricRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(exporter, AnvilCraft.of("craft_a_royal_steel_ingot"));
        // 粉碎
        smash(ModItems.HOLLOW_MAGNET_BLOCK, ModItems.MAGNET_INGOT, 8, exporter);
        smash(ModItems.MAGNET_BLOCK, ModItems.MAGNET_INGOT, 9, exporter);
        smash(Items.NETHER_STAR, ModItems.NETHER_STAR_SHARD, 4, exporter);
        smash(Items.HEART_OF_THE_SEA, ModItems.SEED_OF_THE_SEA, 4, exporter);
        smash(Items.IRON_BLOCK, Items.IRON_INGOT, 9, exporter);
        smash(Items.IRON_INGOT, Items.IRON_NUGGET, 9, exporter);
        smash(Items.GOLD_BLOCK, Items.GOLD_INGOT, 9, exporter);
        smash(Items.GOLD_INGOT, Items.GOLD_NUGGET, 9, exporter);
        smash(Items.COPPER_BLOCK, Items.COPPER_INGOT, 9, exporter);
        smash(Items.NETHERITE_BLOCK, Items.NETHERITE_INGOT, 9, exporter);
        smash(Items.RAW_IRON_BLOCK, Items.RAW_IRON, 9, exporter);
        smash(Items.RAW_GOLD_BLOCK, Items.RAW_GOLD, 9, exporter);
        smash(Items.RAW_COPPER_BLOCK, Items.RAW_COPPER, 9, exporter);
        smash(Items.REDSTONE_BLOCK, Items.REDSTONE, 9, exporter);
        smash(Items.COAL_BLOCK, Items.COAL, 9, exporter);
        smash(Items.LAPIS_BLOCK, Items.LAPIS_LAZULI, 9, exporter);
        smash(Items.EMERALD_BLOCK, Items.EMERALD, 9, exporter);
        smash(Items.DIAMOND_BLOCK, Items.DIAMOND, 9, exporter);
        smash(Items.SLIME_BLOCK, Items.SLIME_BALL, 9, exporter);
        smash(Items.MELON, Items.MELON_SLICE, 9, exporter);
        smash(Items.HAY_BLOCK, Items.WHEAT, 9, exporter);
        smash(Items.BONE_BLOCK, Items.BONE_MEAL, 9, exporter);
        smash(Items.BONE, Items.BONE_MEAL, 3, exporter);
        smash(Items.DRIED_KELP_BLOCK, Items.DRIED_KELP, 9, exporter);
        smash(Items.SNOW_BLOCK, Items.SNOWBALL, 4, exporter);
        smash(Items.CLAY, Items.CLAY_BALL, 4, exporter);
        smash(Items.WHEAT, ModItems.FLOUR, 1, exporter);
        smash(Items.BEEF, ModItems.MEAT_STUFFING, 2, exporter);
        smash(Items.PORKCHOP, ModItems.MEAT_STUFFING, 2, exporter);
        smash(ModItems.ROYAL_STEEL_BLOCK, ModItems.ROYAL_STEEL_INGOT, 9, exporter);
        // 铁砧方块处理
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .component(Blocks.SNOW_BLOCK)
                .component(Blocks.CAULDRON)
                .unlockedBy(hasItem(Items.SNOW), FabricRecipeProvider.has(Items.SNOW))
                .save(exporter, AnvilCraft.of("snow_2_water_cauldron_1"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .component(Blocks.SNOW_BLOCK)
                .component(Component.of(Component.Value.of(Blocks.WATER_CAULDRON).with("level", 1)))
                .unlockedBy(hasItem(Items.SNOW), FabricRecipeProvider.has(Items.SNOW))
                .save(exporter, AnvilCraft.of("snow_2_water_cauldron_2"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
                .component(Blocks.SNOW_BLOCK)
                .component(Component.of(Component.Value.of(Blocks.WATER_CAULDRON).with("level", 2)))
                .unlockedBy(hasItem(Items.SNOW), FabricRecipeProvider.has(Items.SNOW))
                .save(exporter, AnvilCraft.of("snow_2_water_cauldron_3"));
    }

    public static void smash(Item item, Item result, int count, Consumer<FinishedRecipe> exporter) {
        ItemAnvilRecipeBuilder.item(RecipeCategory.MISC, result, count)
                .requires(item)
                .component(Blocks.IRON_TRAPDOOR)
                .location(ItemAnvilRecipe.Location.UP)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, AnvilCraft.of("%s_smash_2_%s".formatted(BuiltInRegistries.ITEM.getKey(item).getPath(), BuiltInRegistries.ITEM.getKey(result).getPath())));
    }

    public static @NotNull String hasItem(@NotNull Item item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
