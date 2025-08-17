package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.api.data.MultiphaseData;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.multiple.EightToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.FourToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.TwoToOneSmithingRecipe;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

public class MultipleToOneSmithingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        MultipleToOneSmithingRecipeLoader.two(provider);
        MultipleToOneSmithingRecipeLoader.four(provider);
        MultipleToOneSmithingRecipeLoader.eight(provider);
    }

    public static void two(RegistrateRecipeProvider provider) {
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModItems.EMBER_METAL_HEAVY_HALBERD)
            .input(ModItems.FROST_METAL_HEAVY_HALBERD)
            .result(ModItems.TRANSCENDENCE_HEAVY_HALBERD, MultiphaseData.two())
            .save(provider);
        TwoToOneSmithingRecipe.builder()
            .material(ModItems.MULTIPHASE_TRANSCENDIUM)
            .input(ModItems.EMBER_METAL_RESONATOR)
            .input(ModItems.FROST_METAL_RESONATOR)
            .result(ModItems.TRANSCENDENCE_RESONATOR, MultiphaseData.two())
            .save(provider);
    }

    public static void four(RegistrateRecipeProvider provider) {
        FourToOneSmithingRecipe.builder()
            .material(ModItems.HEAVY_HALBERD_CORE)
            .input(ModItems.FROST_METAL_SWORD)
            .input(ModItems.FROST_METAL_AXE)
            .input(Items.TRIDENT)
            .input(Tags.Items.TOOLS_MACE)
            .result(ModItems.FROST_METAL_HEAVY_HALBERD, MultiphaseData.four())
            .save(provider);
        FourToOneSmithingRecipe.builder()
            .material(ModItems.HEAVY_HALBERD_CORE)
            .input(ModItems.EMBER_METAL_SWORD)
            .input(ModItems.EMBER_METAL_AXE)
            .input(Items.TRIDENT)
            .input(Tags.Items.TOOLS_MACE)
            .result(ModItems.EMBER_METAL_HEAVY_HALBERD, MultiphaseData.four())
            .save(provider);
        FourToOneSmithingRecipe.builder()
            .material(ModItems.RESONATOR_CORE)
            .input(ModItems.FROST_METAL_AXE)
            .input(ModItems.FROST_METAL_SHOVEL)
            .input(ModItems.FROST_METAL_HOE)
            .input(ModItems.FROST_METAL_PICKAXE)
            .result(ModItems.FROST_METAL_RESONATOR, MultiphaseData.four())
            .save(provider);
        FourToOneSmithingRecipe.builder()
            .material(ModItems.RESONATOR_CORE)
            .input(ModItems.EMBER_METAL_AXE)
            .input(ModItems.EMBER_METAL_SHOVEL)
            .input(ModItems.EMBER_METAL_HOE)
            .input(ModItems.EMBER_METAL_PICKAXE)
            .result(ModItems.EMBER_METAL_RESONATOR, MultiphaseData.four())
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

    public static void eight(RegistrateRecipeProvider provider) {
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
            .result(ModItems.MULTITOOL_ITEM)
            .save(provider);
    }
}
