package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class SuperHeatingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        SuperHeatingRecipe.builder()
            .transform(Blocks.LAVA_CAULDRON)
            .requires(Items.COBBLESTONE, 4)
            .requires(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lava_from_cobblestone"));
        SuperHeatingRecipe.builder()
            .transform(Blocks.LAVA_CAULDRON)
            .requires(Tags.Items.STONES, 4)
            .requires(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lava_from_stone"));

        SuperHeatingRecipe.builder()
            .requires(Items.IRON_INGOT, 3)
            .requires(Items.DIAMOND)
            .requires(Items.AMETHYST_SHARD)
            .requires(ModItemTags.GEMS)
            .result(ModItems.ROYAL_STEEL_INGOT)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(Blocks.IRON_BLOCK, 3)
            .requires(Blocks.DIAMOND_BLOCK)
            .requires(Blocks.AMETHYST_BLOCK, 2)
            .requires(ModItemTags.GEM_BLOCKS)
            .result(ModBlocks.ROYAL_STEEL_BLOCK)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND, 8)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .result(ModBlocks.TEMPERING_GLASS, 8)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND, 8)
            .requires(ModItems.EMBER_METAL_INGOT)
            .result(ModBlocks.EMBER_GLASS, 8)
            .save(provider);

        SuperHeatingRecipe.builder()
            .requires(Items.COPPER_INGOT, 2)
            .requires(ModItemTags.ZINC_INGOTS)
            .result(ModItems.BRASS_INGOT, 3)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(Items.COPPER_INGOT, 2)
            .requires(ModItemTags.TIN_INGOTS)
            .result(ModItems.BRONZE_INGOT, 3)
            .save(provider);

        SuperHeatingRecipe.builder()
            .requires(ModItems.WOOD_FIBER, 2)
            .result(Items.CHARCOAL)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(Blocks.COAL_BLOCK, 8)
            .result(Items.DIAMOND)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModItems.CRAB_CLAW)
            .result(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lime_powder_from_crab_claw"));
        SuperHeatingRecipe.builder()
            .requires(ModItemTags.DEAD_CORALS)
            .result(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lime_powder_from_dead_corals"));
        SuperHeatingRecipe.builder()
            .requires(Items.NAUTILUS_SHELL)
            .result(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lime_powder_from_nautilus_shell"));
        SuperHeatingRecipe.builder()
            .requires(Items.POINTED_DRIPSTONE)
            .result(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lime_powder_from_pointed_dripstone"));
        SuperHeatingRecipe.builder()
            .requires(ModItemTags.DEAD_CORAL_BLOCKS)
            .result(ModItems.LIME_POWDER, 4)
            .save(provider, AnvilCraft.of("super_heating/lime_powder_from_dead_coral_blocks"));
        SuperHeatingRecipe.builder()
            .requires(Items.DRIPSTONE_BLOCK)
            .result(ModItems.LIME_POWDER, 4)
            .save(provider, AnvilCraft.of("super_heating/lime_powder_from_dripstone_block"));
        SuperHeatingRecipe.builder()
            .requires(Items.CALCITE)
            .result(ModItems.LIME_POWDER, 4)
            .save(provider, AnvilCraft.of("super_heating/lime_powder_from_calcite"));

        SuperHeatingRecipe.builder()
            .requires(Items.RAW_IRON)
            .requires(ModItems.CAPACITOR)
            .result(ModItems.MAGNET_INGOT)
            .result(ModItems.CAPACITOR_EMPTY)
            .save(provider);

        SuperHeatingRecipe.builder()
            .requires(ModBlocks.END_DUST)
            .result(Items.END_STONE)
            .save(provider);

        SuperHeatingRecipe.builder()
            .transform(ModBlocks.MELT_GEM_CAULDRON.get())
            .requires(ModItemTags.GEM_BLOCKS)
            .save(provider, AnvilCraft.of("super_heating/melt_gem_cauldron"));

        SuperHeatingRecipe.builder()
            .requires(ModItems.TRANSCENDIUM_INGOT)
            .requires(ModItems.MULTIPHASE_MATTER)
            .requires(ModItems.RESONATOR_CORE)
            .requires(ModItems.HEAVY_HALBERD_CORE)
            .requires(ModItems.VOID_MATTER)
            .result(ModItems.MULTIPHASE_TRANSCENDIUM)
            .save(provider);
    }
}
