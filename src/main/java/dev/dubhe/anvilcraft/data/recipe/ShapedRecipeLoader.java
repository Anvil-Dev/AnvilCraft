package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.NeutroniumChargingRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;

public class ShapedRecipeLoader {
    public ShapedRecipeLoader(RegistrumRecipeProvider provider) {
        this.nineToOne(provider);
        this.chargedNeutroniumIngot(provider);
        this.controlValve(provider);
        this.crate(provider);
        this.storagePort(provider);
        this.storageFluidPort(provider);
        this.storagePortConsolidator(provider);
    }

    public void nineToOne(RegistrumRecipeProvider provider) {
        this.nine21(provider, RecipeCategory.BUILDING_BLOCKS, ModItemTags.BRONZE_INGOTS, ModBlocks.BRONZE_BLOCK);
        this.nine21(provider, RecipeCategory.BUILDING_BLOCKS, ModItemTags.BRASS_INGOTS, ModBlocks.BRASS_BLOCK);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PLYWOOD_STAIRS, 4)
            .pattern("A  ")
            .pattern("AA ")
            .pattern("AAA")
            .define('A', ModBlocks.PLYWOOD_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLYWOOD_BLOCK), AnvilCraftDatagen.has(ModBlocks.PLYWOOD_BLOCK))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PLYWOOD_SLAB, 6)
            .pattern("AAA")
            .define('A', ModBlocks.PLYWOOD_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLYWOOD_BLOCK), AnvilCraftDatagen.has(ModBlocks.PLYWOOD_BLOCK))
            .save(provider);
    }

    private void chargedNeutroniumIngot(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHARGED_NEUTRONIUM_INGOT)
            .pattern("SSS")
            .pattern("SNS")
            .pattern("SSS")
            .define('S', ModItems.SUPER_CAPACITOR)
            .define('N', ModItemTags.UNCHARGED_NEUTRONIUM_INGOTS)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.SUPER_CAPACITOR),
                AnvilCraftDatagen.has(ModItems.CHARGED_NEUTRONIUM_INGOT)
            )
            .save(new RecipeOutput() {
                @Override
                public Advancement.Builder advancement() {
                    return provider.advancement();
                }

                @Override
                public void accept(
                    ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions
                ) {
                    provider.accept(id, new NeutroniumChargingRecipe((ShapedRecipe) recipe), advancement, conditions);
                }
            });
    }

    private void controlValve(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CONTROL_VALVE)
            .pattern(" C ")
            .pattern("PHP")
            .define('C', ModItems.CIRCUIT_BOARD)
            .define('P', ModItems.PIPE)
            .define('H', ModBlocks.CHUTE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PIPE), AnvilCraftDatagen.has(ModItems.PIPE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHUTE), AnvilCraftDatagen.has(ModBlocks.CHUTE))
            .save(provider);
    }

    private void crate(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRATE, 2)
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ItemTags.PLANKS)
            .define('B', ModItems.RESIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ItemTags.PLANKS), AnvilCraftDatagen.has(ItemTags.PLANKS))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RESIN), AnvilCraftDatagen.has(ModItems.RESIN))
            .save(provider);
    }

    private void storagePort(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STORAGE_PORT, 4)
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', Items.SHULKER_SHELL)
            .define('B', ModBlocks.CRATE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SHULKER_SHELL), AnvilCraftDatagen.has(Items.SHULKER_SHELL))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CRATE), AnvilCraftDatagen.has(ModBlocks.CRATE))
            .save(provider);
    }

    private void storageFluidPort(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STORAGE_FLUID_PORT, 1)
            .pattern(" A ")
            .pattern("BBB")
            .pattern(" A ")
            .define('A', Items.SHULKER_SHELL)
            .define('B', ModBlocks.FLUID_TANK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SHULKER_SHELL), AnvilCraftDatagen.has(Items.SHULKER_SHELL))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.FLUID_TANK), AnvilCraftDatagen.has(ModBlocks.FLUID_TANK))
            .save(provider);
    }

    private void storagePortConsolidator(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STORAGE_PORT_CONSOLIDATOR)
            .pattern(" P ")
            .pattern("PSP")
            .pattern(" P ")
            .define('P', ModItems.PROCESSOR)
            .define('S', ModBlocks.STORAGE_PORT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), AnvilCraftDatagen.has(ModItems.PROCESSOR))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.STORAGE_PORT),
                AnvilCraftDatagen.has(ModBlocks.STORAGE_PORT)
            )
            .save(provider);
    }

    private void nine21(
        RegistrumRecipeProvider provider,
        RecipeCategory recipeCategory,
        ItemLike ingredient,
        ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(recipeCategory, result)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ingredient)
            .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(result))
            .save(provider);
    }

    private void nine21(
        RegistrumRecipeProvider provider,
        @SuppressWarnings("SameParameterValue")
        RecipeCategory recipeCategory,
        TagKey<Item> ingredient,
        ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(recipeCategory, result)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ingredient)
            .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(result))
            .save(provider);
    }
}
