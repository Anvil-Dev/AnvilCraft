package dev.dubhe.anvilcraft.integration.twilight;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.anvilcraft.lib.integration.Integration;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import twilightforest.TwilightForestMod;
import twilightforest.init.TFBlocks;
import twilightforest.init.TFItems;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@Integration(value = "twilightforest")
public class TwilightIntegration {
    public void apply() {
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, TwilightIntegration::init);
    }

    public static void init(RegistrateRecipeProvider provider) {
        JewelCraftingRecipe.builder()
            .requires(ModItems.EMBER_METAL_INGOT, 2)
            .requires(Items.BLAZE_POWDER, 2)
            .requires(TFItems.FIERY_INGOT, 2)
            .result(TFItems.LAMP_OF_CINDERS.toStack())
            .save(
                provider.withConditions(new ModLoadedCondition(TwilightForestMod.ID)),
                AnvilCraft.of("jewel_crafting/twilight_forest_lamp_of_cinders")
            );

        JewelCraftingRecipe.builder()
            .requires(ModItems.MAGNET_INGOT, 4)
            .requires(Ingredient.of(ItemTags.IRON_ORES), 4)
            .requires(Ingredient.of(ItemTags.GOLD_ORES), 4)
            .requires(Ingredient.of(ItemTags.COPPER_ORES), 4)
            .result(TFItems.ORE_MAGNET.toStack())
            .save(
                provider.withConditions(new ModLoadedCondition(TwilightForestMod.ID)),
                AnvilCraft.of("jewel_crafting/twilight_forest_ore_magnet")
            );

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, TFBlocks.KNIGHTMETAL_BLOCK)
            .pattern("CCC")
            .pattern("CHC")
            .pattern("CCC")
            .define('C', Items.CACTUS)
            .define('H', ModBlocks.HEAVY_IRON_BLOCK)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(
                provider.withConditions(new ModLoadedCondition(TwilightForestMod.ID)),
                AnvilCraft.of("twilight_forest_knightmetal_block")
            );

        ItemInjectRecipe.builder()
            .requires(ModItems.EMBER_METAL_INGOT)
            .inputBlock(Blocks.IRON_BLOCK)
            .resultBlock(TFBlocks.FIERY_BLOCK)
            .save(
                provider.withConditions(new ModLoadedCondition(TwilightForestMod.ID)),
                AnvilCraft.of("item_inject/twilight_forest_fiery_block")
            );

        MobTransformWithItemRecipe.from(EntityType.ZOMBIE, Items.STONE_PICKAXE, EntityType.GIANT, TFItems.GIANT_PICKAXE.toStack())
            .setItemChancePercentagePerItem(50)
            .save(
                provider.withConditions(new ModLoadedCondition(TwilightForestMod.ID)),
                AnvilCraft.of("mob_transform_with_item/zombie_to_giant_pickaxe")
            );

        MobTransformWithItemRecipe.from(EntityType.ZOMBIE, Items.STONE_SWORD, EntityType.GIANT, TFItems.GIANT_SWORD.toStack())
            .setItemChancePercentagePerItem(50)
            .save(
                provider.withConditions(new ModLoadedCondition(TwilightForestMod.ID)),
                AnvilCraft.of("mob_transform_with_item/zombie_to_giant_sword")
            );
    }
}
