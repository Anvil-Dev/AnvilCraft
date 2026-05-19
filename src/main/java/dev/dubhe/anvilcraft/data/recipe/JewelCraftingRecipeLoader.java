package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

public class JewelCraftingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        HolderGetter<Item> items = provider.getItems();
        JewelCraftingRecipe.builder(items)
            .requires(Items.EXPERIENCE_BOTTLE, 16)
            .requires(Items.GOLD_BLOCK, 8)
            .requires(Items.GOLDEN_APPLE)
            .source(Items.ENCHANTED_GOLDEN_APPLE)
            .save(provider, "enchanted_golden_apple");

        JewelCraftingRecipe.builder(items)
            .requires(Tags.Items.STORAGE_BLOCKS_GOLD)
            .requires(Items.EMERALD, 2)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .source(Items.TOTEM_OF_UNDYING)
            .save(provider, "totem_of_undying");

        JewelCraftingRecipe.builder(items)
            .requires(Items.PHANTOM_MEMBRANE, 8)
            .requires(Tags.Items.FEATHERS, 8)
            .requires(Tags.Items.LEATHERS, 2)
            .requires(Items.BAMBOO, 16)
            .source(Items.ELYTRA)
            .save(provider, "elytra");

        JewelCraftingRecipe.builder(items)
            .requires(Items.POLISHED_TUFF)
            .requires(Items.COPPER_INGOT)
            .source(Items.TRIAL_KEY)
            .save(provider, "trial_key");

        JewelCraftingRecipe.builder(items)
            .requires(Items.POLISHED_TUFF, 3)
            .requires(Items.OXIDIZED_COPPER)
            .requires(Items.OMINOUS_BOTTLE)
            .source(Items.OMINOUS_TRIAL_KEY)
            .save(provider, "ominous_trial_key");

        JewelCraftingRecipe.builder(items)
            .requires(Items.EXPERIENCE_BOTTLE, 16)
            .requires(ModItems.CURSED_GOLD_INGOT, 2)
            .requires(Items.GLASS_BOTTLE)
            .source(Items.OMINOUS_BOTTLE)
            .save(provider, "ominous_bottle");

        JewelCraftingRecipe.builder(items)
            .requires(ModBlocks.HEAVY_IRON_BLOCK, 64)
            .requires(ModItemTags.STORAGE_BLOCKS_LEAD, 64)
            .requires(ModBlocks.SPACE_OVERCOMPRESSOR)
            .source(Items.HEAVY_CORE)
            .save(provider, "heavy_core");

        JewelCraftingRecipe.builder(items)
            .requires(Items.PAPER)
            .requires(Items.INK_SAC)
            .source(ItemIngredientPredicate.Builder.item().hasComponents(
                DataComponentMatchers.Builder.components()
                    .any(DataComponents.PROVIDES_BANNER_PATTERNS)
                    .build()
            ))
            .save(provider, "banner_patterns");

        JewelCraftingRecipe.builder(items)
            .requires(ModItems.HARDEND_RESIN, 4)
            .requires(Items.PAPER)
            .source(ItemIngredientPredicate.Builder.item().hasComponents(
                DataComponentMatchers.Builder.components()
                    .any(DataComponents.JUKEBOX_PLAYABLE)
                    .build()
            ))
            .save(provider, "music_discs");

        // TODO: 等陶片数据驱动
        // JewelCraftingRecipe.builder(items)
        //     .requires(Items.BRICK, 2)
        //     .result(ItemIngredientPredicate.Builder.item().hasComponents(
        //         DataComponentMatchers.Builder.components()
        //             .any(DataComponents.PROVIDES_BANNER_PATTERNS)
        //             .build()
        //     ))
        //     .save(provider, "pottery_sherds");

        JewelCraftingRecipe.builder(items)
            .requires(ModItems.EARTH_CORE_SHARD)
            .requires(Items.DIAMOND)
            .source(ItemIngredientPredicate.Builder.item().hasComponents(
                DataComponentMatchers.Builder.components()
                    .any(DataComponents.PROVIDES_TRIM_MATERIAL)
                    .build()
            ))
            .save(provider, "trim_templates");
    }
}
