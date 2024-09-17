package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.SuperHeatingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class SuperHeatingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        SuperHeatingRecipe.builder()
                .blockResult(ModBlocks.LAVA_CAULDRON.get())
                .requires(Items.COBBLESTONE, 4)
                .requires(ModItems.LIME_POWDER)
                .save(provider, AnvilCraft.of("super_heating/lava_from_cobblestone"));
        SuperHeatingRecipe.builder()
                .blockResult(ModBlocks.LAVA_CAULDRON.get())
                .requires(ModItemTags.STONE, 4)
                .requires(ModItems.LIME_POWDER)
                .save(provider, AnvilCraft.of("super_heating/lava_from_stone"));

        SuperHeatingRecipe.builder()
                .requires(Items.COAL_BLOCK, 16)
                .result(new ItemStack(Items.DIAMOND_BLOCK))
                .save(provider);
        SuperHeatingRecipe.builder()
                .requires(Items.IRON_INGOT, 3)
                .requires(Items.DIAMOND)
                .requires(Items.AMETHYST_SHARD)
                .requires(ModItemTags.GEMS)
                .result(new ItemStack(ModItems.ROYAL_STEEL_INGOT.asItem()))
                .save(provider);
        SuperHeatingRecipe.builder()
                .requires(ModBlocks.QUARTZ_SAND, 8)
                .requires(ModItems.ROYAL_STEEL_INGOT)
                .result(new ItemStack(ModBlocks.TEMPERING_GLASS, 8))
                .save(provider, AnvilCraft.of("super_heating/tempering_glass_from_royal_steel_ingot"));
        SuperHeatingRecipe.builder()
                .requires(ModBlocks.QUARTZ_SAND, 8)
                .requires(ModItems.EMBER_METAL_INGOT)
                .result(new ItemStack(ModBlocks.TEMPERING_GLASS, 8))
                .save(provider, AnvilCraft.of("super_heating/tempering_glass_from_ember_metal_ingot"));
        SuperHeatingRecipe.builder()
                .requires(ModItems.WOOD_FIBER, 4)
                .result(new ItemStack(Items.CHARCOAL))
                .save(provider);
        SuperHeatingRecipe.builder()
                .requires(ModItems.CRAB_CLAW)
                .result(new ItemStack(ModItems.LIME_POWDER.asItem()))
                .save(provider, AnvilCraft.of("super_heating/lime_powder_from_crab_claw"));
        SuperHeatingRecipe.builder()
                .requires(ModItemTags.DEAD_TUBE)
                .result(new ItemStack(ModItems.LIME_POWDER.asItem()))
                .save(provider, AnvilCraft.of("super_heating/lime_powder_from_dead_tube"));
        SuperHeatingRecipe.builder()
                .requires(Items.NAUTILUS_SHELL)
                .result(new ItemStack(ModItems.LIME_POWDER.asItem()))
                .save(provider, AnvilCraft.of("super_heating/lime_powder_from_nautilus_shell"));
        SuperHeatingRecipe.builder()
                .requires(Items.POINTED_DRIPSTONE)
                .result(new ItemStack(ModItems.LIME_POWDER.asItem()))
                .save(provider, AnvilCraft.of("super_heating/lime_powder_from_pointed_dripstone"));
        SuperHeatingRecipe.builder()
                .requires(Items.DRIPSTONE_BLOCK)
                .result(new ItemStack(ModItems.LIME_POWDER.asItem(), 4))
                .save(provider, AnvilCraft.of("super_heating/lime_powder_from_dripstone_block"));
        SuperHeatingRecipe.builder()
                .requires(Items.CALCITE)
                .result(new ItemStack(ModItems.LIME_POWDER.asItem()))
                .save(provider, AnvilCraft.of("super_heating/lime_powder_from_calcite"));

        SuperHeatingRecipe.builder()
                .requires(Items.RAW_IRON)
                .requires(ModItems.CAPACITOR)
                .result(new ItemStack(ModItems.MAGNET_INGOT.asItem()))
                .result(new ItemStack(ModItems.CAPACITOR_EMPTY.asItem()))
                .save(provider);

        SuperHeatingRecipe.builder()
                .requires(ModBlocks.END_DUST)
                .result(new ItemStack(Items.END_STONE))
                .save(provider);

        SuperHeatingRecipe.builder()
                .blockResult(ModBlocks.MELT_GEM_CAULDRON.get())
                .requires(ModItemTags.GEM_BLOCKS)
                .save(provider);
    }
}
