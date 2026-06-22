package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class RegistrumItemRecipeLoader {
    public static <T extends Item> void guideBook(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.TOOLS, ctx.get())
            .requires(Ingredient.of(Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL))
            .requires(Items.BOOK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ANVIL), AnvilCraftDatagen.has(lookup, Items.ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.CHIPPED_ANVIL), AnvilCraftDatagen.has(lookup, Items.CHIPPED_ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DAMAGED_ANVIL), AnvilCraftDatagen.has(lookup, Items.DAMAGED_ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.BOOK), AnvilCraftDatagen.has(lookup, Items.BOOK))
            .save(provider);
    }

    public static <T extends Item> void magnet(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.TOOLS, ctx.get())
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .define('A', Items.ENDER_PEARL)
            .define('B', ModItems.MAGNET_INGOT)
            .define('C', Items.REDSTONE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MAGNET_INGOT), AnvilCraftDatagen.has(lookup, ModItems.MAGNET_INGOT))
            .save(provider);
    }

    public static <T extends Item> void royalSteelPickaxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.ROYAL_STEEL_PICKAXE_BASE)),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/royal_steel_pickaxe"));
    }

    public static <T extends Item> void royalSteelAxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.ROYAL_STEEL_AXE_BASE)),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/royal_steel_axe"));
    }

    public static <T extends Item> void royalSteelShovel(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.ROYAL_STEEL_SHOVEL_BASE)),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/royal_steel_shovel"));
    }

    public static <T extends Item> void royalSteelHoe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.ROYAL_STEEL_HOE_BASE)),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/royal_steel_hoe"));
    }

    public static <T extends Item> void royalSteelSword(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.ROYAL_STEEL_SWORD_BASE)),
                Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.ROYAL_STEEL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/royal_steel_sword"));
    }

    public static <T extends Item> void frostMetalPickaxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.FROST_METAL_PICKAXE_BASE)),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/frost_metal_pickaxe"));
    }

    public static <T extends Item> void frostMetalAxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.FROST_METAL_AXE_BASE)),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/frost_metal_axe"));
    }

    public static <T extends Item> void frostMetalShovel(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.FROST_METAL_SHOVEL_BASE)),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/frost_metal_shovel"));
    }

    public static <T extends Item> void frostMetalHoe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.FROST_METAL_HOE_BASE)),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/frost_metal_hoe"));
    }

    public static <T extends Item> void frostMetalSword(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.FROST_METAL_SWORD_BASE)),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/frost_metal_sword"));
    }

    public static <T extends Item> void emberMetalPickaxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.EMBER_METAL_PICKAXE_BASE)),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/ember_metal_pickaxe"));
    }

    public static <T extends Item> void emberMetalAxe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.EMBER_METAL_AXE_BASE)),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/ember_metal_axe"));
    }

    public static <T extends Item> void emberMetalShovel(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.EMBER_METAL_SHOVEL_BASE)),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/ember_metal_shovel"));
    }

    public static <T extends Item> void emberMetalHoe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.EMBER_METAL_HOE_BASE)),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/ember_metal_hoe"));
    }

    public static <T extends Item> void emberMetalSword(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(lookup.getOrThrow(ModItemTags.EMBER_METAL_SWORD_BASE)),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.recipe("smithing/ember_metal_sword"));
    }

    public static <T extends Item> void anvilHammer(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.TOOLS, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', Items.ANVIL)
            .define('B', Items.LIGHTNING_ROD)
            .define('C', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ANVIL), AnvilCraftDatagen.has(lookup, Items.ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LIGHTNING_ROD), AnvilCraftDatagen.has(lookup, Items.LIGHTNING_ROD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(lookup, Items.IRON_INGOT))
            .save(provider);
    }

    public static <T extends Item> void royalAnvilHammer(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.ANVIL_HAMMER),
                Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.recipe("smithing/royal_anvil_hammer"));
    }

    public static <T extends Item> void emberAnvilHammer(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.ROYAL_ANVIL_HAMMER),
                Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.recipe("smithing/ember_anvil_hammer"));
    }

    public static <T extends Item> void transcendenceAnvilHammer(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.EMBER_ANVIL_HAMMER),
                Ingredient.of(ModBlocks.TRANSCENDIUM_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.TRANSCENDIUM_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.TRANSCENDIUM_BLOCK))
            .save(provider, AnvilCraft.recipe("smithing/transcendence_anvil_hammer"));
    }

    public static <T extends Item> void dragonRod(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), AnvilCraftDatagen.has(lookup, ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ANVIL_HAMMER), AnvilCraftDatagen.has(lookup, ModItems.ANVIL_HAMMER))
            .save(provider);
    }

    public static <T extends Item> void royalDragonRod(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.ROYAL_ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), AnvilCraftDatagen.has(lookup, ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_ANVIL_HAMMER),
                AnvilCraftDatagen.has(lookup, ModItems.ROYAL_ANVIL_HAMMER)
            )
            .save(provider);
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModItems.DRAGON_ROD),
            Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
            RecipeCategory.TOOLS,
            ctx.get()
        ).unlocks(
            AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
            AnvilCraftDatagen.has(lookup, ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
        ).unlocks(
            AnvilCraftDatagen.hasItem(ModItems.DRAGON_ROD),
            AnvilCraftDatagen.has(lookup, ModItems.DRAGON_ROD)
        ).unlocks(
            AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK),
            AnvilCraftDatagen.has(lookup, ModBlocks.ROYAL_STEEL_BLOCK)
        ).save(provider, ctx.getId().withPrefix("smithing/").toString());
    }

    public static <T extends Item> void emberDragonRod(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.EMBER_ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), AnvilCraftDatagen.has(lookup, ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_ANVIL_HAMMER), AnvilCraftDatagen.has(lookup, ModItems.EMBER_ANVIL_HAMMER))
            .save(provider);
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModItems.ROYAL_DRAGON_ROD),
            Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
            RecipeCategory.TOOLS,
            ctx.get()
        ).unlocks(
            AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK),
            AnvilCraftDatagen.has(lookup, ModBlocks.EMBER_METAL_BLOCK)
        ).save(provider, ctx.getId().withPrefix("smithing/").toString());
    }

    public static <T extends Item> void transcendenceDragonRod(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.TRANSCENDENCE_ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), AnvilCraftDatagen.has(lookup, ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.TRANSCENDENCE_ANVIL_HAMMER),
                AnvilCraftDatagen.has(lookup, ModItems.TRANSCENDENCE_ANVIL_HAMMER)
            )
            .save(provider);
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModItems.EMBER_DRAGON_ROD),
            Ingredient.of(ModBlocks.TRANSCENDIUM_BLOCK),
            RecipeCategory.TOOLS,
            ctx.get()
        ).unlocks(
            AnvilCraftDatagen.hasItem(ModBlocks.TRANSCENDIUM_BLOCK),
            AnvilCraftDatagen.has(lookup, ModBlocks.TRANSCENDIUM_BLOCK)
        ).save(provider, ctx.getId().withPrefix("smithing/").toString());
    }

    public static <T extends Item> void energyWeaponPlatform(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("ASS")
            .pattern("ASS")
            .pattern("FPC")
            .define('A', Blocks.ANVIL)
            .define('C', ModBlocks.SPACE_OVERCOMPRESSOR)
            .define('F', Blocks.SMITHING_TABLE)
            .define('P', ModItems.PROCESSOR)
            .define('S', ModItems.SUPER_CAPACITOR)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.ANVIL), AnvilCraftDatagen.has(lookup, Blocks.ANVIL))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.SPACE_OVERCOMPRESSOR),
                AnvilCraftDatagen.has(lookup, ModBlocks.SPACE_OVERCOMPRESSOR)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.SMITHING_TABLE), AnvilCraftDatagen.has(lookup, Blocks.SMITHING_TABLE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), AnvilCraftDatagen.has(lookup, ModItems.PROCESSOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SUPER_CAPACITOR), AnvilCraftDatagen.has(lookup, ModItems.SUPER_CAPACITOR))
            .save(provider);
    }

    public static <T extends Item> void spectralSlingshot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("B B")
            .pattern(" C ")
            .define('A', Items.PHANTOM_MEMBRANE)
            .define('B', ModBlocks.SPECTRAL_ANVIL.asItem())
            .define('C', Items.CROSSBOW)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.PHANTOM_MEMBRANE), AnvilCraftDatagen.has(lookup, Items.PHANTOM_MEMBRANE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SPECTRAL_ANVIL), AnvilCraftDatagen.has(lookup, ModBlocks.SPECTRAL_ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.CROSSBOW), AnvilCraftDatagen.has(lookup, Items.CROSSBOW))
            .save(provider);
    }

    public static <T extends Item> void ionocraft(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("BBB")
            .pattern(" C ")
            .define('A', ModItemTags.COPPER_NUGGETS)
            .define('B', Tags.Items.RODS_WOODEN)
            .define('C', ModItemTags.TIN_PLATES)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.COPPER_NUGGETS), AnvilCraftDatagen.has(lookup, ModItemTags.COPPER_NUGGETS))
            .unlockedBy(AnvilCraftDatagen.hasItem(Tags.Items.RODS_WOODEN), AnvilCraftDatagen.has(lookup, Tags.Items.RODS_WOODEN))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_PLATES), AnvilCraftDatagen.has(lookup, ModItemTags.TIN_PLATES))
            .save(provider);
    }

    public static <T extends Item> void ionocraftBackpack(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        DataComponentPatch patch = DataComponentPatch.builder()
            .set(ModComponents.STORED_ENERGY, new StoredEnergy(0))
            .build();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, new ItemStackTemplate(ctx.get(), patch))
            .pattern("ABA")
            .pattern("ABA")
            .pattern("CDC")
            .define('A', ModItems.IONOCRAFT.asItem())
            .define('B', ModItemTags.CAPACITOR)
            .define('C', ModItemTags.TIN_PLATES)
            .define('D', Items.LEATHER_CHESTPLATE)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.IONOCRAFT.asItem()), AnvilCraftDatagen.has(lookup, ModItems.IONOCRAFT.asItem()))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.CAPACITOR), AnvilCraftDatagen.has(lookup, ModItemTags.CAPACITOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_PLATES), AnvilCraftDatagen.has(lookup, ModItemTags.TIN_PLATES))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LEATHER_CHESTPLATE), AnvilCraftDatagen.has(lookup, Items.LEATHER_CHESTPLATE))
            .save(provider);
    }

    public static <T extends Item> void permutationTemplateItem(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("EEE")
            .pattern("ETV")
            .pattern("VVV")
            .define('E', ModItems.EARTH_CORE_SHARD)
            .define('T', ModItemTags.TEMPLATES)
            .define('V', ModItems.VOID_MATTER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FROST_SMITHING_TABLE),
                AnvilCraftDatagen.has(lookup, ModBlocks.FROST_SMITHING_TABLE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD), AnvilCraftDatagen.has(lookup, ModItems.EARTH_CORE_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER), AnvilCraftDatagen.has(lookup, ModItems.VOID_MATTER))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get())
            .requires(ModItems.DEFORMATION_TEMPLATE)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.DEFORMATION_TEMPLATE),
                AnvilCraftDatagen.has(lookup, ModItems.DEFORMATION_TEMPLATE)
            )
            .save(provider, AnvilCraft.recipe("shapeless/deform_to_permut"));
    }

    public static <T extends Item> void deformationTemplateItem(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("VVV")
            .pattern("VTE")
            .pattern("EEE")
            .define('E', ModItems.EARTH_CORE_SHARD)
            .define('T', ModItemTags.TEMPLATES)
            .define('V', ModItems.VOID_MATTER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FROST_SMITHING_TABLE),
                AnvilCraftDatagen.has(lookup, ModBlocks.FROST_SMITHING_TABLE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD), AnvilCraftDatagen.has(lookup, ModItems.EARTH_CORE_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER), AnvilCraftDatagen.has(lookup, ModItems.VOID_MATTER))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get())
            .requires(ModItems.PERMUTATION_TEMPLATE)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.PERMUTATION_TEMPLATE),
                AnvilCraftDatagen.has(lookup, ModItems.PERMUTATION_TEMPLATE)
            )
            .save(provider, AnvilCraft.recipe("shapeless/permut_to_deform"));
    }

    public static <T extends Item> void disk(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.TOOLS, ctx.get())
            .pattern("ABA")
            .pattern("ACA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', Items.IRON_INGOT)
            .define('C', ModItems.MAGNET_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(lookup, ModItems.HARDEND_RESIN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(lookup, Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MAGNET_INGOT), AnvilCraftDatagen.has(lookup, ModItems.MAGNET_INGOT))
            .save(provider);
    }

    public static <T extends Item> void structureDisk(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.TOOLS, ctx.get())
            .requires(ModItems.DISK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DISK), AnvilCraftDatagen.has(lookup, ModItems.DISK))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.TOOLS, ModItems.DISK.get())
            .requires(ctx.get())
            .unlockedBy(AnvilCraftDatagen.hasItem(ctx.get()), AnvilCraftDatagen.has(lookup, ctx.get()))
            .save(provider, AnvilCraft.recipe("disk_from_structure_disk"));
    }

    public static <T extends Item> void filter(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.TOOLS, ctx.get())
            .pattern("ACA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', Items.HOPPER)
            .define('C', ModItems.CIRCUIT_BOARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(lookup, ModItems.HARDEND_RESIN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.HOPPER), AnvilCraftDatagen.has(lookup, Items.HOPPER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(lookup, ModItems.CIRCUIT_BOARD))
            .save(provider);
    }

    public static <T extends Item> void totemOfRecovery(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("CCC")
            .pattern("BAB")
            .pattern("CCC")
            .define('A', Items.TOTEM_OF_UNDYING)
            .define('B', ModItems.RECOVERY_PEARL)
            .define('C', Items.ECHO_SHARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.TOTEM_OF_UNDYING), AnvilCraftDatagen.has(lookup, Items.TOTEM_OF_UNDYING))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RECOVERY_PEARL), AnvilCraftDatagen.has(lookup, ModItems.RECOVERY_PEARL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ECHO_SHARD), AnvilCraftDatagen.has(lookup, Items.ECHO_SHARD))
            .save(provider);
    }

    public static <T extends Item> void totemOfRage(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("BBB")
            .pattern("CAC")
            .pattern("BBB")
            .define('A', Items.TOTEM_OF_UNDYING)
            .define('B', ModBlocks.CURSED_GOLD_BLOCK)
            .define('C', ModItems.EMBER_METAL_NUGGET)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.TOTEM_OF_UNDYING), AnvilCraftDatagen.has(lookup, Items.TOTEM_OF_UNDYING))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CURSED_GOLD_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.CURSED_GOLD_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_NUGGET), AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_NUGGET))
            .save(provider);
    }

    public static <T extends Item> void capacitorEmpty(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', ModItemTags.COPPER_PLATES)
            .define('B', ModItems.RESIN)
            .unlockedBy("has_copper_plates", AnvilCraftDatagen.has(lookup, ModItemTags.COPPER_PLATES))
            .unlockedBy("has_resin", AnvilCraftDatagen.has(lookup, ModItems.RESIN))
            .save(provider);
    }

    public static <T extends Item> void recoveryPearl(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.TOOLS, ctx.get())
            .pattern(" B ")
            .pattern("BAB")
            .pattern(" B ")
            .define('A', Items.ENDER_PEARL)
            .define('B', Items.ECHO_SHARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ENDER_PEARL), AnvilCraftDatagen.has(lookup, Items.ENDER_PEARL))
            .save(provider);
    }

    public static <T extends Item> void pillBox(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', ModFoodItems.PILL)
            .unlockedBy("has_hardend_resin", AnvilCraftDatagen.has(lookup, ModItems.HARDEND_RESIN))
            .unlockedBy("has_pill", AnvilCraftDatagen.has(lookup, ModFoodItems.PILL))
            .save(provider);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> amulet(
        NonNullConsumer<JewelCraftingRecipe.Builder> builderConsumer
    ) {
        return (ctx, provider) -> {
            JewelCraftingRecipe.Builder builder = JewelCraftingRecipe.builder(provider.getItems())
                .requires(ModItems.SILVER_INGOT, 1)
                .source(ctx.get());
            builderConsumer.accept(builder);
            builder.save(provider, ctx.getName());
        };
    }

    public static <T extends Item> void magnetIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.MAGNET_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.MAGNET_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.MAGNET_BLOCK))
            .group(ctx.getId().toString())
            .save(provider, AnvilCraft.recipe("magnet_ingot_from_block"));
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 8)
            .requires(ModBlocks.HOLLOW_MAGNET_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.HOLLOW_MAGNET_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.HOLLOW_MAGNET_BLOCK)
            )
            .save(provider, AnvilCraft.recipe("magnet_ingot_from_hollow_block"));
    }

    public static <T extends Item> void royalSteelIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.ROYAL_STEEL_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.recipe("royal_steel_ingot_from_royal_steel_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ModItems.ROYAL_STEEL_INGOT)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.ROYAL_STEEL_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_NUGGET.get()),
                AnvilCraftDatagen.has(lookup, ModItems.ROYAL_STEEL_NUGGET)
            )
            .save(provider, AnvilCraft.recipe("royal_steel_ingot_from_royal_steel_nugget"));
    }

    public static <T extends Item> void royalSteelNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT.get()),
                AnvilCraftDatagen.has(lookup, ModItems.ROYAL_STEEL_INGOT)
            )
            .save(provider);
    }

    public static <T extends Item> void frostMetalIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.FROST_METAL_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FROST_METAL_BLOCK.asItem()),
                AnvilCraftDatagen.has(lookup, ModBlocks.FROST_METAL_BLOCK)
            )
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.FROST_METAL_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_NUGGET),
                AnvilCraftDatagen.has(lookup, ModItems.FROST_METAL_NUGGET)
            )
            .save(provider);
    }

    public static <T extends Item> void frostMetalNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.FROST_METAL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.FROST_METAL_INGOT))
            .save(provider);
    }

    public static <T extends Item> void emberMetalIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.EMBER_METAL_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK.asItem()),
                AnvilCraftDatagen.has(lookup, ModBlocks.EMBER_METAL_BLOCK)
            )
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.EMBER_METAL_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_NUGGET),
                AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_NUGGET)
            )
            .save(provider);
    }

    public static <T extends Item> void emberMetalNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.EMBER_METAL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
    }

    public static <T extends Item> void transcendiumIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.TRANSCENDIUM_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.TRANSCENDIUM_BLOCK.asItem()),
                AnvilCraftDatagen.has(lookup, ModBlocks.TRANSCENDIUM_BLOCK)
            )
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.TRANSCENDIUM_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_NUGGET),
                AnvilCraftDatagen.has(lookup, ModItems.TRANSCENDIUM_NUGGET)
            )
            .save(provider);
    }

    public static <T extends Item> void transcendiumNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.TRANSCENDIUM_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT), AnvilCraftDatagen.has(lookup, ModItems.TRANSCENDIUM_INGOT))
            .save(provider, AnvilCraft.recipe(ctx.getId().getPath() + "_from_ingot"));
    }

    public static <T extends Item> void cursedGoldIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.CURSED_GOLD_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.CURSED_GOLD_BLOCK.asItem()),
                AnvilCraftDatagen.has(lookup, ModBlocks.CURSED_GOLD_BLOCK)
            )
            .save(provider, AnvilCraft.recipe("cursed_gold_ingot_from_cursed_gold_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.CURSED_GOLD_NUGGET)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_NUGGET.get()),
                AnvilCraftDatagen.has(lookup, ModItems.CURSED_GOLD_NUGGET)
            )
            .save(provider, AnvilCraft.recipe("cursed_gold_ingot_from_cursed_gold_nugget"));
    }

    public static <T extends Item> void cursedGoldNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.CURSED_GOLD_INGOT)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT.get()),
                AnvilCraftDatagen.has(lookup, ModItems.CURSED_GOLD_INGOT)
            )
            .save(provider);
    }

    public static <T extends Item> void topaz(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.TOPAZ_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.TOPAZ_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.TOPAZ_BLOCK)
            )
            .save(provider);
    }

    public static <T extends Item> void ruby(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RUBY_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RUBY_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.RUBY_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void sapphire(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.SAPPHIRE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SAPPHIRE_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.SAPPHIRE_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void expGem(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.EXP_GEM_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.EXP_GEM_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.EXP_GEM_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void resin(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RESIN_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RESIN_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.RESIN_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void amber(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.AMBER_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.AMBER_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.AMBER_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void circuitBoard(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get())
            .requires(ModItemTags.COPPER_PLATES)
            .requires(ModItems.HARDEND_RESIN)
            .requires(ModItems.HARDEND_RESIN)
            .requires(ModItems.HARDEND_RESIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.COPPER_PLATES), AnvilCraftDatagen.has(lookup, ModItemTags.COPPER_PLATES))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(lookup, ModItems.HARDEND_RESIN))
            .save(provider);
    }

    public static <T extends Item> void processor(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("   ")
            .pattern("CAC")
            .pattern("BBB")
            .define('A', Items.COMPARATOR)
            .define('B', ModItems.HARDEND_RESIN)
            .define('C', ModItemTags.COPPER_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(lookup, ModItems.HARDEND_RESIN))
            .save(provider);
    }

    public static <T extends Item> void tungstenNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.TUNGSTEN_INGOTS)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS),
                AnvilCraftDatagen.has(lookup, ModItemTags.TUNGSTEN_INGOTS)
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
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(block)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(lookup, block))
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', nuggetTag)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(nuggetTag), AnvilCraftDatagen.has(lookup, nuggetTag))
            .save(provider);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(rawMaterial), RecipeCategory.MISC, CookingBookCategory.MISC, ctx.get(), 1, 200)
            .group(ctx.getId().toString())
            .unlockedBy("has_item", AnvilCraftDatagen.has(lookup, rawMaterial))
            .save(provider, AnvilCraft.recipe("smelting/" + ctx.getName()));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(rawMaterial), RecipeCategory.MISC, CookingBookCategory.MISC, ctx.get(), 1, 100)
            .group(ctx.getId().toString())
            .unlockedBy("has_item", AnvilCraftDatagen.has(lookup, rawMaterial))
            .save(provider, AnvilCraft.recipe("blasting/" + ctx.getName()));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(deepslateOre), RecipeCategory.MISC, CookingBookCategory.MISC, ctx.get(), 1, 200)
            .group(ctx.getId().toString())
            .unlockedBy("has_item", AnvilCraftDatagen.has(lookup, deepslateOre))
            .save(provider, AnvilCraft.recipe("smelting/" + ctx.getName() + "_from_ore"));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(deepslateOre), RecipeCategory.MISC, CookingBookCategory.MISC, ctx.get(), 1, 100)
            .group(ctx.getId().toString())
            .unlockedBy("has_item", AnvilCraftDatagen.has(lookup, deepslateOre))
            .save(provider, AnvilCraft.recipe("blasting/" + ctx.getName() + "_from_ore"));
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
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.TITANIUM_INGOTS)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS),
                AnvilCraftDatagen.has(lookup, ModItemTags.TITANIUM_INGOTS)
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
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.ZINC_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS), AnvilCraftDatagen.has(lookup, ModItemTags.ZINC_INGOTS))
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
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.TIN_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS), AnvilCraftDatagen.has(lookup, ModItemTags.TIN_INGOTS))
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
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.LEAD_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS), AnvilCraftDatagen.has(lookup, ModItemTags.LEAD_INGOTS))
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
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.SILVER_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS), AnvilCraftDatagen.has(lookup, ModItemTags.SILVER_INGOTS))
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
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.URANIUM_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS), AnvilCraftDatagen.has(lookup, ModItemTags.URANIUM_INGOTS))
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
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.PLUTONIUM_INGOTS)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.PLUTONIUM_INGOTS),
                AnvilCraftDatagen.has(lookup, ModItemTags.PLUTONIUM_INGOTS)
            )
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
    }

    public static <T extends Item> void plutoniumIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.PLUTONIUM_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.PLUTONIUM_BLOCK.asItem()),
                AnvilCraftDatagen.has(lookup, ModBlocks.PLUTONIUM_BLOCK)
            )
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.PLUTONIUM_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.PLUTONIUM_NUGGETS),
                AnvilCraftDatagen.has(lookup, ModItemTags.PLUTONIUM_NUGGETS)
            )
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_nuggets"));
    }

    public static <T extends Item> void copperNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(Ingredient.of(Items.COPPER_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(lookup, Items.COPPER_INGOT))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, Items.COPPER_INGOT)
            .requires(ctx.get(), 9)
            .unlockedBy(AnvilCraftDatagen.hasItem(ctx.get()), AnvilCraftDatagen.has(lookup, ctx.get()))
            .save(provider, AnvilCraft.recipe("copper_ingot_from_nugget"));
    }

    public static <T extends Item> void bronzeIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.BRONZE_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRONZE_BLOCK.asItem()), AnvilCraftDatagen.has(lookup, ModBlocks.BRONZE_BLOCK))
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.BRONZE_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_NUGGETS), AnvilCraftDatagen.has(lookup, ModItemTags.BRONZE_NUGGETS))
            .save(provider);
    }

    public static <T extends Item> void bronzeNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.BRONZE_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_INGOTS), AnvilCraftDatagen.has(lookup, ModItemTags.BRONZE_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void brassIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.BRASS_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRASS_BLOCK.asItem()), AnvilCraftDatagen.has(lookup, ModBlocks.BRASS_BLOCK))
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.BRASS_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_NUGGETS), AnvilCraftDatagen.has(lookup, ModItemTags.BRASS_NUGGETS))
            .save(provider);
    }

    public static <T extends Item> void brassNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItemTags.BRASS_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS), AnvilCraftDatagen.has(lookup, ModItemTags.BRASS_INGOTS))
            .save(provider);
    }

    public static <T extends Item> void netheriteCrystalNucleus(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .define('A', ModItemTags.TUNGSTEN_PLATES)
            .define('B', Items.NETHERITE_SCRAP)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_PLATES),
                AnvilCraftDatagen.has(lookup, ModItemTags.TUNGSTEN_PLATES)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.NETHERITE_SCRAP), AnvilCraftDatagen.has(lookup, Items.NETHERITE_SCRAP))
            .save(provider);
    }

    public static <T extends Item> void levitationPowder(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.LEVITATION_POWDER_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.LEVITATION_POWDER_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.LEVITATION_POWDER_BLOCK)
            )
            .save(provider, ctx.getId().withSuffix("_from_block").toString());
    }

    public static <T extends Item> void rawZinc(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_ZINC_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_ZINC_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.RAW_ZINC_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawTin(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TIN_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_TIN_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.RAW_TIN_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawTitanium(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TITANIUM_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_TITANIUM_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.RAW_TITANIUM_BLOCK)
            )
            .save(provider);
    }

    public static <T extends Item> void rawTungsten(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TUNGSTEN_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_TUNGSTEN_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.RAW_TUNGSTEN_BLOCK)
            )
            .save(provider);
    }

    public static <T extends Item> void rawLead(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_LEAD_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_LEAD_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.RAW_LEAD_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawSilver(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_SILVER_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_SILVER_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.RAW_SILVER_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void rawUranium(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_URANIUM_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_URANIUM_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.RAW_URANIUM_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void voidMatter(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.VOID_MATTER_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.VOID_MATTER_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.VOID_MATTER_BLOCK))
            .save(provider);
    }

    public static <T extends Item> void earthCoreShard(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.EARTH_CORE_SHARD_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.EARTH_CORE_SHARD_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.EARTH_CORE_SHARD_BLOCK)
            )
            .save(provider);
    }

    public static <T extends Item> void multiphaseMatter(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.MULTIPHASE_MATTER_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MULTIPHASE_MATTER_BLOCK.asItem()),
                AnvilCraftDatagen.has(lookup, ModBlocks.MULTIPHASE_MATTER_BLOCK)
            )
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
    }

    public static <T extends Item> void heavyHalberdCore(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("HHH")
            .pattern("HMH")
            .pattern("HHH")
            .define('H', ModBlocks.HEAVY_IRON_BLOCK)
            .define('M', ModItems.MULTIPHASE_MATTER)
            .unlockedBy("has_heavy_iron_block", AnvilCraftDatagen.has(lookup, ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MULTIPHASE_MATTER), AnvilCraftDatagen.has(lookup, ModItems.MULTIPHASE_MATTER))
            .save(provider);
    }

    public static <T extends Item> void resonatorCore(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AEA")
            .pattern("EME")
            .pattern("AEA")
            .define('A', Items.AMETHYST_SHARD)
            .define('E', Items.ECHO_SHARD)
            .define('M', ModItems.MULTIPHASE_MATTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.AMETHYST_SHARD), AnvilCraftDatagen.has(lookup, Items.AMETHYST_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ECHO_SHARD), AnvilCraftDatagen.has(lookup, Items.ECHO_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MULTIPHASE_MATTER), AnvilCraftDatagen.has(lookup, ModItems.MULTIPHASE_MATTER))
            .save(provider);
    }

    public static <T extends Item> void multiphaseTranscendium(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.MULTIPHASE_MATTER),
                Ingredient.of(ModItems.TRANSCENDIUM_INGOT),
                RecipeCategory.MISC,
                ctx.get()
            )
            .unlocks(
                AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                AnvilCraftDatagen.has(lookup, ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE)
            )
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.MULTIPHASE_MATTER), AnvilCraftDatagen.has(lookup, ModItems.MULTIPHASE_MATTER))
            .unlocks(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT), AnvilCraftDatagen.has(lookup, ModItems.TRANSCENDIUM_INGOT))
            .save(provider, AnvilCraft.recipe("multiphase_transcendium"));
    }

    public static <T extends Item> void negativeMatter(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.NEGATIVE_MATTER_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.NEGATIVE_MATTER_BLOCK.asItem()),
                AnvilCraftDatagen.has(lookup, ModBlocks.NEGATIVE_MATTER_BLOCK)
            )
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.NEGATIVE_MATTER_NUGGET)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.NEGATIVE_MATTER_NUGGET),
                AnvilCraftDatagen.has(lookup, ModItems.NEGATIVE_MATTER_NUGGET)
            )
            .save(provider);
    }

    public static <T extends Item> void negativeMatterNugget(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.NEGATIVE_MATTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.NEGATIVE_MATTER), AnvilCraftDatagen.has(lookup, ModItems.NEGATIVE_MATTER))
            .save(provider, AnvilCraft.recipe(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
    }

    public static <T extends Item> void stableNeutroniumIngot(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ctx.get(), 1)
            .requires(ModItems.NEUTRONIUM_INGOT)
            .requires(ModItems.LEVITATION_POWDER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.NEUTRONIUM_INGOT), AnvilCraftDatagen.has(lookup, ModItems.NEUTRONIUM_INGOT))
            .save(provider);
    }

    public static <T extends Item> void cocoaLiquor(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.FOOD, ctx.get(), 2)
            .requires(ModItems.COCOA_POWDER)
            .requires(ModItems.COCOA_POWDER)
            .requires(ModItems.COCOA_BUTTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.COCOA_POWDER), AnvilCraftDatagen.has(lookup, ModItems.COCOA_POWDER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.COCOA_BUTTER), AnvilCraftDatagen.has(lookup, ModItems.COCOA_BUTTER))
            .save(provider);
    }

    public static <T extends Item> void chocolate(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.FOOD, ctx.get(), 4)
            .pattern("ABA")
            .pattern("CDC")
            .pattern("ABA")
            .define('A', ModItems.COCOA_LIQUOR)
            .define('B', ModItems.COCOA_BUTTER)
            .define('C', ModItems.CREAM)
            .define('D', Items.SUGAR)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.COCOA_LIQUOR), AnvilCraftDatagen.has(lookup, ModItems.COCOA_LIQUOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.COCOA_BUTTER), AnvilCraftDatagen.has(lookup, ModItems.COCOA_BUTTER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CREAM), AnvilCraftDatagen.has(lookup, ModItems.CREAM))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUGAR), AnvilCraftDatagen.has(lookup, Items.SUGAR))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.FOOD, ctx.get(), 9)
            .requires(ModBlocks.CHOCOLATE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.CHOCOLATE_BLOCK))
            .save(provider, AnvilCraft.recipe("chocolate_from_block"));
    }

    public static <T extends Item> void chocolateBlack(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.FOOD, ctx.get(), 4)
            .pattern("AAA")
            .pattern("BCB")
            .pattern("AAA")
            .define('A', ModItems.COCOA_LIQUOR)
            .define('B', ModItems.COCOA_BUTTER)
            .define('C', Items.SUGAR)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.COCOA_LIQUOR), AnvilCraftDatagen.has(lookup, ModItems.COCOA_LIQUOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CREAM), AnvilCraftDatagen.has(lookup, ModItems.CREAM))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUGAR), AnvilCraftDatagen.has(lookup, Items.SUGAR))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.FOOD, ctx.get(), 9)
            .requires(ModBlocks.BLACK_CHOCOLATE_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.BLACK_CHOCOLATE_BLOCK)
            )
            .save(provider, AnvilCraft.recipe("black_chocolate_from_block"));
    }

    public static <T extends Item> void chocolateWhite(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.FOOD, ctx.get(), 4)
            .pattern("AAA")
            .pattern("BCB")
            .pattern("AAA")
            .define('A', ModItems.COCOA_BUTTER)
            .define('B', ModItems.CREAM)
            .define('C', Items.SUGAR)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.COCOA_BUTTER), AnvilCraftDatagen.has(lookup, ModItems.COCOA_BUTTER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CREAM), AnvilCraftDatagen.has(lookup, ModItems.CREAM))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUGAR), AnvilCraftDatagen.has(lookup, Items.SUGAR))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.FOOD, ctx.get(), 9)
            .requires(ModBlocks.WHITE_CHOCOLATE_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.WHITE_CHOCOLATE_BLOCK)
            )
            .save(provider, AnvilCraft.recipe("white_chocolate_from_block"));
    }

    public static <T extends Item> void creamyBreadRoll(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.FOOD, ctx.get())
            .requires(Items.BREAD)
            .requires(Items.SUGAR)
            .requires(ModItems.CREAM)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CREAM), AnvilCraftDatagen.has(lookup, ModItems.CREAM))
            .save(provider);
    }

    @SuppressWarnings("unused")
    public static <T extends Item> void recipe(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> axe(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.axe(ingredient, (ctx, _) -> new ItemStackTemplate(ctx.get()));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> axe(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStackTemplate> result
    ) {
        return RegistrumItemRecipeLoader.tool("AA", "AB", " B", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> hoe(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.hoe(ingredient, (ctx, _) -> new ItemStackTemplate(ctx.get()));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> hoe(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStackTemplate> result
    ) {
        return RegistrumItemRecipeLoader.tool("AA", " B", " B", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> sword(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.sword(ingredient, (ctx, _) -> new ItemStackTemplate(ctx.get()));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> sword(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStackTemplate> result
    ) {
        return RegistrumItemRecipeLoader.tool("A", "A", "B", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> shovel(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.shovel(ingredient, (ctx, _) -> new ItemStackTemplate(ctx.get()));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> shovel(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStackTemplate> result
    ) {
        return RegistrumItemRecipeLoader.tool("A", "B", "B", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> pickaxe(
        ItemLike ingredient
    ) {
        return RegistrumItemRecipeLoader.pickaxe(ingredient, (ctx, _) -> new ItemStackTemplate(ctx.get()));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> pickaxe(
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStackTemplate> result
    ) {
        return RegistrumItemRecipeLoader.tool("AAA", " B ", " B ", ingredient, result);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumRecipeProvider> tool(
        String pattern1,
        String pattern2,
        String pattern3,
        ItemLike ingredient,
        NonNullBiFunction<DataGenContext<Item, T>, RegistrumRecipeProvider, ItemStackTemplate> result
    ) {
        return (ctx, provider) -> {
            HolderGetter<Item> lookup = provider.getItems();
            ShapedRecipeBuilder.shaped(lookup, RecipeCategory.TOOLS, result.apply(ctx, provider))
                .pattern(pattern1)
                .pattern(pattern2)
                .pattern(pattern3)
                .define('A', ingredient)
                .define('B', Items.STICK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(lookup, ingredient))
                .save(provider);
        };
    }
}
