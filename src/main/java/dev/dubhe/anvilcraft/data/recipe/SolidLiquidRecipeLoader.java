package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.util.FluidStackPredicate;
import dev.dubhe.anvilcraft.util.VanillaConstants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

public class SolidLiquidRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.DIRT, Items.CLAY);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.CRIMSON_FUNGUS, Items.NETHER_WART_BLOCK);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.WARPED_FUNGUS, Items.WARPED_WART_BLOCK);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.BRAIN_CORAL, Items.BRAIN_CORAL_BLOCK);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.BUBBLE_CORAL, Items.BUBBLE_CORAL_BLOCK);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.FIRE_CORAL, Items.FIRE_CORAL_BLOCK);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.HORN_CORAL, Items.HORN_CORAL_BLOCK);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.TUBE_CORAL, Items.TUBE_CORAL_BLOCK);
        SolidLiquidRecipeLoader.solidLiquid(provider, ModItems.SPONGE_GEMMULE, Items.WET_SPONGE, 250);
        SolidLiquidRecipeLoader.solidLiquid(provider, ModItemTags.FLOUR, ModFoodItems.DOUGH);
        SolidLiquidRecipeLoader.solidLiquid(provider, Items.DRIED_KELP, Items.KELP);

        VanillaConstants.CONCRETE_POWDERS.forEach(block -> solidLiquid(provider, block, block.concrete));

        VanillaConstants.WEATHERING_COPPERS.forEach(weatheringCopper -> {
            if (!(weatheringCopper instanceof Block block)) return;
            weatheringCopper.getNext(block.defaultBlockState()).ifPresent(
                state -> solidLiquid(provider, block, state.getBlock())
            );
        });

        SolidLiquidRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .consume(1000)
            .transform(ModBlocks.CEMENT_CAULDRONS.get(Color.GRAY).get(), 1000)
            .requires(ModItems.LIME_POWDER, 4)
            .requires(ModBlocks.CINERITE)
            .save(provider, AnvilCraft.of("solid_liquid/cement_cauldron"));

        SolidLiquidRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(Items.RED_MUSHROOM)
            .result(Blocks.RED_MUSHROOM_BLOCK)
            .result(Blocks.MUSHROOM_STEM, 0.1f)
            .save(provider);
        SolidLiquidRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(Items.BROWN_MUSHROOM)
            .result(Blocks.BROWN_MUSHROOM_BLOCK)
            .result(Blocks.MUSHROOM_STEM, 0.1f)
            .save(provider);

        SolidLiquidRecipe.builder()
            .cauldron(ModFluids.EXP_FLUID.get())
            .consume(1000)
            .result(ModItems.EXP_GEM)
            .save(provider);

        SolidLiquidRecipe.builder()
            .cauldron(ModFluids.EXP_FLUID.get())
            .consume(2000)
            .transform(ModFluids.LIQUID_ENCHANTMENT.get(), 1)
            .requires(Items.LAPIS_LAZULI, 3)
            .save(provider, AnvilCraft.of("solid_liquid/liquid_enchantment"));

        SolidLiquidRecipe.builder()
            .cauldron(NeoForgeMod.MILK.get())
            .consume(1000)
            .result(ModFoodItems.CREAM, 4)
            .save(provider, AnvilCraft.of("solid_liquid/cream_from_milk"));

        SolidLiquidRecipe.builder()
            .cauldron(ModFluids.HONEY.get())
            .consume(1000)
            .result(Items.HONEY_BLOCK)
            .save(provider, AnvilCraft.of("solid_liquid/honey_block"));

        SolidLiquidRecipe.builder()
            .cauldron(ModFluids.HONEY.get())
            .consume(250)
            .requires(ModItemTags.CREAM, 4)
            .requires(Items.SUGAR)
            .result(ModBlocks.HONEY_CREAM_BLOCK)
            .save(provider);

        SolidLiquidRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .consume(1000)
            .transform(ModBlocks.EXP_FLUID_CAULDRON.get(), 1000)
            .requires(ModItems.EXP_GEM)
            .save(provider, AnvilCraft.of("solid_liquid/exp_fluid_cauldron"));

        SolidLiquidRecipeLoader.liquidEnchantment(provider, ModItems.ROYAL_STEEL_INGOT, 1, Enchantments.SILK_TOUCH);
        SolidLiquidRecipeLoader.liquidEnchantment(provider, ModItems.FROST_METAL_INGOT, 1, ModEnchantments.DISINTEGRATION_KEY);
        SolidLiquidRecipeLoader.liquidEnchantment(provider, ModItems.EMBER_METAL_INGOT, 16, ModEnchantments.SMELTING_KEY);
        SolidLiquidRecipeLoader.liquidEnchantment(provider, ModItems.TRANSCENDIUM_INGOT, 128, Enchantments.FORTUNE, Enchantments.LOOTING);
        SolidLiquidRecipeLoader.liquidEnchantment(provider, Items.EMERALD, 1, Enchantments.MENDING);
        SolidLiquidRecipeLoader.liquidEnchantment(provider, ModItems.RUBY, 8, Enchantments.FIRE_PROTECTION);
        SolidLiquidRecipeLoader.liquidEnchantment(provider, ModItems.SAPPHIRE, 2, Enchantments.FROST_WALKER);
        SolidLiquidRecipeLoader.liquidEnchantment(provider, ModItems.TOPAZ, 1, Enchantments.CHANNELING);
        SolidLiquidRecipeLoader.liquidEnchantment(
            provider,
            Items.AMETHYST_BLOCK,
            12,
            ModEnchantments.FELLING_KEY,
            ModEnchantments.HARVEST_KEY,
            ModEnchantments.BEHEADING_KEY
        );
    }

    private static void solidLiquid(RegistrumRecipeProvider provider, ItemLike input, ItemLike result, int consume) {
        SolidLiquidRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(input)
            .result(result)
            .consume(consume)
            .save(provider);
    }

    private static void solidLiquid(RegistrumRecipeProvider provider, ItemLike input, ItemLike result) {
        SolidLiquidRecipeLoader.solidLiquid(provider, input, result, 0);
    }

    @SuppressWarnings("SameParameterValue")
    private static void solidLiquid(RegistrumRecipeProvider provider, TagKey<Item> input, ItemLike result, int consume) {
        SolidLiquidRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(input)
            .result(result)
            .consume(consume)
            .save(provider);
    }

    @SuppressWarnings("SameParameterValue")
    private static void solidLiquid(RegistrumRecipeProvider provider, TagKey<Item> input, ItemLike result) {
        SolidLiquidRecipeLoader.solidLiquid(provider, input, result, 0);
    }

    @SafeVarargs
    @SuppressWarnings("SameParameterValue")
    private static void liquidEnchantment(
        RegistrumRecipeProvider provider,
        ItemLike input,
        int amount,
        ResourceKey<Enchantment>... enchantments
    ) {
        SolidLiquidRecipe.Builder builder = SolidLiquidRecipe.builder()
            .cauldron(
                FluidStackPredicate.builder()
                    .fluid(ModFluids.LIQUID_ENCHANTMENT)
                    .component(b -> b.expectNull(ModComponents.LIQUID_ENCHANTMENT))
                    .build()
            )
            .consume(amount)
            .requires(input);
        int each = amount / enchantments.length;
        StringBuilder idBuilder = new StringBuilder();
        for (ResourceKey<Enchantment> enchantment : enchantments) {
            FluidStack stack = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), each);
            stack.set(ModComponents.LIQUID_ENCHANTMENT, enchantment);
            builder.transform(stack);
            idBuilder.append(enchantment.location().getPath());
            idBuilder.append("_and_");
        }
        String id = idBuilder.substring(0, idBuilder.length() - 5);
        builder.save(provider, AnvilCraft.of("solid_liquid/" + id));
    }
}
