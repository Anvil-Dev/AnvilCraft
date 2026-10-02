package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

public class JewelCraftingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        JewelCraftingRecipe.builder(provider.getItems())
            .requires(Tags.Items.STORAGE_BLOCKS_GOLD)
            .requires(Items.EMERALD, 2)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .result(Items.TOTEM_OF_UNDYING)
            .save(provider);

        JewelCraftingRecipe.builder(provider.getItems())
            .requires(Items.PHANTOM_MEMBRANE, 8)
            .requires(Tags.Items.FEATHERS, 8)
            .requires(Tags.Items.LEATHERS, 2)
            .requires(Items.BAMBOO, 16)
            .result(Items.ELYTRA)
            .save(provider);

        JewelCraftingRecipe.builder(provider.getItems())
            .requires(Items.POLISHED_TUFF)
            .requires(Items.COPPER_INGOT)
            .result(Items.TRIAL_KEY)
            .hasVanishingCurse(false)
            .save(provider);

        JewelCraftingRecipe.builder(provider.getItems())
            .requires(Items.POLISHED_TUFF, 3)
            .requires(Items.OXIDIZED_COPPER)
            .requires(Items.OMINOUS_BOTTLE)
            .result(Items.OMINOUS_TRIAL_KEY)
            .hasVanishingCurse(false)
            .save(provider);

        JewelCraftingRecipe.builder(provider.getItems())
            .requires(Items.EXPERIENCE_BOTTLE, 16)
            .requires(ModItems.CURSED_GOLD_INGOT, 2)
            .requires(Items.GLASS_BOTTLE)
            .result(Items.OMINOUS_BOTTLE)
            .hasVanishingCurse(false)
            .save(provider);

        JewelCraftingRecipe.builder(provider.getItems())
            .requires(ModBlocks.HEAVY_IRON_BLOCK, 64)
            .requires(ModItemTags.STORAGE_BLOCKS_LEAD, 64)
            .requires(ModBlocks.SPACE_OVERCOMPRESSOR)
            .result(Items.HEAVY_CORE)
            .save(provider);
    }
}
