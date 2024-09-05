package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.anvilcraft.lib.data.provider.RegistratorRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class SmithingRecipesLoader {
    /**
     * 初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistratorRecipeProvider provider) {
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItemTags.ROYAL_STEEL_PICKAXE_BASE),
                        Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_PICKAXE.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_PICKAXE.asItem()),
                        AnvilCraftDatagen.has(ModItems.AMETHYST_PICKAXE))
                .save(provider, AnvilCraft.of("smithing/royal_steel_pickaxe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItemTags.ROYAL_STEEL_AXE_BASE),
                        Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_AXE.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_AXE.asItem()),
                        AnvilCraftDatagen.has(ModItems.AMETHYST_AXE))
                .save(provider, AnvilCraft.of("smithing/royal_steel_axe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItemTags.ROYAL_STEEL_HOE_BASE),
                        Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_HOE.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_HOE.asItem()),
                        AnvilCraftDatagen.has(ModItems.AMETHYST_HOE))
                .save(provider, AnvilCraft.of("smithing/royal_steel_hoe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItemTags.ROYAL_STEEL_SHOVEL_BASE),
                        Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_SHOVEL.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_SHOVEL.asItem()),
                        AnvilCraftDatagen.has(ModItems.AMETHYST_SHOVEL))
                .save(provider, AnvilCraft.of("smithing/royal_steel_shovel"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItemTags.ROYAL_STEEL_SWORD_BASE),
                        Ingredient.of(ModItems.ROYAL_STEEL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_SWORD.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.AMETHYST_SWORD.asItem()),
                        AnvilCraftDatagen.has(ModItems.AMETHYST_SWORD))
                .save(provider, AnvilCraft.of("smithing/royal_steel_sword"));
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(Items.GRINDSTONE),
                        Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                        RecipeCategory.TOOLS, ModBlocks.ROYAL_GRINDSTONE.asItem()
                )
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK.asItem()),
                        AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .unlocks(AnvilCraftDatagen.hasItem(Items.GRINDSTONE), AnvilCraftDatagen.has(Items.GRINDSTONE))
                .save(provider, AnvilCraft.of("smithing/royal_grindstone"));
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(Items.ANVIL),
                        Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.TOOLS, ModBlocks.ROYAL_ANVIL.asItem()
                )
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK.asItem()),
                        AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .unlocks(AnvilCraftDatagen.hasItem(Items.ANVIL), AnvilCraftDatagen.has(Items.ANVIL))
                .save(provider, AnvilCraft.of("smithing/royal_anvil"));
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(Items.SMITHING_TABLE),
                        Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK), RecipeCategory.TOOLS,
                        ModBlocks.ROYAL_SMITHING_TABLE.asItem()
                )
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK.asItem()),
                        AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .unlocks(AnvilCraftDatagen.hasItem(Items.SMITHING_TABLE), AnvilCraftDatagen.has(Items.SMITHING_TABLE))
                .save(provider, AnvilCraft.of("smithing/royal_smithing_table"));
        SmithingTransformRecipeBuilder
            .smithing(
                Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.ANVIL_HAMMER),
                Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                RecipeCategory.TOOLS,
                ModItems.ROYAL_ANVIL_HAMMER.asItem()
            )
            .unlocks(
                AnvilCraftDatagen.hasItem(ModItems.ANVIL_HAMMER),
                AnvilCraftDatagen.has(ModItems.ANVIL_HAMMER)
            )
            .unlocks(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
            )
            .save(
                provider,
                new ResourceLocation("anvilcraft", "anvil_hammer_upgrade")
            );

        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItems.ROYAL_STEEL_PICKAXE), Ingredient.of(ModItems.EMBER_METAL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.EMBER_METAL_PICKAXE.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_PICKAXE.asItem()),
                        AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_PICKAXE))
                .save(provider, AnvilCraft.of("smithing/ember_metal_pickaxe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItems.ROYAL_STEEL_AXE), Ingredient.of(ModItems.EMBER_METAL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.EMBER_METAL_AXE.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_AXE.asItem()),
                        AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_AXE))
                .save(provider, AnvilCraft.of("smithing/ember_metal_axe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItems.ROYAL_STEEL_HOE), Ingredient.of(ModItems.EMBER_METAL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.EMBER_METAL_HOE.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_HOE.asItem()),
                        AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_HOE))
                .save(provider, AnvilCraft.of("smithing/ember_metal_hoe"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItems.ROYAL_STEEL_SHOVEL), Ingredient.of(ModItems.EMBER_METAL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.EMBER_METAL_SHOVEL.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_SHOVEL.asItem()),
                        AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_SHOVEL))
                .save(provider, AnvilCraft.of("smithing/ember_metal_shovel"));
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItems.ROYAL_STEEL_SWORD), Ingredient.of(ModItems.EMBER_METAL_INGOT.get()),
                        RecipeCategory.TOOLS, ModItems.EMBER_METAL_SWORD.get())
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_SWORD.asItem()),
                        AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_SWORD))
                .save(provider, AnvilCraft.of("smithing/ember_metal_sword"));
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModBlocks.ROYAL_ANVIL),
                        Ingredient.of(ModBlocks.EMBER_METAL_BLOCK), RecipeCategory.TOOLS,
                        ModBlocks.EMBER_ANVIL.asItem()
                )
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK.asItem()),
                        AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_ANVIL),
                        AnvilCraftDatagen.has(ModBlocks.ROYAL_ANVIL))
                .save(provider, AnvilCraft.of("smithing/ember_anvil"));
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModBlocks.ROYAL_GRINDSTONE),
                        Ingredient.of(ModBlocks.EMBER_METAL_BLOCK), RecipeCategory.TOOLS,
                        ModBlocks.EMBER_GRINDSTONE.asItem()
                )
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK.asItem()),
                        AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_GRINDSTONE),
                        AnvilCraftDatagen.has(ModBlocks.ROYAL_GRINDSTONE))
                .save(provider, AnvilCraft.of("smithing/ember_grindstone"));
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModBlocks.ROYAL_SMITHING_TABLE),
                        Ingredient.of(ModBlocks.EMBER_METAL_BLOCK), RecipeCategory.TOOLS,
                        ModBlocks.EMBER_SMITHING_TABLE.asItem()
                )
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK.asItem()),
                        AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_SMITHING_TABLE),
                        AnvilCraftDatagen.has(ModBlocks.ROYAL_SMITHING_TABLE))
                .save(provider, AnvilCraft.of("smithing/ember_smithing_table"));
        SmithingTransformRecipeBuilder
                .smithing(
                        Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItems.ROYAL_ANVIL_HAMMER),
                        Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                        RecipeCategory.TOOLS,
                        ModItems.EMBER_ANVIL_HAMMER.asItem()
                )
                .unlocks(
                        AnvilCraftDatagen.hasItem(ModItems.ROYAL_ANVIL_HAMMER),
                        AnvilCraftDatagen.has(ModItems.ROYAL_ANVIL_HAMMER)
                )
                .unlocks(
                        AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                        AnvilCraftDatagen.has(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE)
                )
                .save(
                        provider,
                        new ResourceLocation("anvilcraft", "ember_metal_anvil_hammer_upgrade")
            );
    }
}
