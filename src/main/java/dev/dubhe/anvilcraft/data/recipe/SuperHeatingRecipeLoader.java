package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.SuperHeatingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class SuperHeatingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        SuperHeatingRecipe.builder()
            .blockResult(Blocks.LAVA_CAULDRON)
            .requires(Items.COBBLESTONE, 4)
            .requires(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lava_from_cobblestone"));
        SuperHeatingRecipe.builder()
            .blockResult(Blocks.LAVA_CAULDRON)
            .requires(Tags.Items.STONES, 4)
            .requires(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lava_from_stone"));

        SuperHeatingRecipe.builder()
            .requires(Items.IRON_INGOT, 3)
            .requires(Items.DIAMOND)
            .requires(Items.AMETHYST_SHARD)
            .requires(ModItemTags.GEMS)
            .result(new ItemStack(ModItems.ROYAL_STEEL_INGOT.asItem()))
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(Blocks.IRON_BLOCK, 3)
            .requires(Blocks.DIAMOND_BLOCK)
            .requires(Blocks.AMETHYST_BLOCK, 2)
            .requires(ModItemTags.GEM_BLOCKS)
            .result(ModBlocks.ROYAL_STEEL_BLOCK.asStack())
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND, 8)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .result(new ItemStack(ModBlocks.TEMPERING_GLASS, 8))
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND, 8)
            .requires(ModItems.EMBER_METAL_INGOT)
            .result(new ItemStack(ModBlocks.EMBER_GLASS, 8))
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModItems.WOOD_FIBER, 2)
            .result(new ItemStack(Items.CHARCOAL))
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(Blocks.COAL_BLOCK, 8)
            .result(new ItemStack(Items.DIAMOND))
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModItems.CRAB_CLAW)
            .result(new ItemStack(ModItems.LIME_POWDER.asItem()))
            .save(provider, AnvilCraft.of("super_heating/lime_powder_from_crab_claw"));
        SuperHeatingRecipe.builder()
            .requires(ModItemTags.DEAD_CORALS)
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
