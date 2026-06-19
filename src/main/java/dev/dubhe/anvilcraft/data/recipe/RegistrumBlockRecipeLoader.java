package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

import static dev.dubhe.anvilcraft.AnvilCraft.of;

public class RegistrumBlockRecipeLoader {
    public static <T extends Block> void recipe(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
    }

    public static <T extends Block> void feCollector(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 2)
            .pattern("ABA")
            .pattern("AAA")
            .define('A', Items.COPPER_INGOT)
            .define('B', ModBlocks.CHARGE_COLLECTOR)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.FE_COLLECTOR), AnvilCraftDatagen.has(ModBlocks.FE_COLLECTOR))
            .save(provider);
    }

    public static <T extends Block> void expCollectorBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern(" C ")
            .pattern("ADA")
            .define('A', ModItems.ROYAL_STEEL_INGOT)
            .define('B', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('C', Blocks.SCULK_CATALYST)
            .define('D', ModBlocks.FLUID_TANK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.SCULK_CATALYST), AnvilCraftDatagen.has(Blocks.SCULK_CATALYST))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.FLUID_TANK), AnvilCraftDatagen.has(ModBlocks.FLUID_TANK))
            .save(provider);
    }

    public static <T extends Block> void neoforge(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', ModBlocks.CAKE_BLOCK)
            .define('B', ModFoodItems.CREAMY_BREAD_ROLL)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CAKE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CAKE_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModFoodItems.CREAMY_BREAD_ROLL), AnvilCraftDatagen.has(ModFoodItems.CREAMY_BREAD_ROLL))
            .save(provider);
    }

    public static <T extends Block> void frostAnvil(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModBlocks.ROYAL_ANVIL),
            Ingredient.of(ModBlocks.FROST_METAL_BLOCK),
            RecipeCategory.MISC,
            ctx.get().asItem()
        ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK)).save(provider, AnvilCraft.of("smithing/frost_anvil"));
    }

    public static <T extends Block> void frostGrindstone(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModBlocks.ROYAL_GRINDSTONE),
            Ingredient.of(ModBlocks.FROST_METAL_BLOCK),
            RecipeCategory.MISC,
            ctx.get().asItem()
        ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK)).save(provider, AnvilCraft.of("smithing/frost_grindstone"));
    }

    public static <T extends Block> void frostSmithingTable(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModBlocks.ROYAL_SMITHING_TABLE),
                Ingredient.of(ModBlocks.FROST_METAL_BLOCK),
                RecipeCategory.MISC,
                ctx.get().asItem()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK))
            .save(provider, AnvilCraft.of("smithing/frost_smithing_table"));
    }

    public static <T extends Block> void multiphaseMatterBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.MULTIPHASE_MATTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MULTIPHASE_MATTER), AnvilCraftDatagen.has(ModItems.MULTIPHASE_MATTER))
            .save(provider);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumRecipeProvider> reinforcedConcreteSlab(
        BlockEntry<? extends Block> parent
    ) {
        return (ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
                .pattern("AAA")
                .define('A', parent)
                .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()), AnvilCraftDatagen.has(parent))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(Ingredient.of(parent), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(parent))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumRecipeProvider> reinforcedConcreteStair(
        BlockEntry<? extends Block> parent
    ) {
        return (ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("A  ")
                .pattern("AA ")
                .pattern("AAA")
                .define('A', parent)
                .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()), AnvilCraftDatagen.has(parent))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(Ingredient.of(parent), RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(parent))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumRecipeProvider> reinforcedConcreteWall(
        BlockEntry<? extends Block> parent
    ) {
        return (ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("AAA")
                .pattern("AAA")
                .define('A', parent)
                .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()), AnvilCraftDatagen.has(parent))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(Ingredient.of(parent), RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(parent))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumRecipeProvider> pressurePlateItems(
        String id,
        Item... ingredients
    ) {
        return (ctx, provider) -> {
            for (Item ingredient : ingredients) {
                ResourceLocation location1 = BuiltInRegistries.ITEM.getKey(ingredient);
                ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 1)
                    .pattern("AA")
                    .define('A', ingredient)
                    .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(ingredient))
                    .save(provider, AnvilCraft.of(id + "_from_" + location1.getPath().replace('/', '_')));
            }
        };
    }

    @SafeVarargs
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumRecipeProvider> pressurePlateTags(
        String id,
        TagKey<Item>... ingredients
    ) {
        return (ctx, provider) -> {
            for (TagKey<Item> ingredient : ingredients) {
                ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 1)
                    .pattern("AA")
                    .define('A', ingredient)
                    .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(ingredient))
                    .save(provider, AnvilCraft.of(id + "_from_" + ingredient.location().getPath().replace('/', '_')));
            }
        };
    }

    public static <T extends Block> void magnetBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.MAGNET_INGOT)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.MAGNET_INGOT))
            .save(provider);
    }

    public static <T extends Block> void hollowMagnetBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("AAA")
            .pattern("A A")
            .pattern("AAA")
            .define('A', ModItems.MAGNET_INGOT)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.MAGNET_INGOT))
            .save(provider);
    }

    public static <T extends Block> void ferriteCoreMagnetBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModItems.MAGNET_INGOT)
            .define('B', Tags.Items.INGOTS_IRON)
            .unlockedBy("has_magnet_ingot", RegistrumRecipeProvider.has(ModItems.MAGNET_INGOT))
            .unlockedBy("has_iron_ingot", RegistrumRecipeProvider.has(Tags.Items.INGOTS_IRON))
            .save(provider);
    }

    public static <T extends Block> void stampingPlatform(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("BAB")
            .pattern("B B")
            .pattern("B B")
            .define('A', ModItemTags.IRON_PLATES)
            .define('B', Tags.Items.INGOTS_IRON)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.IRON_PLATES), AnvilCraftDatagen.has(ModItemTags.IRON_PLATES))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Tags.Items.INGOTS_IRON))
            .save(provider);
    }

    public static <T extends Block> void crushingTable(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModBlocks.STAMPING_PLATFORM)
            .requires(Items.GRINDSTONE)
            .unlockedBy("has_" + Items.GRINDSTONE, AnvilCraftDatagen.has(Items.GRINDSTONE))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Tags.Items.INGOTS_IRON))
            .save(provider, AnvilCraft.of("shapeless_crushing_table_recipe"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("BAB")
            .pattern("B B")
            .pattern("B B")
            .define('A', Items.GRINDSTONE)
            .define('B', Tags.Items.INGOTS_IRON)
            .unlockedBy("has_" + Items.GRINDSTONE, AnvilCraftDatagen.has(Items.GRINDSTONE))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Tags.Items.INGOTS_IRON))
            .save(provider, AnvilCraft.of("shaped_crushing_table_recipe"));
    }

    public static <T extends Block> void fishTank(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A A")
            .pattern("B B")
            .pattern("BBB")
            .define('A', Items.IRON_INGOT)
            .define('B', Tags.Items.GLASS_PANES)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
    }

    public static <T extends Block> void fluidTank(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 2)
            .requires(ModItemTags.BRASS_PLATES)
            .requires(ModBlocks.FISH_TANK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.FISH_TANK.asItem()), AnvilCraftDatagen.has(ModBlocks.FISH_TANK))
            .save(provider);
    }

    public static <T extends Block> void neutronIrradiator(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern(" A ")
            .pattern("BCB")
            .pattern("BBB")
            .define('A', Ingredient.of(ModItems.NEUTRONIUM_INGOT, ModItems.CHARGED_NEUTRONIUM_INGOT, ModItems.STABLE_NEUTRONIUM_INGOT))
            .define('B', ModItems.EMBER_METAL_INGOT)
            .define('C', ModBlocks.NEGATIVE_MATTER_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.NEUTRONIUM_INGOT), AnvilCraftDatagen.has(ModItems.NEUTRONIUM_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.CHARGED_NEUTRONIUM_INGOT),
                AnvilCraftDatagen.has(ModItems.CHARGED_NEUTRONIUM_INGOT)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.STABLE_NEUTRONIUM_INGOT),
                AnvilCraftDatagen.has(ModItems.STABLE_NEUTRONIUM_INGOT)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.NEGATIVE_MATTER_BLOCK), AnvilCraftDatagen.has(ModBlocks.NEGATIVE_MATTER_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider);
    }

    public static <T extends Block> void royalAnvil(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(Items.ANVIL),
            Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
            RecipeCategory.MISC,
            ctx.get().asItem()
        ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK)).save(provider, AnvilCraft.of("smithing/royal_anvil"));
    }

    public static <T extends Block> void royalGrindstone(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(Items.GRINDSTONE),
            Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
            RecipeCategory.MISC,
            ctx.get().asItem()
        ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK)).save(provider, AnvilCraft.of("smithing/royal_grindstone"));
    }

    public static <T extends Block> void royalSmithingTable(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(Items.SMITHING_TABLE),
                Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                RecipeCategory.MISC,
                ctx.get().asItem()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("smithing/royal_smithing_table"));
    }

    public static <T extends Block> void emberAnvil(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModBlocks.ROYAL_ANVIL),
            Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
            RecipeCategory.MISC,
            ctx.get().asItem()
        ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK)).save(provider, AnvilCraft.of("smithing/ember_anvil"));
    }

    public static <T extends Block> void emberGrindstone(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(ModBlocks.ROYAL_GRINDSTONE),
            Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
            RecipeCategory.MISC,
            ctx.get().asItem()
        ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK)).save(provider, AnvilCraft.of("smithing/ember_grindstone"));
    }

    public static <T extends Block> void emberSmithingTable(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModBlocks.ROYAL_SMITHING_TABLE),
                Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                RecipeCategory.MISC,
                ctx.get().asItem()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("smithing/ember_smithing_table"));
    }

    public static <T extends Block> void transcendenceAnvil(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModBlocks.EMBER_ANVIL),
                Ingredient.of(ModBlocks.TRANSCENDIUM_BLOCK),
                RecipeCategory.MISC,
                ctx.get().asItem()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.TRANSCENDIUM_BLOCK))
            .save(provider, AnvilCraft.of("smithing/transcendence_anvil"));
    }

    public static <T extends Block> void heater(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("BCB")
            .pattern("BBB")
            .define('A', Items.TERRACOTTA)
            .define('B', Items.IRON_INGOT)
            .define('C', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.TERRACOTTA), AnvilCraftDatagen.has(Items.TERRACOTTA))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .save(provider);
    }

    public static <T extends Block> void transmissionPole(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('B', Items.LIGHTNING_ROD)
            .define('C', Items.IRON_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LIGHTNING_ROD), AnvilCraftDatagen.has(Items.LIGHTNING_ROD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .save(provider);
    }

    public static <T extends Block> void remoteTransmissionPole(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('B', ModBlocks.TRANSMISSION_POLE)
            .define('C', Items.ANVIL)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TRANSMISSION_POLE), AnvilCraftDatagen.has(ModBlocks.TRANSMISSION_POLE))
            .save(provider);
    }

    public static <T extends Block> void teslaTower(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("ACA")
            .pattern("ADA")
            .define('A', ModItems.ROYAL_STEEL_INGOT)
            .define('B', ModBlocks.TOPAZ_BLOCK)
            .define('C', ModBlocks.TRANSMISSION_POLE)
            .define('D', ModItems.CIRCUIT_BOARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TRANSMISSION_POLE), AnvilCraftDatagen.has(ModBlocks.TRANSMISSION_POLE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TOPAZ_BLOCK), AnvilCraftDatagen.has(ModBlocks.TOPAZ_BLOCK))
            .save(provider);
    }

    public static <T extends Block> void inductionLight(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', Items.IRON_INGOT)
            .define('B', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .save(provider);
    }

    public static <T extends Block> void chargeCollector(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern(" A ")
            .pattern("B B")
            .pattern("CCC")
            .define('A', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('B', Items.COPPER_INGOT)
            .define('C', Items.IRON_INGOT)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
    }

    public static <T extends Block> void heliostats(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
            .pattern("S S")
            .pattern("SFS")
            .pattern(" I ")
            .define('S', ModItemTags.SILVER_PLATES)
            .define('F', Items.SUNFLOWER)
            .define('I', Blocks.IRON_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUNFLOWER), AnvilCraftDatagen.has(Items.SUNFLOWER))
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.IRON_BLOCK), AnvilCraftDatagen.has(Blocks.IRON_BLOCK))
            .save(provider, AnvilCraft.of("heliostats_biological"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
            .pattern("SDS")
            .pattern("SCS")
            .pattern(" I ")
            .define('S', ModItemTags.SILVER_PLATES)
            .define('D', Blocks.DAYLIGHT_DETECTOR)
            .define('C', ModItems.PROCESSOR)
            .define('I', Blocks.IRON_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), AnvilCraftDatagen.has(ModItems.PROCESSOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.IRON_BLOCK), AnvilCraftDatagen.has(Blocks.IRON_BLOCK))
            .save(provider, AnvilCraft.of("heliostats_electrical"));
    }

    public static <T extends Block> void loadMonitor(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("A")
            .pattern("B")
            .define('A', Items.COMPASS)
            .define('B', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COMPASS), AnvilCraftDatagen.has(Items.COMPASS))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .save(provider);
    }

    public static <T extends Block> void powerConverterSmall(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.POWER_CONVERTER_BIG), RecipeCategory.MISC, ctx.get(), 64)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_BIG))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_big"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.POWER_CONVERTER_MIDDLE), RecipeCategory.MISC, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_MIDDLE))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_middle"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 64)
            .requires(ModBlocks.POWER_CONVERTER_BIG)
            .unlockedBy("has_big", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_BIG))
            .save(provider, AnvilCraft.of(ctx.getName() + "_from_big"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 8)
            .requires(ModBlocks.POWER_CONVERTER_MIDDLE)
            .unlockedBy("has_middle", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_MIDDLE))
            .save(provider, AnvilCraft.of(ctx.getName() + "_from_middle"));
    }

    public static <T extends Block> void powerConverterMiddle(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModBlocks.POWER_CONVERTER_SMALL, 8)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.POWER_CONVERTER_SMALL),
                AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_SMALL)
            )
            .save(provider, ctx.getId() + "_from_small");
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.POWER_CONVERTER_BIG), RecipeCategory.MISC, ctx.get(), 8)
            .unlockedBy("has_big", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_BIG))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 8)
            .requires(ModBlocks.POWER_CONVERTER_BIG)
            .unlockedBy("has_big", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_BIG))
            .save(provider, AnvilCraft.of(ctx.getName() + "_from_big"));
    }

    public static <T extends Block> void powerConverterBig(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .define('A', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('B', Items.COPPER_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModBlocks.POWER_CONVERTER_MIDDLE, 8)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.POWER_CONVERTER_MIDDLE),
                AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_MIDDLE)
            )
            .save(provider, ctx.getId() + "_from_middle");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModBlocks.POWER_CONVERTER_SMALL, 64)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.POWER_CONVERTER_SMALL),
                AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_SMALL)
            )
            .save(provider, ctx.getId() + "_from_small");
    }

    public static <T extends Block> void piezoelectricCrystal(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("ABA")
            .pattern(" B ")
            .pattern("ABA")
            .define('A', Items.COPPER_INGOT)
            .define('B', Items.QUARTZ_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.QUARTZ_BLOCK), AnvilCraftDatagen.has(Items.QUARTZ_BLOCK))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("ABA")
            .pattern(" B ")
            .pattern("ABA")
            .define('A', Items.COPPER_INGOT)
            .define('B', Items.AMETHYST_BLOCK)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.AMETHYST_BLOCK), AnvilCraftDatagen.has(Items.AMETHYST_BLOCK))
            .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_amethyst");
    }

    public static <T extends Block> void batchCrafter(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("ABA")
            .pattern("ADA")
            .pattern("AEA")
            .define('A', Items.GLASS)
            .define('B', Items.CRAFTER)
            .define('D', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('E', ModItems.CIRCUIT_BOARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.GLASS), AnvilCraftDatagen.has(Items.GLASS))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.CRAFTER), AnvilCraftDatagen.has(Items.CRAFTER))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
            .save(provider);
    }

    public static <T extends Block> void batchCutter(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("ABA")
            .pattern("ADA")
            .pattern("AEA")
            .define('A', Items.GLASS)
            .define('B', Items.STONECUTTER)
            .define('D', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('E', ModItems.CIRCUIT_BOARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.GLASS), AnvilCraftDatagen.has(Items.GLASS))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.STONECUTTER), AnvilCraftDatagen.has(Items.STONECUTTER))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
            .save(provider);
    }

    public static <T extends Block> void itemCollector(DataGenContext<Block, T> c, RegistrumRecipeProvider p) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
            .pattern("ABA")
            .pattern("CDC")
            .pattern("ACA")
            .define('A', Items.IRON_INGOT)
            .define('B', ModItems.MAGNET)
            .define('C', Items.HOPPER)
            .define('D', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MAGNET), AnvilCraftDatagen.has(ModItems.MAGNET))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.HOPPER), AnvilCraftDatagen.has(Items.HOPPER))
            .save(p);
    }

    public static <T extends Block> void heatCollector(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("CBC")
            .pattern("BIB")
            .pattern("RHR")
            .define('B', ModItems.SAPPHIRE)
            .define('C', ModItemTags.COPPER_PLATES)
            .define('H', ModBlocks.CHARGE_COLLECTOR)
            .define('I', Items.BLUE_ICE)
            .define('R', ModItems.ROYAL_STEEL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SAPPHIRE), AnvilCraftDatagen.has(ModItems.SAPPHIRE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.COPPER_PLATES), AnvilCraftDatagen.has(ModItemTags.COPPER_PLATES))
            .unlockedBy("has_charge_collector", AnvilCraftDatagen.has(ModBlocks.CHARGE_COLLECTOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.BLUE_ICE), AnvilCraftDatagen.has(Items.BLUE_ICE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .save(provider);
    }

    public static <T extends Block> void charger(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ADA")
            .pattern("ABA")
            .pattern("CCC")
            .define('A', Items.COPPER_INGOT)
            .define('B', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('C', Items.IRON_INGOT)
            .define('D', Tags.Items.GLASS_PANES)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModBlocks.DISCHARGER)
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DISCHARGER))
            .save(provider, AnvilCraft.of("charger_from_discharger"));
    }

    public static <T extends Block> void discharger(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModBlocks.CHARGER)
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.CHARGER))
            .save(provider, AnvilCraft.of("discharger_from_charger"));
    }

    public static <T extends Block> void activeSilencer(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("ACA")
            .define('A', Items.AMETHYST_BLOCK)
            .define('B', Items.NOTE_BLOCK)
            .define('C', Items.SCULK_SENSOR)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.AMETHYST_BLOCK), AnvilCraftDatagen.has(Items.AMETHYST_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.NOTE_BLOCK), AnvilCraftDatagen.has(Items.NOTE_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SCULK_SENSOR), AnvilCraftDatagen.has(Items.SCULK_SENSOR))
            .save(provider, AnvilCraft.of("active_silencer_old"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("ACA")
            .pattern("BDB")
            .define('A', Items.AMETHYST_BLOCK)
            .define('B', Items.NOTE_BLOCK)
            .define('C', ModItems.PROCESSOR)
            .define('D', ModBlocks.PIEZOELECTRIC_CRYSTAL)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.AMETHYST_BLOCK), AnvilCraftDatagen.has(Items.AMETHYST_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.NOTE_BLOCK), AnvilCraftDatagen.has(Items.NOTE_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), AnvilCraftDatagen.has(ModItems.PROCESSOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PIEZOELECTRIC_CRYSTAL), AnvilCraftDatagen.has(ModBlocks.PIEZOELECTRIC_CRYSTAL))
            .save(provider);
    }

    public static <T extends Block> void blockPlacer(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("AAA")
            .pattern("DCB")
            .pattern("AAA")
            .define('A', Items.COBBLESTONE)
            .define('B', ModItems.CRAB_CLAW)
            .define('C', Items.REDSTONE)
            .define('D', Items.HOPPER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CRAB_CLAW), AnvilCraftDatagen.has(ModItems.CRAB_CLAW))
            .save(provider);
    }

    public static <T extends Block> void smartBlockPlacer(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("AAB")
            .pattern("AC ")
            .pattern("DEA")
            .define('A', Items.IRON_INGOT)
            .define('B', ModItems.CRAB_CLAW)
            .define('C', ModItems.PROCESSOR)
            .define('D', Items.HOPPER)
            .define('E', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CRAB_CLAW), AnvilCraftDatagen.has(ModItems.CRAB_CLAW))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), AnvilCraftDatagen.has(ModItems.PROCESSOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.HOPPER), AnvilCraftDatagen.has(Items.HOPPER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
            .save(provider);
    }

    public static <T extends Block> void structureScanner(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("AB")
            .define('A', Blocks.LECTERN)
            .define('B', Items.ENDER_EYE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.LECTERN), AnvilCraftDatagen.has(Blocks.LECTERN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ENDER_EYE), AnvilCraftDatagen.has(Items.ENDER_EYE))
            .save(provider);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("AB")
            .pattern("CD")
            .define('A', Tags.Items.GLASS_PANES)
            .define('B', ModItems.PROCESSOR)
            .define('C', Blocks.LECTERN)
            .define('D', ModBlocks.RUBY_LASER)
            .unlockedBy(AnvilCraftDatagen.hasItem(Tags.Items.GLASS_PANES), AnvilCraftDatagen.has(Tags.Items.GLASS_PANES))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), AnvilCraftDatagen.has(ModItems.PROCESSOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.LECTERN), AnvilCraftDatagen.has(Blocks.LECTERN))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RUBY_LASER), AnvilCraftDatagen.has(ModBlocks.RUBY_LASER))
            .save(provider, AnvilCraft.of("structure_scanner_alternative"));
    }

    public static <T extends Block> void blockDevourer(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("DA ")
            .pattern("CBA")
            .pattern("DA ")
            .define('A', Items.NETHERITE_INGOT)
            .define('B', Items.DRAGON_HEAD)
            .define('C', Items.REDSTONE)
            .define('D', Items.COBBLESTONE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.NETHERITE_INGOT), AnvilCraftDatagen.has(Items.NETHERITE_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DRAGON_HEAD), AnvilCraftDatagen.has(Items.DRAGON_HEAD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.REDSTONE), AnvilCraftDatagen.has(Items.REDSTONE))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COBBLESTONE), AnvilCraftDatagen.has(Items.COBBLESTONE))
            .save(provider);
    }

    public static <T extends Block> void rubyLaser(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("AEA")
            .pattern("BDB")
            .pattern("ACA")
            .define('A', ModItems.ROYAL_STEEL_INGOT)
            .define('B', ModBlocks.INDUCTION_LIGHT)
            .define('C', ModItemTags.SILVER_PLATES)
            .define('D', ModBlocks.RUBY_BLOCK)
            .define('E', Items.TINTED_GLASS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.INDUCTION_LIGHT), AnvilCraftDatagen.has(ModBlocks.INDUCTION_LIGHT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.SILVER_PLATES), AnvilCraftDatagen.has(ModItemTags.SILVER_PLATES))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RUBY_BLOCK), AnvilCraftDatagen.has(ModBlocks.RUBY_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.TINTED_GLASS), AnvilCraftDatagen.has(Items.TINTED_GLASS))
            .save(provider);
    }

    public static <T extends Block> void rubyPrism(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ACA")
            .pattern("CBC")
            .pattern("AAA")
            .define('A', ModItems.ROYAL_STEEL_INGOT)
            .define('B', ModBlocks.RUBY_BLOCK)
            .define('C', ModItems.RUBY)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT), AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RUBY_BLOCK), AnvilCraftDatagen.has(ModBlocks.RUBY_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RUBY), AnvilCraftDatagen.has(ModItems.RUBY))
            .save(provider);
    }

    public static <T extends Block> void laserReceiver(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("DCD")
            .define('A', ModItems.RUBY)
            .define('B', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
            .define('C', Items.REDSTONE)
            .define('D', ModItems.ROYAL_STEEL_INGOT)
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RUBY))
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
            .unlockedBy("has_item", AnvilCraftDatagen.has(Items.REDSTONE))
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
            .save(provider);
    }

    public static <T extends Block> void blockComparator(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("ABA")
            .pattern(" C ")
            .pattern(" D ")
            .define('A', Blocks.OBSERVER)
            .define('B', Blocks.COMPARATOR)
            .define('C', ModItems.CIRCUIT_BOARD)
            .define('D', Tags.Items.DUSTS_REDSTONE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
            .save(provider);
    }

    public static <T extends Block> void itemDetector(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("CC ")
            .pattern("CBR")
            .pattern("III")
            .define('C', Tags.Items.INGOTS_COPPER)
            .define('B', ModItems.CIRCUIT_BOARD)
            .define('R', Blocks.COMPARATOR)
            .define('I', Tags.Items.INGOTS_IRON)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
            .save(provider);
    }

    public static <T extends Block> void impactPile(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern(" A ")
            .pattern(" B ")
            .pattern(" B ")
            .define('A', Blocks.OBSIDIAN)
            .define('B', Items.NETHERITE_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.OBSIDIAN), AnvilCraftDatagen.has(Blocks.OBSIDIAN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.NETHERITE_INGOT), AnvilCraftDatagen.has(Items.NETHERITE_INGOT))
            .save(provider);
    }

    public static <T extends Block> void overseerBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("ABA")
            .pattern("CBC")
            .define('A', Items.OBSIDIAN)
            .define('B', Items.ENDER_EYE)
            .define('C', ModBlocks.ROYAL_STEEL_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK), AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ENDER_EYE), AnvilCraftDatagen.has(Items.ENDER_EYE))
            .save(provider);
    }

    public static <T extends Block> void jewelCraftingTable(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABC")
            .pattern("DDD")
            .pattern("F F")
            .define('A', Blocks.GRINDSTONE)
            .define('B', Blocks.GLASS)
            .define('C', Blocks.GRINDSTONE)
            .define('D', Blocks.SMOOTH_STONE)
            .define('F', ItemTags.PLANKS)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.GRINDSTONE), AnvilCraftDatagen.has(Blocks.GRINDSTONE))
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.STONECUTTER), AnvilCraftDatagen.has(Blocks.STONECUTTER))
            .save(provider);
    }

    public static <T extends Block> void transparentCraftingTable(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern(" A ")
            .pattern("ABA")
            .pattern(" A ")
            .define('A', Items.AMETHYST_SHARD)
            .define('B', Items.CRAFTING_TABLE)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(Items.AMETHYST_SHARD))
            .save(provider);
    }

    public static <T extends Block> void crabTrap(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("B B")
            .pattern("ABA")
            .define('A', Items.STICK)
            .define('B', Items.STRING)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.STRING))
            .save(provider);
    }

    public static <T extends Block> void chute(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("A A")
            .pattern("ABA")
            .pattern(" A ")
            .define('A', Items.IRON_INGOT)
            .define('B', Items.DROPPER)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER), AnvilCraftDatagen.has(Items.DROPPER))
            .save(provider);
    }

    public static <T extends Block> void magneticChute(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern(" A ")
            .pattern("ABA")
            .pattern("A A")
            .define('A', ModItems.MAGNET_INGOT)
            .define('B', Items.DROPPER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MAGNET_INGOT), AnvilCraftDatagen.has(ModItems.MAGNET_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER), AnvilCraftDatagen.has(Items.DROPPER))
            .save(provider);
    }

    public static <T extends Block> void slidingRail(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 16)
            .pattern("A A")
            .pattern("BAB")
            .pattern("BBB")
            .define('A', Blocks.BLUE_ICE)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.BLUE_ICE), AnvilCraftDatagen.has(Blocks.BLUE_ICE))
            .save(provider);
    }

    public static <T extends Block> void poweredSlidingRail(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 8)
            .pattern("SSS")
            .pattern("SPS")
            .pattern("SSS")
            .define('P', Items.PISTON)
            .define('S', ModBlocks.SLIDING_RAIL)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SLIDING_RAIL), AnvilCraftDatagen.has(ModBlocks.SLIDING_RAIL))
            .save(provider);
    }

    public static <T extends Block> void activatorSlidingRail(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 8)
            .pattern("SSS")
            .pattern("SRS")
            .pattern("SSS")
            .define('R', Blocks.REDSTONE_BLOCK)
            .define('S', ModBlocks.SLIDING_RAIL)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SLIDING_RAIL), AnvilCraftDatagen.has(ModBlocks.SLIDING_RAIL))
            .save(provider);
    }

    public static <T extends Block> void detectorSlidingRail(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 8)
            .pattern("SSS")
            .pattern("SPS")
            .pattern("SSS")
            .define('P', Ingredient.of(Items.STONE_PRESSURE_PLATE, Items.POLISHED_BLACKSTONE_PRESSURE_PLATE))
            .define('S', ModBlocks.SLIDING_RAIL)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SLIDING_RAIL), AnvilCraftDatagen.has(ModBlocks.SLIDING_RAIL))
            .save(provider);
    }

    public static <T extends Block> void slidingRailStop(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("A A")
            .pattern("BAB")
            .pattern("BBB")
            .define('A', Blocks.SOUL_SAND)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.BLUE_ICE), AnvilCraftDatagen.has(Blocks.BLUE_ICE))
            .save(provider);
    }

    public static <T extends Block> void voidEnergyCollector(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("CCC")
            .define('A', ModBlocks.VOID_MATTER_BLOCK)
            .define('B', ModBlocks.CHARGE_COLLECTOR)
            .define('C', ModBlocks.HEAVY_IRON_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.VOID_MATTER_BLOCK), AnvilCraftDatagen.has(ModBlocks.VOID_MATTER_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHARGE_COLLECTOR), AnvilCraftDatagen.has(ModBlocks.CHARGE_COLLECTOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK), AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider);
    }

    public static <T extends Block> void magnetoElectricCoreBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("BCB")
            .pattern("ABA")
            .define('A', Tags.Items.INGOTS_COPPER)
            .define('B', Tags.Items.GLASS_BLOCKS)
            .define('C', ModBlocks.HOLLOW_MAGNET_BLOCK)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HOLLOW_MAGNET_BLOCK))
            .save(provider);
    }

    public static <T extends Block> void propelPiston(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ItemStack itemStack = new ItemStack(ctx.get());
        itemStack.set(ModComponents.STORED_ENERGY, 8000000);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, itemStack)
            .pattern("CDC")
            .pattern("ABA")
            .pattern("AEA")
            .define('A', ModItems.IONOCRAFT)
            .define('B', ModItems.CAPACITOR)
            .define('C', Items.IRON_INGOT)
            .define('D', Items.PISTON)
            .define('E', ModItems.RUBY)
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.IONOCRAFT))
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.CAPACITOR))
            .unlockedBy("has_item", AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy("has_item", AnvilCraftDatagen.has(Items.PISTON))
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RUBY))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("CDC")
            .pattern("ABA")
            .pattern("AEA")
            .define('A', ModItems.IONOCRAFT)
            .define('B', ModItems.CAPACITOR_EMPTY)
            .define('C', Items.IRON_INGOT)
            .define('D', Items.PISTON)
            .define('E', ModItems.RUBY)
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.IONOCRAFT))
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.CAPACITOR_EMPTY))
            .unlockedBy("has_item", AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy("has_item", AnvilCraftDatagen.has(Items.PISTON))
            .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RUBY))
            .save(provider, "empty_propel_piston");
    }

    public static <T extends Block> void royalSteelBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.ROYAL_STEEL_INGOT)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
            .save(provider);
    }

    public static <T extends Block> void smoothRoyalSteelBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/smooth_royal_steel_block"));
    }

    public static <T extends Block> void cutRoyalSteelBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_block"));
    }

    public static <T extends Block> void cutRoyalSteelPillar(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_ROYAL_STEEL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_pillar_from_cut_royal_steel_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_pillar_from_royal_steel_block"));
    }

    public static <T extends Block> void cutRoyalSteelSlab(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_slab_from_royal_steel_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_ROYAL_STEEL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_slab_from_cut_royal_steel_block"));
    }

    public static <T extends Block> void cutRoyalSteelStairs(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_stairs_from_royal_steel_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_ROYAL_STEEL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 1)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_stairs_from_cut_royal_steel_block"));
    }

    public static <T extends Block> void frostMetalBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.FROST_METAL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), RegistrumRecipeProvider.has(ModItems.FROST_METAL_INGOT))
            .save(provider);
    }

    public static <T extends Block> void cutFrostMetalBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FROST_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_frost_metal_block"));
    }

    public static <T extends Block> void cutFrostMetalPillar(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FROST_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_frost_metal_pillar_from_frost_metal_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_FROST_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_frost_metal_pillar_from_cut_frost_metal_block"));
    }

    public static <T extends Block> void cutFrostMetalSlab(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FROST_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_frost_metal_slab_from_frost_metal_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_FROST_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_frost_metal_slab_from_cut_frost_metal_block"));
    }

    public static <T extends Block> void cutFrostMetalStairs(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FROST_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_frost_metal_stairs_from_frost_metal_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_FROST_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 1)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_frost_metal_stairs_from_cut_frost_metal_block"));
    }

    public static <T extends Block> void emberMetalBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.EMBER_METAL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), RegistrumRecipeProvider.has(ModItems.EMBER_METAL_INGOT))
            .save(provider);
    }

    public static <T extends Block> void cutEmberMetalBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.EMBER_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_block"));
    }

    public static <T extends Block> void cutEmberMetalPillar(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.EMBER_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_pillar_from_ember_metal_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_EMBER_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_pillar_from_cut_ember_metal_block"));
    }

    public static <T extends Block> void cutEmberMetalSlab(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.EMBER_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_slab_from_ember_metal_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_EMBER_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_slab_from_cut_ember_metal_block"));
    }

    public static <T extends Block> void cutEmberMetalStairs(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.EMBER_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_stairs_from_ember_metal_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_EMBER_METAL_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 1)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_EMBER_METAL_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_stairs_from_cut_ember_metal_block"));
    }

    public static <T extends Block> void transcendiumBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.TRANSCENDIUM_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT), RegistrumRecipeProvider.has(ModItems.TRANSCENDIUM_INGOT))
            .save(provider);
    }

    public static <T extends Block> void heavyIronBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', Tags.Items.STORAGE_BLOCKS_IRON)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(Tags.Items.STORAGE_BLOCKS_IRON))
            .save(provider);
    }

    public static <T extends Block> void polishedHeavyIronBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
    }

    public static <T extends Block> void polishedHeavyIronSlab(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                2
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
    }

    public static <T extends Block> void polishedHeavyIronStairs(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
    }

    public static <T extends Block> void cutHeavyIronBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                4
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
    }

    public static <T extends Block> void cutHeavyIronSlab(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 16)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                8
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
    }

    public static <T extends Block> void cutHeavyIronStairs(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                4
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
    }

    public static <T extends Block> void heavyIronPlate(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 16)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                8
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_SLAB),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                4
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_SLAB))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_slab"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_HEAVY_IRON_SLAB), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_SLAB))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_slab"));
    }

    public static <T extends Block> void heavyIronColumn(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                4
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
    }

    public static <T extends Block> void heavyIronBeam(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                4
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
    }

    public static <T extends Block> void heavyIronWall(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                4
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
    }

    public static <T extends Block> void heavyIronDoor(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                2
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
    }

    public static <T extends Block> void heavyIronTrapdoor(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 8)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                4
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
    }

    public static <T extends Block> void cursedGoldBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.CURSED_GOLD_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT), AnvilCraftDatagen.has(ModItems.CURSED_GOLD_INGOT))
            .save(provider);
    }

    public static <T extends Block> void zincBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.ZINC_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS), AnvilCraftDatagen.has(ModItemTags.ZINC_INGOTS))
            .save(provider);
    }

    public static <T extends Block> void tinBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.TIN_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS), AnvilCraftDatagen.has(ModItemTags.TIN_INGOTS))
            .save(provider);
    }

    public static <T extends Block> void titaniumBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.TITANIUM_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS), AnvilCraftDatagen.has(ModItemTags.TITANIUM_INGOTS))
            .save(provider);
    }

    public static <T extends Block> void tungstenBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.TUNGSTEN_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS), AnvilCraftDatagen.has(ModItemTags.TUNGSTEN_INGOTS))
            .save(provider);
    }

    public static <T extends Block> void leadBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.LEAD_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS), AnvilCraftDatagen.has(ModItemTags.LEAD_INGOTS))
            .save(provider);
    }

    public static <T extends Block> void silverBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.SILVER_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS), AnvilCraftDatagen.has(ModItemTags.SILVER_INGOTS))
            .save(provider);
    }

    public static <T extends Block> void uraniumBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.URANIUM_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS), AnvilCraftDatagen.has(ModItemTags.URANIUM_INGOTS))
            .save(provider);
    }

    public static <T extends Block> void plutoniumBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItemTags.PLUTONIUM_INGOTS)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.PLUTONIUM_INGOTS), AnvilCraftDatagen.has(ModItemTags.PLUTONIUM_INGOTS))
            .save(provider);
    }

    public static <T extends Block> void topazBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.TOPAZ)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TOPAZ), AnvilCraftDatagen.has(ModItems.TOPAZ))
            .save(provider);
    }

    public static <T extends Block> void rubyBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RUBY)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RUBY), AnvilCraftDatagen.has(ModItems.RUBY))
            .save(provider);
    }

    public static <T extends Block> void sapphireBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.SAPPHIRE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SAPPHIRE), AnvilCraftDatagen.has(ModItems.SAPPHIRE))
            .save(provider);
    }

    public static <T extends Block> void expGemBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.EXP_GEM)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EXP_GEM), AnvilCraftDatagen.has(ModItems.EXP_GEM))
            .save(provider);
    }

    public static <T extends Block> void resinBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RESIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RESIN), AnvilCraftDatagen.has(ModItems.RESIN))
            .save(provider);
    }

    public static <T extends Block> void amberBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.AMBER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.AMBER), AnvilCraftDatagen.has(ModItems.AMBER))
            .save(provider);
    }

    public static <T extends Item> void levitationPowderBlock(DataGenContext<Item, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ctx.get())
            .requires(ModItems.LEVITATION_POWDER, 9)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.LEVITATION_POWDER), AnvilCraftDatagen.has(ModItems.LEVITATION_POWDER))
            .save(provider, ctx.getId().withSuffix("_from_powders"));
    }

    public static <T extends Block> void controllableSand(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ctx.get())
            .pattern("LRL")
            .pattern("RSR")
            .pattern("LRL")
            .define('L', ModItems.LEVITATION_POWDER)
            .define('R', Items.REDSTONE)
            .define('S', ItemTags.SAND)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.LEVITATION_POWDER), AnvilCraftDatagen.has(ModItems.LEVITATION_POWDER))
            .save(provider);
    }

    public static <T extends Block> void chocolateBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModFoodItems.CHOCOLATE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModFoodItems.CHOCOLATE), AnvilCraftDatagen.has(ModFoodItems.CHOCOLATE))
            .save(provider);
    }

    public static <T extends Block> void blackChocolateBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModFoodItems.CHOCOLATE_BLACK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModFoodItems.CHOCOLATE_BLACK), AnvilCraftDatagen.has(ModFoodItems.CHOCOLATE_BLACK))
            .save(provider);
    }

    public static <T extends Block> void whiteChocolateBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModFoodItems.CHOCOLATE_WHITE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModFoodItems.CHOCOLATE_WHITE), AnvilCraftDatagen.has(ModFoodItems.CHOCOLATE_WHITE))
            .save(provider);
    }

    public static <T extends Block> void chocolateSlab(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
            .pattern("AAA")
            .define('A', ModBlocks.CHOCOLATE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
            .save(provider);
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CHOCOLATE_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
    }

    public static <T extends Block> void blackChocolateSlab(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
            .pattern("AAA")
            .define('A', ModBlocks.BLACK_CHOCOLATE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK))
            .save(provider);
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.BLACK_CHOCOLATE_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
    }

    public static <T extends Block> void whiteChocolateSlab(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
            .pattern("AAA")
            .define('A', ModBlocks.WHITE_CHOCOLATE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK))
            .save(provider);
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.WHITE_CHOCOLATE_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
    }

    public static <T extends Block> void chocolateStairs(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .pattern("A  ")
            .pattern("AA ")
            .pattern("AAA")
            .define('A', ModBlocks.CHOCOLATE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
            .save(provider);
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CHOCOLATE_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
    }

    public static <T extends Block> void blackChocolateStairs(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .pattern("A  ")
            .pattern("AA ")
            .pattern("AAA")
            .define('A', ModBlocks.BLACK_CHOCOLATE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK))
            .save(provider);
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.BLACK_CHOCOLATE_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
    }

    public static <T extends Block> void whiteChocolateStairs(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .pattern("A  ")
            .pattern("AA ")
            .pattern("AAA")
            .define('A', ModBlocks.WHITE_CHOCOLATE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK))
            .save(provider);
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.WHITE_CHOCOLATE_BLOCK), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
    }

    public static <T extends Block> void rawZincBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_ZINC)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_ZINC), AnvilCraftDatagen.has(ModItems.RAW_ZINC))
            .save(provider);
    }

    public static <T extends Block> void rawTinBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TIN), AnvilCraftDatagen.has(ModItems.RAW_TIN))
            .save(provider);
    }

    public static <T extends Block> void rawTitaniumBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TITANIUM)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TITANIUM), AnvilCraftDatagen.has(ModItems.RAW_TITANIUM))
            .save(provider);
    }

    public static <T extends Block> void rawTungstenBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TUNGSTEN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TUNGSTEN), AnvilCraftDatagen.has(ModItems.RAW_TUNGSTEN))
            .save(provider);
    }

    public static <T extends Block> void rawLeadBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_LEAD)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_LEAD), AnvilCraftDatagen.has(ModItems.RAW_LEAD))
            .save(provider);
    }

    public static <T extends Block> void rawSilverBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_SILVER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_SILVER), AnvilCraftDatagen.has(ModItems.RAW_SILVER))
            .save(provider);
    }

    public static <T extends Block> void rawUraniumBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_URANIUM)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_URANIUM), AnvilCraftDatagen.has(ModItems.RAW_URANIUM))
            .save(provider);
    }

    public static <T extends Block> void voidMatterBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.VOID_MATTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER), AnvilCraftDatagen.has(ModItems.VOID_MATTER))
            .save(provider);
    }

    public static <T extends Block> void earthCoreShardBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.EARTH_CORE_SHARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD), AnvilCraftDatagen.has(ModItems.EARTH_CORE_SHARD))
            .save(provider);
    }

    public static <T extends Block> void negativeMatterBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.NEGATIVE_MATTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.NEGATIVE_MATTER), RegistrumRecipeProvider.has(ModItems.NEGATIVE_MATTER))
            .save(provider);
    }

    public static <T extends Block> void confinedNeutroniumIngotBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ItemInjectRecipe.builder()
            .inputBlock(ModBlocks.CONFINEMENT_CHAMBER)
            .requires(ModItems.CHARGED_NEUTRONIUM_INGOT)
            .resultBlock(ctx)
            .save(provider);
    }

    public static <T extends Block> void confinementChamber(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("B B")
            .pattern("ABA")
            .define('A', ModItems.TRANSCENDIUM_NUGGET)
            .define('B', ModItems.MAGNET_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_NUGGET), AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_NUGGET))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MAGNET_INGOT), AnvilCraftDatagen.has(ModItems.MAGNET_INGOT))
            .save(provider);
    }

    public static <T extends Block> void singularityCrystal(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        MultiblockRecipe.builder(ctx.get(), 1)
            .layer("ABA", "BAB", "ABA")
            .layer("BAB", "ABA", "BAB")
            .layer("ABA", "BAB", "ABA")
            .symbol('A', ModBlocks.CONFINED_NEUTRONIUM_INGOT_BLOCK)
            .symbol('B', ModBlocks.NEGATIVE_MATTER_BLOCK)
            .save(provider);
    }

    public static <T extends Block> void sugarBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .requires(Items.SUGAR, 9)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(Items.SUGAR))
            .save(provider);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, Items.SUGAR, 9)
            .requires(ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ctx.get()))
            .save(provider, of("sugar_from_sugar_block"));
    }

    public static <T extends Block> void gunpowerBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .requires(Items.GUNPOWDER, 9)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(Items.GUNPOWDER))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, Items.GUNPOWDER, 9)
            .requires(ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ctx.get()))
            .save(provider, of("gunpowder_from_gunpowder_block"));
    }

    public static <T extends Block> void rottenFleshBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .requires(Items.ROTTEN_FLESH, 9)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(Items.ROTTEN_FLESH))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, Items.ROTTEN_FLESH, 9)
            .requires(ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ctx.get()))
            .save(provider, of("rotten_flesh_from_rotten_flesh_block"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ctx.get()), RecipeCategory.MISC, Items.NETHERRACK, 0.0F, 200)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ctx.get()))
            .save(provider);
    }

    public static <T extends Block> void flintBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .requires(Items.FLINT, 9)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(Items.FLINT))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, Items.FLINT, 9)
            .requires(ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ctx.get()))
            .save(provider, of("flint_from_flint_block"));
    }

    public static <T extends Block> void polishedFlintBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .pattern("AA")
            .pattern("AA")
            .define('A', ModBlocks.FLINT_BLOCK)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FLINT_BLOCK))
            .save(provider, of("shaped/polished_flint_block"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FLINT_BLOCK))
            .save(provider, of("stonecutting/polished_flint_block"));
    }

    public static <T extends Block> void cutFlintBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .pattern("AA")
            .pattern("AA")
            .define('A', ModBlocks.POLISHED_FLINT_BLOCK)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_FLINT_BLOCK))
            .save(provider, of("shaped/cut_flint_block"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_block_from_flint_block"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.POLISHED_FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_block_from_polished_flint_block"));
    }

    public static <T extends Block> void cutFlintSlabBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
            .pattern("AAA")
            .define('A', ModBlocks.CUT_FLINT_BLOCK)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_FLINT_BLOCK))
            .save(provider, of("shaped/cut_flint_slab"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_slab_from_flint_block"));

        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_FLINT_BLOCK.get()),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                2
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_slab_from_polished_flint_block"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_slab_from_cut_flint_block"));
    }

    public static <T extends Block> void cutFlintStairsBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .pattern("A  ")
            .pattern("AA ")
            .pattern("AAA")
            .define('A', ModBlocks.CUT_FLINT_BLOCK)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_FLINT_BLOCK))
            .save(provider, of("shaped/cut_flint_stairs"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_stairs_from_flint_block"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.POLISHED_FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_stairs_from_polished_flint_block"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_stairs_from_cut_flint_block"));
    }

    public static <T extends Block> void cutFlintPillarBlock(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 2)
            .pattern("A")
            .pattern("A")
            .define('A', ModBlocks.CUT_FLINT_BLOCK)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_FLINT_BLOCK))
            .save(provider, of("shaped/cut_flint_pillar"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_pillar_from_flint_block"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.POLISHED_FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_pillar_from_polished_flint_block"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModBlocks.CUT_FLINT_BLOCK.get()), RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_FLINT_BLOCK))
            .save(provider, of("stonecutting/cut_flint_pillar_from_cut_flint_block"));
    }

    public static <T extends Block> void plywood(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ItemCompressRecipe.builder()
            .requires(ModItems.WOOD_FIBER, 4)
            .requires(ModItems.RESIN)
            .result(ctx.get(), 16)
            .save(provider);
    }

    public static <T extends Block> void pulseGenerator(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("BCR")
            .pattern("III")
            .define('B', ModItems.CIRCUIT_BOARD)
            .define('C', Items.CLOCK)
            .define('I', Items.IRON_INGOT)
            .define('R', Items.REDSTONE_TORCH)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
    }

    public static <T extends Block> void advancedComparator(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern(" R ")
            .pattern("CBC")
            .pattern("III")
            .define('B', ModItems.CIRCUIT_BOARD)
            .define('C', Items.COMPARATOR)
            .define('I', Items.IRON_INGOT)
            .define('R', Items.REDSTONE_TORCH)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
    }

    public static <T extends Block> void redstoneComputer(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
            .pattern("BDB")
            .pattern("BPB")
            .pattern("BIB")
            .define('B', ModItems.CIRCUIT_BOARD)
            .define('P', ModItems.PROCESSOR)
            .define('I', Items.IRON_BLOCK)
            .define('D', ModItems.DISK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), AnvilCraftDatagen.has(ModItems.PROCESSOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DISK), AnvilCraftDatagen.has(ModItems.DISK))
            .save(provider);
    }

    public static <T extends Block> void copperPressurePlate(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ResourceLocation location1 = BuiltInRegistries.ITEM.getKey(Items.COPPER_INGOT);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 1)
            .pattern("AA")
            .define('A', Items.COPPER_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
            .save(provider, AnvilCraft.of("copper_pressure_plate_from_" + location1.getPath().replace('/', '_')));
    }

    public static <T extends Block> void tradingStation(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("WWW")
            .pattern("S S")
            .pattern("PBP")
            .define('B', Blocks.BARREL)
            .define('P', ItemTags.PLANKS)
            .define('S', Items.STICK)
            .define('W', ItemTags.WOOL)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.BARREL), AnvilCraftDatagen.has(Blocks.BARREL))
            .unlockedBy(AnvilCraftDatagen.hasItem(ItemTags.PLANKS), AnvilCraftDatagen.has(ItemTags.PLANKS))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.STICK), AnvilCraftDatagen.has(Items.STICK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ItemTags.WOOL), AnvilCraftDatagen.has(ItemTags.WOOL))
            .save(provider);
    }

    // === Celestial Forging Anvil components ===

    public static <T extends Block> void cfaLogisticsInterface(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
            .pattern("ABA")
            .pattern("BCB")
            .pattern("ABA")
            .define('A', ModBlocks.CHUTE)
            .define('B', ModBlocks.MAGNETIC_CHUTE)
            .define('C', ModBlocks.SPACETIME_SUPERCOMPUTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SPACETIME_SUPERCOMPUTER),
                RegistrumRecipeProvider.has(ModBlocks.SPACETIME_SUPERCOMPUTER))
            .save(provider);
    }

    public static <T extends Block> void cfaFluidInterface(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
            .pattern("ABA")
            .pattern("BCB")
            .pattern("ABA")
            .define('A', ModBlocks.FLUID_TANK)
            .define('B', ModBlocks.LARGE_FLUID_TANK)
            .define('C', ModBlocks.SPACETIME_SUPERCOMPUTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SPACETIME_SUPERCOMPUTER),
                RegistrumRecipeProvider.has(ModBlocks.SPACETIME_SUPERCOMPUTER))
            .save(provider);
    }

    public static <T extends Block> void cfaLaserInterface(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
            .pattern("ABA")
            .pattern("BCB")
            .pattern("ABA")
            .define('A', ModBlocks.LASER_RECEIVER)
            .define('B', ModBlocks.RUBY_LASER)
            .define('C', ModBlocks.SPACETIME_SUPERCOMPUTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SPACETIME_SUPERCOMPUTER),
                RegistrumRecipeProvider.has(ModBlocks.SPACETIME_SUPERCOMPUTER))
            .save(provider);

        // Shapeless: spacetime_supercomputer + large_laser → 16
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 16)
            .requires(ModBlocks.SPACETIME_SUPERCOMPUTER)
            .requires(ModBlocks.LARGE_LASER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SPACETIME_SUPERCOMPUTER),
                RegistrumRecipeProvider.has(ModBlocks.SPACETIME_SUPERCOMPUTER))
            .save(provider, of("celestial_forging_anvil_laser_interface_from_large_laser"));
    }

    public static <T extends Block> void cfaAmplifier(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ItemInjectRecipe.builder()
            .requires(ModBlocks.SPACETIME_SUPERCOMPUTER)
            .inputBlock(ModBlocks.GIANT_ANVIL)
            .resultBlock(ctx)
            .save(provider);
    }

    public static <T extends Block> void cfaInterfacePlaceholder(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModItems.TRANSCENDIUM_NUGGET),
                RecipeCategory.BUILDING_BLOCKS,
                ctx.get(),
                4
            )
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_NUGGET))
            .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
    }

    public static <T extends Block> void celestialForgingAnvilPortal(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
            .pattern(" T ")
            .pattern("TET")
            .pattern("TST")
            .define('T', ModItems.TRANSCENDIUM_INGOT)
            .define('E', Items.ENDER_PEARL)
            .define('S', ModBlocks.SPACETIME_SUPERCOMPUTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT),
                AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SPACETIME_SUPERCOMPUTER),
                RegistrumRecipeProvider.has(ModBlocks.SPACETIME_SUPERCOMPUTER))
            .save(provider);
    }

    public static <T extends Block> void infiniteCollector(DataGenContext<Block, T> ctx, RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern(" C ")
            .pattern("CHC")
            .pattern("TTT")
            .define('C', ModBlocks.CHARGE_COLLECTOR)
            .define('H', ModBlocks.HEAT_COLLECTOR)
            .define('T', ModItems.TRANSCENDIUM_INGOT)
            .unlockedBy("has_charge_collector", AnvilCraftDatagen.has(ModBlocks.CHARGE_COLLECTOR))
            .unlockedBy("has_heat_collector", AnvilCraftDatagen.has(ModBlocks.HEAT_COLLECTOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT),
                AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_INGOT))
            .save(provider);
    }
}
