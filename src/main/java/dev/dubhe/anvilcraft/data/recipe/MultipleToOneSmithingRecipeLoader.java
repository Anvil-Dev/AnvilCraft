package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.api.recipe.data.MultiphaseData;
import dev.dubhe.anvilcraft.api.recipe.data.NormalDataComponent;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.multiple.EightToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.FourToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.TwoToOneSmithingRecipe;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

public class MultipleToOneSmithingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        MultipleToOneSmithingRecipeLoader.two(provider);
        MultipleToOneSmithingRecipeLoader.four(provider);
        MultipleToOneSmithingRecipeLoader.eight(provider);
    }

    public static void two(RegistrumRecipeProvider provider) {
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModItems.EMBER_ANVIL_HAMMER)
            .input(ModItems.FROST_ANVIL_HAMMER)
            .resultCopy(ModItems.TRANSCENDENCE_ANVIL_HAMMER, MultiphaseData.two())
            .save(provider);
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModItems.EMBER_DRAGON_ROD)
            .input(ModItems.FROST_DRAGON_ROD)
            .resultCopy(ModItems.TRANSCENDENCE_DRAGON_ROD, MultiphaseData.two())
            .save(provider);
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModItems.EMBER_METAL_HEAVY_HALBERD)
            .input(ModItems.FROST_METAL_HEAVY_HALBERD)
            .resultCopy(ModItems.TRANSCENDENCE_HEAVY_HALBERD, MultiphaseData.two())
            .save(provider);
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModItems.EMBER_METAL_RESONATOR)
            .input(ModItems.FROST_METAL_RESONATOR)
            .resultCopy(ModItems.TRANSCENDENCE_RESONATOR, MultiphaseData.two())
            .save(provider);
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModBlocks.EMBER_ANVIL)
            .input(ModBlocks.FROST_ANVIL)
            .result(ModBlocks.TRANSCENDENCE_ANVIL)
            .save(provider);
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModBlocks.EMBER_GRINDSTONE)
            .input(ModBlocks.FROST_GRINDSTONE)
            .result(ModBlocks.TRANSCENDENCE_GRINDSTONE)
            .save(provider);
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModBlocks.EMBER_SMITHING_TABLE)
            .input(ModBlocks.FROST_SMITHING_TABLE)
            .result(ModBlocks.TRANSCENDENCE_SMITHING_TABLE)
            .save(provider);
    }

    public static void four(RegistrumRecipeProvider provider) {
        FourToOneSmithingRecipe.builder()
            .material(ModItems.HEAVY_HALBERD_CORE)
            .input(ModItems.FROST_METAL_SWORD)
            .input(ModItems.FROST_METAL_AXE)
            .input(Items.TRIDENT)
            .input(Tags.Items.TOOLS_MACE)
            .resultMerge(ModItems.FROST_METAL_HEAVY_HALBERD, NormalDataComponent.frostFour())
            .save(provider);
        FourToOneSmithingRecipe.builder()
            .material(ModItems.HEAVY_HALBERD_CORE)
            .input(ModItems.EMBER_METAL_SWORD)
            .input(ModItems.EMBER_METAL_AXE)
            .input(Items.TRIDENT)
            .input(Tags.Items.TOOLS_MACE)
            .resultMerge(ModItems.EMBER_METAL_HEAVY_HALBERD, NormalDataComponent.emberFour())
            .save(provider);
        FourToOneSmithingRecipe.builder()
            .material(ModItems.RESONATOR_CORE)
            .input(ModItems.FROST_METAL_AXE)
            .input(ModItems.FROST_METAL_SHOVEL)
            .input(ModItems.FROST_METAL_HOE)
            .input(ModItems.FROST_METAL_PICKAXE)
            .resultMerge(ModItems.FROST_METAL_RESONATOR, NormalDataComponent.frostFour())
            .save(provider);
        FourToOneSmithingRecipe.builder()
            .material(ModItems.RESONATOR_CORE)
            .input(ModItems.EMBER_METAL_AXE)
            .input(ModItems.EMBER_METAL_SHOVEL)
            .input(ModItems.EMBER_METAL_HOE)
            .input(ModItems.EMBER_METAL_PICKAXE)
            .resultMerge(ModItems.EMBER_METAL_RESONATOR, NormalDataComponent.emberFour())
            .save(provider);
        FourToOneSmithingRecipe.builder()
            .material(ModBlocks.FROST_METAL_BLOCK)
            .input(ModItems.SAPPHIRE_AMULET)
            .input(ModItems.RUBY_AMULET)
            .input(ModItems.TOPAZ_AMULET)
            .input(ModItems.EMERALD_AMULET)
            .result(ModItems.GEM_AMULET)
            .save(provider);
        FourToOneSmithingRecipe.builder()
            .material(ModBlocks.FROST_METAL_BLOCK)
            .input(ModItems.SILENCE_AMULET)
            .input(ModItems.FEATHER_AMULET)
            .input(ModItems.CAT_AMULET)
            .input(ModItems.DOG_AMULET)
            .result(ModItems.NATURE_AMULET)
            .save(provider);
    }

    public static void eight(RegistrumRecipeProvider provider) {
        EightToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_MATTER)
            .input(Items.SHEARS)
            .input(Items.FLINT_AND_STEEL)
            .input(Items.BRUSH)
            .input(Items.SPYGLASS)
            .input(ModItems.MAGNET)
            .input(Items.FISHING_ROD)
            .input(Items.CARROT_ON_A_STICK)
            .input(Items.WARPED_FUNGUS_ON_A_STICK)
            .resultMerge(ModItems.MULTITOOL_ITEM, NormalDataComponent.normalEight())
            .save(provider);
    }
}
