package dev.dubhe.anvilcraft.data.generator.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.anvilcraft.lib.data.provider.RegistratorTagsProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class ItemTagLoader {
    /**
     * 物品标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistratorTagsProvider<Item> provider) {
        provider.create(ModItemTags.PLATES).setReplace(false)
            .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
            .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        provider.create(ModItemTags.IRON_PLATES).setReplace(false)
            .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
        provider.create(ModItemTags.GOLD_PLATES).setReplace(false)
            .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        provider.create(ModItemTags.PLATES_FORGE).setReplace(false)
            .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
            .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        provider.create(ModItemTags.IRON_PLATES_FORGE).setReplace(false)
            .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
        provider.create(ModItemTags.GOLD_PLATES_FORGE).setReplace(false)
            .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        provider.create(ModItemTags.ROYAL_STEEL_PICKAXE_BASE).setReplace(false)
            .add(ModItems.AMETHYST_PICKAXE.get())
            .add(Items.GOLDEN_PICKAXE)
            .add(Items.IRON_PICKAXE)
            .add(Items.DIAMOND_PICKAXE);
        provider.create(ModItemTags.ROYAL_STEEL_AXE_BASE).setReplace(false)
            .add(ModItems.AMETHYST_AXE.get())
            .add(Items.GOLDEN_AXE)
            .add(Items.IRON_AXE)
            .add(Items.DIAMOND_AXE);
        provider.create(ModItemTags.ROYAL_STEEL_HOE_BASE).setReplace(false)
            .add(ModItems.AMETHYST_HOE.get())
            .add(Items.GOLDEN_HOE)
            .add(Items.IRON_HOE)
            .add(Items.DIAMOND_HOE);
        provider.create(ModItemTags.ROYAL_STEEL_SWORD_BASE).setReplace(false)
            .add(ModItems.AMETHYST_SWORD.get())
            .add(Items.GOLDEN_SWORD)
            .add(Items.IRON_SWORD)
            .add(Items.DIAMOND_SWORD);
        provider.create(ModItemTags.ROYAL_STEEL_SHOVEL_BASE).setReplace(false)
            .add(ModItems.AMETHYST_SHOVEL.get())
            .add(Items.GOLDEN_SHOVEL)
            .add(Items.IRON_SHOVEL)
            .add(Items.DIAMOND_SHOVEL);
        provider.create(ModItemTags.GEMS).setReplace(false)
            .add(Items.EMERALD)
            .add(ModItems.RUBY.get())
            .add(ModItems.SAPPHIRE.get())
            .add(ModItems.TOPAZ.get());
        provider.create(ModItemTags.GEMS).setReplace(false)
            .add(Items.EMERALD)
            .add(ModItems.RUBY.get())
            .add(ModItems.SAPPHIRE.get())
            .add(ModItems.TOPAZ.get());
        provider.create(ModItemTags.GEM_BLOCKS).setReplace(false)
            .add(Items.EMERALD_BLOCK)
            .add(ModBlocks.RUBY_BLOCK.asItem())
            .add(ModBlocks.SAPPHIRE_BLOCK.asItem())
            .add(ModBlocks.TOPAZ_BLOCK.asItem());
        provider.create(ModItemTags.DEAD_TUBE).setReplace(false)
            .add(Items.DEAD_BRAIN_CORAL)
            .add(Items.DEAD_BUBBLE_CORAL)
            .add(Items.DEAD_FIRE_CORAL)
            .add(Items.DEAD_HORN_CORAL)
            .add(Items.DEAD_TUBE_CORAL)
            .add(Items.DEAD_TUBE_CORAL_FAN)
            .add(Items.DEAD_BRAIN_CORAL_FAN)
            .add(Items.DEAD_BUBBLE_CORAL_FAN)
            .add(Items.DEAD_FIRE_CORAL_FAN)
            .add(Items.DEAD_HORN_CORAL_FAN);
        provider.create(ModItemTags.SEEDS_PACK_CONTENT).setReplace(false)
            .addOptionalTag(ModItemTags.SEEDS)
            .addOptionalTag(ModItemTags.BERRIES)
            .addOptionalTag(ModItemTags.VEGETABLES)
            .addOptionalTag(ModItemTags.SEEDS_FORGE)
            .addOptionalTag(ModItemTags.BERRIES_FORGE)
            .addOptionalTag(ModItemTags.VEGETABLES_FORGE);
        provider.create(ModItemTags.VEGETABLES).setReplace(false)
            .add(Items.POTATO)
            .add(Items.CARROT);
        provider.create(ModItemTags.BERRIES).setReplace(false)
            .add(Items.SWEET_BERRIES)
            .add(Items.GLOW_BERRIES);
        provider.create(ModItemTags.WRENCHES).setReplace(false)
            .add(ModItems.ANVIL_HAMMER.get())
            .add(ModItems.ROYAL_ANVIL_HAMMER.get());
        provider.create(ModItemTags.WRENCH_FORGE).setReplace(false)
                .add(ModItems.ANVIL_HAMMER.get())
                .add(ModItems.ROYAL_ANVIL_HAMMER.get());
        provider.create(ModItemTags.FIRE_STARTER).setReplace(false)
                .add(Items.TORCH)
                .add(Items.SOUL_TORCH)
                .add(Items.CAMPFIRE)
                .add(Items.SOUL_CAMPFIRE)
                .add(Items.BLAZE_POWDER);
        provider.create(ModItemTags.UNBROKEN_FIRE_STARTER).setReplace(false)
                .add(ModBlocks.REDHOT_NETHERITE.asItem())
                .add(ModBlocks.GLOWING_NETHERITE.asItem())
                .add(ModBlocks.HEATED_NETHERITE.asItem())
                .add(ModBlocks.INCANDESCENT_NETHERITE.asItem())
                .add(ModBlocks.REDHOT_TUNGSTEN.asItem())
                .add(ModBlocks.GLOWING_TUNGSTEN.asItem())
                .add(ModBlocks.HEATED_TUNGSTEN.asItem())
                .add(ModBlocks.INCANDESCENT_TUNGSTEN.asItem());
        provider.create(ModItemTags.NETHERITE_BLOCK).setReplace(false)
                .add(ModBlocks.REDHOT_NETHERITE.asItem())
                .add(ModBlocks.GLOWING_NETHERITE.asItem())
                .add(ModBlocks.HEATED_NETHERITE.asItem())
                .add(ModBlocks.INCANDESCENT_NETHERITE.asItem());
        provider.create(ModItemTags.EXPLOSION_PROOF).setReplace(false)
                .add(ModBlocks.EARTH_CORE_SHARD_BLOCK.asItem())
                .add(ModBlocks.EARTH_CORE_SHARD_ORE.asItem())
                .add(ModItems.EARTH_CORE_SHARD.get());
    }
}
