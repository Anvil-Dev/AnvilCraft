package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class VanillaRecipesLoader {
    /**
     * 原版配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.ELYTRA)
            .pattern("ABA")
            .pattern("C C")
            .pattern("C C")
            .define('A', ModItems.ELYTRA_MEMBRANE)
            .define('B', Items.STRING)
            .define('C', Items.FEATHER)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.ELYTRA_MEMBRANE))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.CHIPPED_ANVIL)
            .pattern("AAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', Items.IRON_BLOCK)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.DAMAGED_ANVIL)
            .pattern("BAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', Items.IRON_BLOCK)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, Items.TRIDENT)
            .pattern(" AA")
            .pattern(" BA")
            .pattern("B  ")
            .define('A', ModItems.PRISMARINE_BLADE)
            .define('B', Items.PRISMARINE_BRICKS)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.PRISMARINE_BLADE))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.HEART_OF_THE_SEA)
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', ModItems.SEA_HEART_SHELL)
            .define('B', ModItems.SAPPHIRE)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.SEA_HEART_SHELL))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CIRCUIT_BOARD)
                .pattern("BCB")
                .pattern("AAA")
                .define('A', ModItems.HARDEND_RESIN)
                .define('B', Items.COPPER_INGOT)
                .define('C', Items.QUARTZ)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.HARDEND_RESIN))
                .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.WET_SPONGE)
                .pattern("AA")
                .pattern("AA")
                .define('A', ModItems.SPONGE_GEMMULE)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.SPONGE_GEMMULE))
                .save(provider);

        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 600)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("campfire_cooking_bread"));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("smoking_bread"));
        SimpleCookingRecipeBuilder.generic(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 600, RecipeSerializer.SMOKING_RECIPE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("generic_cooking_bread"));

        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ModItemTags.DOUGH_FORGE), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 600)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("campfire_cooking_bread_forge"));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItemTags.DOUGH_FORGE), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("smoking_bread_forge"));
        SimpleCookingRecipeBuilder.generic(Ingredient.of(ModItemTags.DOUGH_FORGE), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 600, RecipeSerializer.SMOKING_RECIPE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("generic_cooking_bread_forge"));

        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_STAIRS, ModBlocks.CUT_ROYAL_STEEL_BLOCK);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_SLAB, ModBlocks.CUT_ROYAL_STEEL_BLOCK, 2);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_BLOCK, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_STAIRS, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_SLAB, ModBlocks.ROYAL_STEEL_BLOCK, 8);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK, ModBlocks.ROYAL_STEEL_BLOCK, 4);

        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_PICKAXE_BASE), Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_PICKAXE.get())
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_PICKAXE.asItem()),
                AnvilCraftDatagen.has(ModItems.AMETHYST_PICKAXE))
            .save(provider, AnvilCraft.of("smithing/royal_steel_pickaxe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_AXE_BASE), Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_AXE.get())
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_AXE.asItem()),
                AnvilCraftDatagen.has(ModItems.AMETHYST_AXE))
            .save(provider, AnvilCraft.of("smithing/royal_steel_axe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_HOE_BASE), Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_HOE.get())
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_HOE.asItem()),
                AnvilCraftDatagen.has(ModItems.AMETHYST_HOE))
            .save(provider, AnvilCraft.of("smithing/royal_steel_hoe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_SHOVEL_BASE), Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_SHOVEL.get())
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_SHOVEL.asItem()),
                AnvilCraftDatagen.has(ModItems.AMETHYST_SHOVEL))
            .save(provider, AnvilCraft.of("smithing/royal_steel_shovel"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_SWORD_BASE), Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_SWORD.get())
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_SWORD.asItem()),
                AnvilCraftDatagen.has(ModItems.AMETHYST_SWORD))
            .save(provider, AnvilCraft.of("smithing/royal_steel_sword"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(), Ingredient.of(Items.GRINDSTONE),
                Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.TOOLS, ModBlocks.ROYAL_GRINDSTONE.asItem())
            .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .unlocks(AnvilCraftDatagen.hasItem(Items.GRINDSTONE), AnvilCraftDatagen.has(Items.GRINDSTONE))
            .save(provider, AnvilCraft.of("smithing/royal_grindstone"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(), Ingredient.of(Items.ANVIL),
                Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.TOOLS, ModBlocks.ROYAL_ANVIL.asItem())
            .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .unlocks(AnvilCraftDatagen.hasItem(Items.ANVIL), AnvilCraftDatagen.has(Items.ANVIL))
            .save(provider, AnvilCraft.of("smithing/royal_anvil"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(), Ingredient.of(Items.SMITHING_TABLE),
                Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.TOOLS,
                ModBlocks.ROYAL_SMITHING_TABLE.asItem())
            .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .unlocks(AnvilCraftDatagen.hasItem(Items.SMITHING_TABLE), AnvilCraftDatagen.has(Items.SMITHING_TABLE))
            .save(provider, AnvilCraft.of("smithing/royal_smithing_table"));
    }
}
