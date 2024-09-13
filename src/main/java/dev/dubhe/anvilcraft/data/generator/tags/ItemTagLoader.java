package dev.dubhe.anvilcraft.data.generator.tags;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import org.jetbrains.annotations.NotNull;

public class ItemTagLoader {
    /**
     * 物品标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateTagsProvider<Item> provider) {
        provider.addTag(ModItemTags.PLATES)
                .add(findResourceKey(Items.HEAVY_WEIGHTED_PRESSURE_PLATE))
                .add(findResourceKey(Items.LIGHT_WEIGHTED_PRESSURE_PLATE));
        provider.addTag(ModItemTags.IRON_PLATES).add(findResourceKey(Items.HEAVY_WEIGHTED_PRESSURE_PLATE));
        provider.addTag(ModItemTags.GOLD_PLATES).add(findResourceKey(Items.LIGHT_WEIGHTED_PRESSURE_PLATE));
        provider.addTag(ModItemTags.ROYAL_STEEL_PICKAXE_BASE)
                .add(findResourceKey(ModItems.AMETHYST_PICKAXE.get()))
                .add(findResourceKey(Items.GOLDEN_PICKAXE))
                .add(findResourceKey(Items.IRON_PICKAXE))
                .add(findResourceKey(Items.DIAMOND_PICKAXE));
        provider.addTag(ModItemTags.ROYAL_STEEL_AXE_BASE)
                .add(findResourceKey(ModItems.AMETHYST_AXE.get()))
                .add(findResourceKey(Items.GOLDEN_AXE))
                .add(findResourceKey(Items.IRON_AXE))
                .add(findResourceKey(Items.DIAMOND_AXE));
        provider.addTag(ModItemTags.ROYAL_STEEL_HOE_BASE)
                .add(findResourceKey(ModItems.AMETHYST_HOE.get()))
                .add(findResourceKey(Items.GOLDEN_HOE))
                .add(findResourceKey(Items.IRON_HOE))
                .add(findResourceKey(Items.DIAMOND_HOE));
        provider.addTag(ModItemTags.ROYAL_STEEL_SWORD_BASE)
                .add(findResourceKey(ModItems.AMETHYST_SWORD.get()))
                .add(findResourceKey(Items.GOLDEN_SWORD))
                .add(findResourceKey(Items.IRON_SWORD))
                .add(findResourceKey(Items.DIAMOND_SWORD));
        provider.addTag(ModItemTags.ROYAL_STEEL_SHOVEL_BASE)
                .add(findResourceKey(ModItems.AMETHYST_SHOVEL.get()))
                .add(findResourceKey(Items.GOLDEN_SHOVEL))
                .add(findResourceKey(Items.IRON_SHOVEL))
                .add(findResourceKey(Items.DIAMOND_SHOVEL));
        provider.addTag(ModItemTags.GEMS)
                .add(findResourceKey(Items.EMERALD))
                .add(findResourceKey(ModItems.RUBY.get()))
                .add(findResourceKey(ModItems.SAPPHIRE.get()))
                .add(findResourceKey(ModItems.TOPAZ.get()));
        provider.addTag(ModItemTags.GEMS)
                .add(findResourceKey(Items.EMERALD))
                .add(findResourceKey(ModItems.RUBY.get()))
                .add(findResourceKey(ModItems.SAPPHIRE.get()))
                .add(findResourceKey(ModItems.TOPAZ.get()));
        provider.addTag(ModItemTags.GEM_BLOCKS)
                .add(findResourceKey(Items.EMERALD_BLOCK))
                .add(findResourceKey(ModBlocks.RUBY_BLOCK.asItem()))
                .add(findResourceKey(ModBlocks.SAPPHIRE_BLOCK.asItem()))
                .add(findResourceKey(ModBlocks.TOPAZ_BLOCK.asItem()));
        provider.addTag(ModItemTags.DEAD_TUBE)
                .add(findResourceKey(Items.DEAD_BRAIN_CORAL))
                .add(findResourceKey(Items.DEAD_BUBBLE_CORAL))
                .add(findResourceKey(Items.DEAD_FIRE_CORAL))
                .add(findResourceKey(Items.DEAD_HORN_CORAL))
                .add(findResourceKey(Items.DEAD_TUBE_CORAL))
                .add(findResourceKey(Items.DEAD_TUBE_CORAL_FAN))
                .add(findResourceKey(Items.DEAD_BRAIN_CORAL_FAN))
                .add(findResourceKey(Items.DEAD_BUBBLE_CORAL_FAN))
                .add(findResourceKey(Items.DEAD_FIRE_CORAL_FAN))
                .add(findResourceKey(Items.DEAD_HORN_CORAL_FAN));
        provider.addTag(ModItemTags.SEEDS_PACK_CONTENT)
                .addOptionalTag(ModItemTags.SEEDS)
                .addOptionalTag(ModItemTags.BERRIES)
                .addOptionalTag(ModItemTags.VEGETABLES);
        provider.addTag(ModItemTags.VEGETABLES)
                .add(findResourceKey(Items.POTATO))
                .add(findResourceKey(Items.CARROT));
        provider.addTag(ModItemTags.BERRIES)
                .add(findResourceKey(Items.SWEET_BERRIES))
                .add(findResourceKey(Items.GLOW_BERRIES));
        provider.addTag(ModItemTags.WRENCH)
                .add(findResourceKey(ModItems.ANVIL_HAMMER.get()))
                .add(findResourceKey(ModItems.ROYAL_ANVIL_HAMMER.get()));
        provider.addTag(ModItemTags.FIRE_STARTER)
                .add(findResourceKey(Items.TORCH))
                .add(findResourceKey(Items.SOUL_TORCH))
                .add(findResourceKey(Items.CAMPFIRE))
                .add(findResourceKey(Items.SOUL_CAMPFIRE))
                .add(findResourceKey(Items.BLAZE_POWDER));
        provider.addTag(ModItemTags.UNBROKEN_FIRE_STARTER)
                .add(findResourceKey(ModBlocks.REDHOT_NETHERITE.asItem()))
                .add(findResourceKey(ModBlocks.GLOWING_NETHERITE.asItem()))
                .add(findResourceKey(ModBlocks.HEATED_NETHERITE.asItem()))
                .add(findResourceKey(ModBlocks.INCANDESCENT_NETHERITE.asItem()))
                .add(findResourceKey(ModBlocks.REDHOT_TUNGSTEN.asItem()))
                .add(findResourceKey(ModBlocks.GLOWING_TUNGSTEN.asItem()))
                .add(findResourceKey(ModBlocks.HEATED_TUNGSTEN.asItem()))
                .add(findResourceKey(ModBlocks.INCANDESCENT_TUNGSTEN.asItem()));
        provider.addTag(ModItemTags.NETHERITE_BLOCK)
                .add(findResourceKey(ModBlocks.REDHOT_NETHERITE.asItem()))
                .add(findResourceKey(ModBlocks.GLOWING_NETHERITE.asItem()))
                .add(findResourceKey(ModBlocks.HEATED_NETHERITE.asItem()))
                .add(findResourceKey(ModBlocks.INCANDESCENT_NETHERITE.asItem()));
        provider.addTag(ModItemTags.EXPLOSION_PROOF)
                .add(findResourceKey(ModBlocks.EARTH_CORE_SHARD_BLOCK.asItem()))
                .add(findResourceKey(ModBlocks.EARTH_CORE_SHARD_ORE.asItem()))
                .add(findResourceKey(ModItems.EARTH_CORE_SHARD.get()));
    }

    private static ResourceKey<Item> findResourceKey(Item item) {
        return ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item));
    }
}
