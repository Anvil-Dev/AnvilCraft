package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class RegistrumItemRecipeLoader {
    public static <T extends Item> void guideBook(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
            .requires(Ingredient.of(Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL))
            .requires(Items.BOOK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.ANVIL))
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.CHIPPED_ANVIL))
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.DAMAGED_ANVIL))
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.BOOK))
            .save(provider);
    }

    public static <T extends Item> void magnet(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .define('A', Items.ENDER_PEARL)
            .define('B', ModItems.MAGNET_INGOT)
            .define('C', Items.REDSTONE)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.MAGNET_INGOT))
            .save(provider);
    }

    public static <T extends Item> void royalSteelPickaxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_PICKAXE_BASE),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.of("smithing/royal_steel_pickaxe"));
    }

    public static <T extends Item> void royalSteelAxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_AXE_BASE),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.of("smithing/royal_steel_axe"));
    }

    public static <T extends Item> void royalSteelShovel(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_SHOVEL_BASE),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.of("smithing/royal_steel_shovel"));
    }

    public static <T extends Item> void royalSteelHoe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_HOE_BASE),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.of("smithing/royal_steel_hoe"));
    }

    public static <T extends Item> void royalSteelSword(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.ROYAL_STEEL_SWORD_BASE),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.of("smithing/royal_steel_sword"));
    }

    public static <T extends Item> void frostMetalPickaxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.FROST_METAL_PICKAXE_BASE),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/frost_metal_pickaxe"));
    }

    public static <T extends Item> void frostMetalAxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.FROST_METAL_AXE_BASE),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/frost_metal_axe"));
    }

    public static <T extends Item> void frostMetalShovel(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.FROST_METAL_SHOVEL_BASE),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/frost_metal_shovel"));
    }

    public static <T extends Item> void frostMetalHoe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.FROST_METAL_HOE_BASE),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/frost_metal_hoe"));
    }

    public static <T extends Item> void frostMetalSword(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.FROST_METAL_SWORD_BASE),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/frost_metal_sword"));
    }

    public static <T extends Item> void emberMetalPickaxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.EMBER_METAL_PICKAXE_BASE),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/ember_metal_pickaxe"));
    }

    public static <T extends Item> void emberMetalAxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.EMBER_METAL_AXE_BASE),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/ember_metal_axe"));
    }

    public static <T extends Item> void emberMetalShovel(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.EMBER_METAL_SHOVEL_BASE),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/ember_metal_shovel"));
    }

    public static <T extends Item> void emberMetalHoe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.EMBER_METAL_HOE_BASE),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/ember_metal_hoe"));
    }

    public static <T extends Item> void emberMetalSword(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.EMBER_METAL_SWORD_BASE),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/ember_metal_sword"));
    }

    public static <T extends Item> void anvilHammer(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', Items.ANVIL)
            .define('B', Items.LIGHTNING_ROD)
            .define('C', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ANVIL), RegistrumRecipeProvider.has(Items.ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LIGHTNING_ROD), RegistrumRecipeProvider.has(Items.LIGHTNING_ROD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), RegistrumRecipeProvider.has(Items.IRON_INGOT))
            .save(provider);
    }

    public static <T extends Item> void royalAnvilHammer(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.ANVIL_HAMMER),
                Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("smithing/royal_anvil_hammer"));
    }

    public static <T extends Item> void emberAnvilHammer(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.ROYAL_ANVIL_HAMMER),
                Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("smithing/ember_anvil_hammer"));
    }

    public static <T extends Item> void transcendenceAnvilHammer(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.EMBER_ANVIL_HAMMER),
                Ingredient.of(ModBlocks.TRANSCENDIUM_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.TRANSCENDIUM_BLOCK))
            .save(provider, AnvilCraft.of("smithing/transcendence_anvil_hammer"));
    }

    public static <T extends Item> void dragonRod(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), RegistrumRecipeProvider.has(ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ANVIL_HAMMER), RegistrumRecipeProvider.has(ModItems.ANVIL_HAMMER))
            .save(provider);
    }

    public static <T extends Item> void royalDragonRod(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.ROYAL_ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), RegistrumRecipeProvider.has(ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_ANVIL_HAMMER),
                RegistrumRecipeProvider.has(ModItems.ROYAL_ANVIL_HAMMER)
            )
            .save(provider);
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.DRAGON_ROD),
                Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.DRAGON_ROD), AnvilCraftDatagen.has(ModItems.DRAGON_ROD))
            .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK), AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, ctx.getId().withPrefix("smithing/"));
    }

    public static <T extends Item> void emberDragonRod(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.EMBER_ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), AnvilCraftDatagen.has(ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_ANVIL_HAMMER), AnvilCraftDatagen.has(ModItems.EMBER_ANVIL_HAMMER))
            .save(provider);
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModItems.ROYAL_DRAGON_ROD),
            Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
            RecipeCategory.TOOLS,
            ctx.get()
        ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK)).save(provider, ctx.getId().withPrefix("smithing/"));
    }

    public static <T extends Item> void transcendenceDragonRod(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.TRANSCENDENCE_ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), AnvilCraftDatagen.has(ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.TRANSCENDENCE_ANVIL_HAMMER),
                AnvilCraftDatagen.has(ModItems.TRANSCENDENCE_ANVIL_HAMMER)
            )
            .save(provider);
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModItems.EMBER_DRAGON_ROD),
            Ingredient.of(ModBlocks.TRANSCENDIUM_BLOCK),
            RecipeCategory.TOOLS,
            ctx.get()
        ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.TRANSCENDIUM_BLOCK)).save(provider, ctx.getId().withPrefix("smithing/"));
    }

    public static <T extends Item> void energyWeaponPlatform(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ASS")
            .pattern("ASS")
            .pattern("FPC")
            .define('A', Blocks.ANVIL)
            .define('C', ModBlocks.SPACE_OVERCOMPRESSOR)
            .define('F', Blocks.SMITHING_TABLE)
            .define('P', ModItems.PROCESSOR)
            .define('S', ModItems.SUPER_CAPACITOR)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.ANVIL), RegistrumRecipeProvider.has(Blocks.ANVIL))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.SPACE_OVERCOMPRESSOR),
                RegistrumRecipeProvider.has(ModBlocks.SPACE_OVERCOMPRESSOR)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.SMITHING_TABLE), RegistrumRecipeProvider.has(Blocks.SMITHING_TABLE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), RegistrumRecipeProvider.has(ModItems.PROCESSOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SUPER_CAPACITOR), RegistrumRecipeProvider.has(ModItems.SUPER_CAPACITOR))
            .save(provider);
    }

    public static <T extends Item> void spectralSlingshot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("B B")
            .pattern(" C ")
            .define('A', Items.PHANTOM_MEMBRANE)
            .define('B', ModBlocks.SPECTRAL_ANVIL.asItem())
            .define('C', Items.CROSSBOW)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.PHANTOM_MEMBRANE), RegistrumRecipeProvider.has(Items.PHANTOM_MEMBRANE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SPECTRAL_ANVIL), RegistrumRecipeProvider.has(ModBlocks.SPECTRAL_ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.CROSSBOW), RegistrumRecipeProvider.has(Items.CROSSBOW))
            .save(provider);
    }

    public static <T extends Item> void ionocraft(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("BBB")
            .pattern(" C ")
            .define('A', ModItemTags.COPPER_NUGGETS)
            .define('B', Tags.Items.RODS_WOODEN)
            .define('C', ModItemTags.TIN_PLATES)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.COPPER_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.COPPER_NUGGETS))
            .unlockedBy(AnvilCraftDatagen.hasItem(Tags.Items.RODS_WOODEN), RegistrumRecipeProvider.has(Tags.Items.RODS_WOODEN))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_PLATES), RegistrumRecipeProvider.has(ModItemTags.TIN_PLATES))
            .save(provider);
    }

    public static <T extends Item> void ionocraftBackpack(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("ABA")
            .pattern("CDC")
            .define('A', ModItems.IONOCRAFT.asItem())
            .define('B', ModItemTags.CAPACITOR)
            .define('C', ModItemTags.TIN_PLATES)
            .define('D', Items.LEATHER_CHESTPLATE)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.IONOCRAFT.asItem()), RegistrumRecipeProvider.has(ModItems.IONOCRAFT.asItem()))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.CAPACITOR), RegistrumRecipeProvider.has(ModItemTags.CAPACITOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_PLATES), RegistrumRecipeProvider.has(ModItemTags.TIN_PLATES))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LEATHER_CHESTPLATE), RegistrumRecipeProvider.has(Items.LEATHER_CHESTPLATE))
            .save(provider);
    }

    public static <T extends Item> void permutationTemplateItem(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("EEE")
            .pattern("ETV")
            .pattern("VVV")
            .define('E', ModItems.EARTH_CORE_SHARD)
            .define('T', ModItemTags.TEMPLATES)
            .define('V', ModItems.VOID_MATTER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FROST_SMITHING_TABLE),
                RegistrumRecipeProvider.has(ModBlocks.FROST_SMITHING_TABLE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD), RegistrumRecipeProvider.has(ModItems.EARTH_CORE_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER), RegistrumRecipeProvider.has(ModItems.VOID_MATTER))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModItems.DEFORMATION_TEMPLATE_ITEM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.DEFORMATION_TEMPLATE_ITEM),
                RegistrumRecipeProvider.has(ModItems.DEFORMATION_TEMPLATE_ITEM)
            )
            .save(provider, AnvilCraft.of("shapeless/deform_to_permut"));
    }

    public static <T extends Item> void deformationTemplateItem(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("VVV")
            .pattern("VTE")
            .pattern("EEE")
            .define('E', ModItems.EARTH_CORE_SHARD)
            .define('T', ModItemTags.TEMPLATES)
            .define('V', ModItems.VOID_MATTER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FROST_SMITHING_TABLE),
                RegistrumRecipeProvider.has(ModBlocks.FROST_SMITHING_TABLE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD), RegistrumRecipeProvider.has(ModItems.EARTH_CORE_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER), RegistrumRecipeProvider.has(ModItems.VOID_MATTER))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModItems.PERMUTATION_TEMPLATE_ITEM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.PERMUTATION_TEMPLATE_ITEM),
                RegistrumRecipeProvider.has(ModItems.PERMUTATION_TEMPLATE_ITEM)
            )
            .save(provider, AnvilCraft.of("shapeless/permut_to_deform"));
    }

    public static <T extends Item> void disk(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("ABA")
            .pattern("ACA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', Items.IRON_INGOT)
            .define('C', ModItems.MAGNET_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), RegistrumRecipeProvider.has(ModItems.HARDEND_RESIN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), RegistrumRecipeProvider.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MAGNET_INGOT), RegistrumRecipeProvider.has(ModItems.MAGNET_INGOT))
            .save(provider);
    }

    @SuppressWarnings("unused")
    public static <T extends Item> void structureDiskConversion(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.DISK.get())
            .requires(ModItems.STRUCTURE_DISK.get())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.STRUCTURE_DISK.get()), AnvilCraftDatagen.has(ModItems.STRUCTURE_DISK.get()))
            .save(provider, AnvilCraft.of("structure_disk_to_disk"));
        
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.STRUCTURE_DISK.get())
            .requires(ModItems.DISK.get())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DISK.get()), AnvilCraftDatagen.has(ModItems.DISK.get()))
            .save(provider, AnvilCraft.of("disk_to_structure_disk"));
    }

    public static <T extends Item> void filter(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("ACA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', Items.HOPPER)
            .define('C', ModItems.CIRCUIT_BOARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), RegistrumRecipeProvider.has(ModItems.HARDEND_RESIN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.HOPPER), RegistrumRecipeProvider.has(Items.HOPPER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), RegistrumRecipeProvider.has(ModItems.CIRCUIT_BOARD))
            .save(provider);
    }

    public static <T extends Item> void totemOfRecovery(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("CCC")
            .pattern("BAB")
            .pattern("CCC")
            .define('A', Items.TOTEM_OF_UNDYING)
            .define('B', ModItems.RECOVERY_PEARL)
            .define('C', Items.ECHO_SHARD)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.TOTEM_OF_UNDYING))
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.RECOVERY_PEARL))
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.ECHO_SHARD))
            .save(provider);
    }

    public static <T extends Item> void totemOfRage(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("BBB")
            .pattern("CAC")
            .pattern("BBB")
            .define('A', Items.TOTEM_OF_UNDYING)
            .define('B', ModBlocks.CURSED_GOLD_BLOCK)
            .define('C', ModItems.EMBER_METAL_NUGGET)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.TOTEM_OF_UNDYING))
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.CURSED_GOLD_BLOCK))
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.EMBER_METAL_NUGGET))
            .save(provider);
    }

    public static <T extends Item> void capacitorEmpty(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', ModItemTags.COPPER_PLATES)
            .define('B', ModItems.RESIN)
            .unlockedBy("has_copper_plates", RegistrumRecipeProvider.has(ModItemTags.COPPER_PLATES))
            .unlockedBy("has_resin", RegistrumRecipeProvider.has(ModItems.RESIN))
            .save(provider);
    }

    public static <T extends Item> void recoveryPearl(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern(" B ")
            .pattern("BAB")
            .pattern(" B ")
            .define('A', Items.ENDER_PEARL)
            .define('B', Items.ECHO_SHARD)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.ENDER_PEARL))
            .save(provider);
    }

    public static <T extends Item> void pillBox(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', ModFoodItems.PILL)
            .unlockedBy("has_hardend_resin", RegistrumRecipeProvider.has(ModItems.HARDEND_RESIN))
            .unlockedBy("has_pill", RegistrumRecipeProvider.has(ModFoodItems.PILL))
            .save(provider);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> amulet(
        NonNullConsumer<JewelCraftingRecipe.Builder> builderConsumer
    ) {
        return (ctx, provider) -> {
            JewelCraftingRecipe.Builder builder = JewelCraftingRecipe.builder()
                .requires(ModItems.SILVER_INGOT, 1)
                .result(new ItemStack(ctx.get()));
            builderConsumer.accept(builder);
            builder.save(provider);
        };
    }

    public static <T extends Item> void magnetIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.MAGNET_BLOCK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.MAGNET_BLOCK))
            .group(ctx.getId().toString())
            .save(provider, AnvilCraft.of("magnet_ingot_from_block"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 8)
            .requires(ModBlocks.HOLLOW_MAGNET_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.HOLLOW_MAGNET_BLOCK))
            .save(provider, AnvilCraft.of("magnet_ingot_from_hollow_block"));
    }

    public static <T extends Item> void royalSteelIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.ROYAL_STEEL_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("royal_steel_ingot_from_royal_steel_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_STEEL_INGOT)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.ROYAL_STEEL_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_NUGGET.get()),
                AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_NUGGET)
            )
            .save(provider, AnvilCraft.of("royal_steel_ingot_from_royal_steel_nugget"));
    }

    public static <T extends Item> void royalSteelNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT.get()),
                AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT)
            )
            .save(provider);
    }

    public static <T extends Item> void frostMetalIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.FROST_METAL_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FROST_METAL_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK)
            )
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.FROST_METAL_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_NUGGET),
                RegistrumRecipeProvider.has(ModItems.FROST_METAL_NUGGET)
            )
            .save(provider);
    }

    public static <T extends Item> void frostMetalNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.FROST_METAL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
            .save(provider);
    }

    public static <T extends Item> void emberMetalIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.EMBER_METAL_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK)
            )
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.EMBER_METAL_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_NUGGET),
                RegistrumRecipeProvider.has(ModItems.EMBER_METAL_NUGGET)
            )
            .save(provider);
    }

    public static <T extends Item> void emberMetalNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.EMBER_METAL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
    }

    public static <T extends Item> void transcendiumIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.TRANSCENDIUM_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.TRANSCENDIUM_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.TRANSCENDIUM_BLOCK)
            )
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.TRANSCENDIUM_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_NUGGET),
                RegistrumRecipeProvider.has(ModItems.TRANSCENDIUM_NUGGET)
            )
            .save(provider);
    }

    public static <T extends Item> void transcendiumNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.TRANSCENDIUM_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT), AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_INGOT))
            .save(provider, AnvilCraft.of(ctx.getId().getPath() + "_from_ingot"));
    }

    public static <T extends Item> void cursedGoldIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.CURSED_GOLD_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.CURSED_GOLD_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.CURSED_GOLD_BLOCK)
            )
            .save(provider, AnvilCraft.of("cursed_gold_ingot_from_cursed_gold_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.CURSED_GOLD_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_NUGGET.get()),
                AnvilCraftDatagen.has(ModItems.CURSED_GOLD_NUGGET)
            )
            .save(provider, AnvilCraft.of("cursed_gold_ingot_from_cursed_gold_nugget"));
    }

    public static <T extends Item> void cursedGoldNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.CURSED_GOLD_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT.get()), AnvilCraftDatagen.has(ModItems.CURSED_GOLD_INGOT))
            .save(provider);
    }

    public static <T extends Item> void topaz(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.TOPAZ_BLOCK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.TOPAZ_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void ruby(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RUBY_BLOCK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.RUBY_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void sapphire(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.SAPPHIRE_BLOCK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.SAPPHIRE_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void expGem(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.EXP_GEM_BLOCK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.EXP_GEM_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void resin(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RESIN_BLOCK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.RESIN_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void amber(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.AMBER_BLOCK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.AMBER_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void circuitBoard(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModItemTags.COPPER_PLATES)
            .requires(ModItems.HARDEND_RESIN)
            .requires(ModItems.HARDEND_RESIN)
            .requires(ModItems.HARDEND_RESIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.COPPER_PLATES), AnvilCraftDatagen.has(ModItemTags.COPPER_PLATES))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(ModItems.HARDEND_RESIN))
            .save(provider);
    }

    public static <T extends Item> void processor(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("   ")
            .pattern("CAC")
            .pattern("BBB")
            .define('A', Items.COMPARATOR)
            .define('B', ModItems.HARDEND_RESIN)
            .define('C', ModItemTags.COPPER_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(ModItems.HARDEND_RESIN))
            .save(provider);
    }

    public static <T extends Item> void tungstenNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.TUNGSTEN_INGOTS)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS),
                RegistrumRecipeProvider.has(ModItemTags.TUNGSTEN_INGOTS)
            )
            .save(provider);
    }

    private static <T extends Item> void standardMetalIngotWithOreRecipes(
        DataGenContext<Item, T> ctx,
        RegistrumRecipeProvider provider,
        ItemLike block,
        TagKey<Item> nuggetTag,
        ItemLike rawMaterial,
        ItemLike deepslateOre
    ) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(block)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(block))
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', nuggetTag)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(nuggetTag), RegistrumRecipeProvider.has(nuggetTag))
            .save(provider);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(rawMaterial), RecipeCategory.MISC, ctx.get(), 1, 200)
            .group(ctx.getId().toString())
            .unlockedBy("has_item", AnvilCraftDatagen.has(rawMaterial))
            .save(provider, AnvilCraft.of("smelting/" + ctx.getName()));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(rawMaterial), RecipeCategory.MISC, ctx.get(), 1, 100)
            .group(ctx.getId().toString())
            .unlockedBy("has_item", AnvilCraftDatagen.has(rawMaterial))
            .save(provider, AnvilCraft.of("blasting/" + ctx.getName()));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(deepslateOre), RecipeCategory.MISC, ctx.get(), 1, 200)
            .group(ctx.getId().toString())
            .unlockedBy("has_item", AnvilCraftDatagen.has(deepslateOre))
            .save(provider, AnvilCraft.of("smelting/" + ctx.getName() + "_from_ore"));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(deepslateOre), RecipeCategory.MISC, ctx.get(), 1, 100)
            .group(ctx.getId().toString())
            .unlockedBy("has_item", AnvilCraftDatagen.has(deepslateOre))
            .save(provider, AnvilCraft.of("blasting/" + ctx.getName() + "_from_ore"));
    }

    public static <T extends Item> void tungstenIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        standardMetalIngotWithOreRecipes(
            ctx,
            provider,
            ModBlocks.TUNGSTEN_BLOCK,
            ModItemTags.TUNGSTEN_NUGGETS,
            ModItems.RAW_TUNGSTEN,
            ModBlocks.DEEPSLATE_TUNGSTEN_ORE
        );
    }

    public static <T extends Item> void titaniumNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.TITANIUM_INGOTS)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS),
                RegistrumRecipeProvider.has(ModItemTags.TITANIUM_INGOTS)
            )
            .save(provider);
    }

    public static <T extends Item> void titaniumIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        standardMetalIngotWithOreRecipes(
            ctx,
            provider,
            ModBlocks.TITANIUM_BLOCK,
            ModItemTags.TITANIUM_NUGGETS,
            ModItems.RAW_TITANIUM,
            ModBlocks.DEEPSLATE_TITANIUM_ORE
        );
    }

    public static <T extends Item> void zincNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.ZINC_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS), RegistrumRecipeProvider.has(ModItemTags.ZINC_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void zincIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        standardMetalIngotWithOreRecipes(
            ctx,
            provider,
            ModBlocks.ZINC_BLOCK,
            ModItemTags.ZINC_NUGGETS,
            ModItems.RAW_ZINC,
            ModBlocks.DEEPSLATE_ZINC_ORE
        );
    }

    public static <T extends Item> void tinNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.TIN_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS), RegistrumRecipeProvider.has(ModItemTags.TIN_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void tinIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        standardMetalIngotWithOreRecipes(
            ctx,
            provider,
            ModBlocks.TIN_BLOCK,
            ModItemTags.TIN_NUGGETS,
            ModItems.RAW_TIN,
            ModBlocks.DEEPSLATE_TIN_ORE
        );
    }

    public static <T extends Item> void leadNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.LEAD_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS), RegistrumRecipeProvider.has(ModItemTags.LEAD_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void leadIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        standardMetalIngotWithOreRecipes(
            ctx,
            provider,
            ModBlocks.LEAD_BLOCK,
            ModItemTags.LEAD_NUGGETS,
            ModItems.RAW_LEAD,
            ModBlocks.DEEPSLATE_LEAD_ORE
        );
    }

    public static <T extends Item> void silverNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.SILVER_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS), RegistrumRecipeProvider.has(ModItemTags.SILVER_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void silverIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        standardMetalIngotWithOreRecipes(
            ctx,
            provider,
            ModBlocks.SILVER_BLOCK,
            ModItemTags.SILVER_NUGGETS,
            ModItems.RAW_SILVER,
            ModBlocks.DEEPSLATE_SILVER_ORE
        );
    }

    public static <T extends Item> void uraniumNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.URANIUM_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS), RegistrumRecipeProvider.has(ModItemTags.URANIUM_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void uraniumIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        standardMetalIngotWithOreRecipes(
            ctx,
            provider,
            ModBlocks.URANIUM_BLOCK,
            ModItemTags.URANIUM_NUGGETS,
            ModItems.RAW_URANIUM,
            ModBlocks.DEEPSLATE_URANIUM_ORE
        );
    }

    public static <T extends Item> void plutoniumNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.PLUTONIUM_INGOTS)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.PLUTONIUM_INGOTS),
                RegistrumRecipeProvider.has(ModItemTags.PLUTONIUM_INGOTS)
            )
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
    }

    public static <T extends Item> void plutoniumIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.PLUTONIUM_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLUTONIUM_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.PLUTONIUM_BLOCK))
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.PLUTONIUM_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.PLUTONIUM_NUGGETS),
                RegistrumRecipeProvider.has(ModItemTags.PLUTONIUM_NUGGETS)
            )
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_nuggets"));
    }

    public static <T extends Item> void copperNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(Ingredient.of(Items.COPPER_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), RegistrumRecipeProvider.has(Items.COPPER_INGOT))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.COPPER_INGOT)
            .requires(ctx.get(), 9)
            .unlockedBy(AnvilCraftDatagen.hasItem(ctx.get()), RegistrumRecipeProvider.has(ctx.get()))
            .save(provider, AnvilCraft.of("copper_ingot_from_nugget"));
    }

    public static <T extends Item> void bronzeIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.BRONZE_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRONZE_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.BRONZE_BLOCK))
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.BRONZE_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.BRONZE_NUGGETS))
            .save(provider);
    }

    public static <T extends Item> void bronzeNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.BRONZE_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_INGOTS), RegistrumRecipeProvider.has(ModItemTags.BRONZE_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void brassIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.BRASS_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRASS_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.BRASS_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.BRASS_NUGGETS))
            .save(provider);
    }

    public static <T extends Item> void brassNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.BRASS_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS), RegistrumRecipeProvider.has(ModItemTags.BRASS_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void netheriteCrystalNucleus(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .define('A', ModItemTags.TUNGSTEN_PLATES)
            .define('B', Items.NETHERITE_SCRAP)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_PLATES),
                RegistrumRecipeProvider.has(ModItemTags.TUNGSTEN_PLATES)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.NETHERITE_SCRAP), RegistrumRecipeProvider.has(Items.NETHERITE_SCRAP))
            .save(provider);
    }

    public static <T extends Item> void levitationPowder(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.LEVITATION_POWDER_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.LEVITATION_POWDER_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.LEVITATION_POWDER_BLOCK)
            )
            .save(provider, ctx.getId().withSuffix("_from_block"));
    }

    public static <T extends Item> void rawZinc(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_ZINC_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_ZINC_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_ZINC_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawTin(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TIN_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_TIN_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_TIN_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawTitanium(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TITANIUM_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_TITANIUM_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_TITANIUM_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawTungsten(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TUNGSTEN_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_TUNGSTEN_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_TUNGSTEN_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawLead(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_LEAD_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_LEAD_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_LEAD_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawSilver(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_SILVER_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_SILVER_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_SILVER_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawUranium(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_URANIUM_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_URANIUM_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_URANIUM_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void voidMatter(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.VOID_MATTER_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.VOID_MATTER_BLOCK), AnvilCraftDatagen.has(ModBlocks.VOID_MATTER_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void earthCoreShard(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.EARTH_CORE_SHARD_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.EARTH_CORE_SHARD_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.EARTH_CORE_SHARD_BLOCK)
            )
            .save(provider);
    }

    public static <T extends Item> void multiphaseMatter(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.MULTIPHASE_MATTER_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MULTIPHASE_MATTER_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.MULTIPHASE_MATTER_BLOCK)
            )
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
    }

    public static <T extends Item> void heavyHalberdCore(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("HHH")
            .pattern("HMH")
            .pattern("HHH")
            .define('H', ModBlocks.HEAVY_IRON_BLOCK)
            .define('M', ModItems.MULTIPHASE_MATTER)
            .unlockedBy("has_heavy_iron_block", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MULTIPHASE_MATTER), AnvilCraftDatagen.has(ModItems.MULTIPHASE_MATTER))
            .save(provider);
    }

    public static <T extends Item> void resonatorCore(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AEA")
            .pattern("EME")
            .pattern("AEA")
            .define('A', Items.AMETHYST_SHARD)
            .define('E', Items.ECHO_SHARD)
            .define('M', ModItems.MULTIPHASE_MATTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.AMETHYST_SHARD), AnvilCraftDatagen.has(Items.AMETHYST_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ECHO_SHARD), AnvilCraftDatagen.has(Items.ECHO_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MULTIPHASE_MATTER), AnvilCraftDatagen.has(ModItems.MULTIPHASE_MATTER))
            .save(provider);
    }

    public static <T extends Item> void multiphaseTranscendium(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.MULTIPHASE_MATTER),
                Ingredient.of(ModItems.TRANSCENDIUM_INGOT),
                RecipeCategory.MISC,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE))
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.MULTIPHASE_MATTER))
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_INGOT))
            .save(provider, AnvilCraft.of("multiphase_transcendium"));
    }

    public static <T extends Item> void negativeMatter(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.NEGATIVE_MATTER_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.NEGATIVE_MATTER_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.NEGATIVE_MATTER_BLOCK)
            )
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.NEGATIVE_MATTER_NUGGET)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.NEGATIVE_MATTER_NUGGET),
                RegistrumRecipeProvider.has(ModItems.NEGATIVE_MATTER_NUGGET)
            )
            .save(provider);
    }

    public static <T extends Item> void negativeMatterNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.NEGATIVE_MATTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.NEGATIVE_MATTER), AnvilCraftDatagen.has(ModItems.NEGATIVE_MATTER))
            .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
    }

    public static <T extends Item> void stableNeutroniumIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 1)
            .requires(ModItems.NEUTRONIUM_INGOT)
            .requires(ModItems.LEVITATION_POWDER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.NEUTRONIUM_INGOT), AnvilCraftDatagen.has(ModItems.NEUTRONIUM_INGOT))
            .save(provider);
    }

    public static <T extends Item> void recipe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> axe(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.axe(ingredient, (ctx, p) -> ctx.get().getDefaultInstance());
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> axe(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStack> result
    ) {
        return RegistrumItemRecipeLoader.tool("AA", "AB", " B", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> hoe(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.hoe(ingredient, (ctx, p) -> ctx.get().getDefaultInstance());
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> hoe(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStack> result
    ) {
        return RegistrumItemRecipeLoader.tool("AA", " B", " B", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> sword(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.sword(ingredient, (ctx, p) -> ctx.get().getDefaultInstance());
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> sword(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStack> result
    ) {
        return RegistrumItemRecipeLoader.tool("A", "A", "B", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> shovel(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.shovel(ingredient, (ctx, p) -> ctx.get().getDefaultInstance());
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> shovel(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStack> result
    ) {
        return RegistrumItemRecipeLoader.tool("A", "B", "B", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> pickaxe(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.pickaxe(ingredient, (ctx, p) -> ctx.get().getDefaultInstance());
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> pickaxe(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStack> result
    ) {
        return RegistrumItemRecipeLoader.tool("AAA", " B ", " B ", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> tool(
        String pattern1,
        String pattern2,
        String pattern3,
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStack> result
    ) {
        return (ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result.apply(ctx, provider))
            .pattern(pattern1)
            .pattern(pattern2)
            .pattern(pattern3)
            .define('A', ingredient)
            .define('B', Items.STICK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ingredient))
            .save(provider);
    }

    public static <T extends Item> void dysonSphereComponent(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("ABA")
            .pattern("CCC")
            .pattern("ABA")
            .define('A', ModItems.EMBER_METAL_INGOT)
            .define('B', ModItems.TRANSCENDIUM_INGOT)
            .define('C', ModBlocks.HEAT_COLLECTOR)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT), AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAT_COLLECTOR.asItem()), AnvilCraftDatagen.has(ModBlocks.HEAT_COLLECTOR))
            .save(provider);
    }

    public static <T extends Item> void penroseSphereComponent(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("ABA")
            .pattern("BCB")
            .pattern("ABA")
            .define('A', ModItems.TRANSCENDIUM_INGOT)
            .define('B', ModBlocks.HELIOSTATS)
            .define('C', ModBlocks.RUBY_PRISM)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT), AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HELIOSTATS.asItem()), AnvilCraftDatagen.has(ModBlocks.HELIOSTATS))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RUBY_PRISM.asItem()), AnvilCraftDatagen.has(ModBlocks.RUBY_PRISM))
            .save(provider);
    }
}
