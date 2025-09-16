package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import twilightforest.TwilightForestMod;
import twilightforest.init.TFItems;

public class JewelCraftingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        JewelCraftingRecipe.builder()
            .requires(Items.EXPERIENCE_BOTTLE, 16)
            .requires(Items.GOLD_BLOCK, 8)
            .requires(Items.GOLDEN_APPLE)
            .result(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE))
            .save(provider);

        JewelCraftingRecipe.builder()
            .requires(Tags.Items.STORAGE_BLOCKS_GOLD)
            .requires(Items.EMERALD, 2)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .result(new ItemStack(Items.TOTEM_OF_UNDYING))
            .save(provider);

        JewelCraftingRecipe.builder()
            .requires(Items.PHANTOM_MEMBRANE, 8)
            .requires(Tags.Items.FEATHERS, 8)
            .requires(Tags.Items.LEATHERS, 2)
            .requires(Items.BAMBOO, 16)
            .result(new ItemStack(Items.ELYTRA))
            .save(provider);

        JewelCraftingRecipe.builder()
            .requires(Items.POLISHED_TUFF)
            .requires(Items.COPPER_INGOT)
            .result(new ItemStack(Items.TRIAL_KEY))
            .save(provider);

        JewelCraftingRecipe.builder()
            .requires(Items.POLISHED_TUFF, 3)
            .requires(Items.OXIDIZED_COPPER)
            .requires(Items.OMINOUS_BOTTLE)
            .result(new ItemStack(Items.OMINOUS_TRIAL_KEY))
            .save(provider);

        JewelCraftingRecipe.builder()
            .requires(Items.EXPERIENCE_BOTTLE, 16)
            .requires(ModItems.CURSED_GOLD_INGOT, 2)
            .requires(Items.GLASS_BOTTLE)
            .result(Items.OMINOUS_BOTTLE.getDefaultInstance())
            .save(provider);

        JewelCraftingRecipe.builder()
            .requires(ModBlocks.HEAVY_IRON_BLOCK, 64)
            .requires(ModItemTags.STORAGE_BLOCKS_LEAD, 64)
            .requires(ModBlocks.SPACE_OVERCOMPRESSOR)
            .result(new ItemStack(Items.HEAVY_CORE))
            .save(provider);

        // Twilight Forest Integration
        JewelCraftingRecipe.builder()
            .requires(ModItems.EMBER_METAL_INGOT, 2)
            .requires(Items.BLAZE_POWDER, 2)
            .requires(TFItems.FIERY_INGOT, 2)
            .result(TFItems.LAMP_OF_CINDERS.toStack())
            .save(provider.withConditions(new ModLoadedCondition(TwilightForestMod.ID)),
                AnvilCraft.of("jewel_crafting/twilight_forest_lamp_of_cinders"));

        JewelCraftingRecipe.builder()
            .requires(ModItems.MAGNET_INGOT, 4)
            .requires(Ingredient.of(ItemTags.IRON_ORES), 4)
            .requires(Ingredient.of(ItemTags.GOLD_ORES), 4)
            .requires(Ingredient.of(ItemTags.COPPER_ORES), 4)
            .result(TFItems.ORE_MAGNET.toStack())
            .save(provider.withConditions(new ModLoadedCondition(TwilightForestMod.ID)),
                AnvilCraft.of("jewel_crafting/twilight_forest_ore_magnet"));
    }
}
