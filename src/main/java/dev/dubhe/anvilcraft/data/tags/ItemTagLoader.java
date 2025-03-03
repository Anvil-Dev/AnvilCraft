package dev.dubhe.anvilcraft.data.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
            .add(ModItems.AMETHYST_PICKAXE.getKey())
            .add(findResourceKey(Items.GOLDEN_PICKAXE))
            .add(findResourceKey(Items.IRON_PICKAXE))
            .add(findResourceKey(Items.DIAMOND_PICKAXE));
        provider.addTag(ModItemTags.ROYAL_STEEL_AXE_BASE)
            .add(ModItems.AMETHYST_AXE.getKey())
            .add(findResourceKey(Items.GOLDEN_AXE))
            .add(findResourceKey(Items.IRON_AXE))
            .add(findResourceKey(Items.DIAMOND_AXE));
        provider.addTag(ModItemTags.ROYAL_STEEL_HOE_BASE)
            .add(ModItems.AMETHYST_HOE.getKey())
            .add(findResourceKey(Items.GOLDEN_HOE))
            .add(findResourceKey(Items.IRON_HOE))
            .add(findResourceKey(Items.DIAMOND_HOE));
        provider.addTag(ModItemTags.ROYAL_STEEL_SWORD_BASE)
            .add(ModItems.AMETHYST_SWORD.getKey())
            .add(findResourceKey(Items.GOLDEN_SWORD))
            .add(findResourceKey(Items.IRON_SWORD))
            .add(findResourceKey(Items.DIAMOND_SWORD));
        provider.addTag(ModItemTags.ROYAL_STEEL_SHOVEL_BASE)
            .add(ModItems.AMETHYST_SHOVEL.getKey())
            .add(findResourceKey(Items.GOLDEN_SHOVEL))
            .add(findResourceKey(Items.IRON_SHOVEL))
            .add(findResourceKey(Items.DIAMOND_SHOVEL));

        provider.addTag(ModItemTags.EMBER_METAL_PICKAXE_BASE)
            .add(ModItems.ROYAL_STEEL_PICKAXE.getKey())
            .add(findResourceKey(Items.NETHERITE_PICKAXE));
        provider.addTag(ModItemTags.EMBER_METAL_AXE_BASE)
            .add(ModItems.ROYAL_STEEL_AXE.getKey())
            .add(findResourceKey(Items.NETHERITE_AXE));
        provider.addTag(ModItemTags.EMBER_METAL_HOE_BASE)
            .add(ModItems.ROYAL_STEEL_HOE.getKey())
            .add(findResourceKey(Items.NETHERITE_HOE));
        provider.addTag(ModItemTags.EMBER_METAL_SWORD_BASE)
            .add(ModItems.ROYAL_STEEL_SWORD.getKey())
            .add(findResourceKey(Items.NETHERITE_SWORD));
        provider.addTag(ModItemTags.EMBER_METAL_SHOVEL_BASE)
            .add(ModItems.ROYAL_STEEL_SHOVEL.getKey())
            .add(findResourceKey(Items.NETHERITE_SHOVEL));

        provider.addTag(ModItemTags.GEMS)
            .add(findResourceKey(Items.EMERALD))
            .add(ModItems.RUBY.getKey())
            .add(ModItems.SAPPHIRE.getKey())
            .add(ModItems.TOPAZ.getKey());
        provider.addTag(ModItemTags.GEM_BLOCKS)
            .add(findResourceKey(Items.EMERALD_BLOCK))
            .add(findResourceKey(ModBlocks.RUBY_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.SAPPHIRE_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.TOPAZ_BLOCK.asItem()));

        provider.addTag(ModItemTags.DEAD_CORALS)
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
            .add(ModItems.ANVIL_HAMMER.getKey())
            .add(ModItems.ROYAL_ANVIL_HAMMER.getKey())
            .add(ModItems.EMBER_ANVIL_HAMMER.getKey());
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
            .add(ModItems.EARTH_CORE_SHARD.getKey())
            .add(findResourceKey(ModBlocks.EMBER_ANVIL.asItem()))
            .add(findResourceKey(ModBlocks.EMBER_SMITHING_TABLE.asItem()))
            .add(findResourceKey(ModBlocks.EMBER_GRINDSTONE.asItem()))
            .add(findResourceKey(ModBlocks.EMBER_METAL_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.EMBER_GLASS.asItem()))
            .add(findResourceKey(ModBlocks.CUT_EMBER_METAL_STAIRS.asItem()))
            .add(findResourceKey(ModBlocks.CUT_EMBER_METAL_SLAB.asItem()))
            .add(findResourceKey(ModBlocks.CUT_EMBER_METAL_PILLAR.asItem()))
            .add(findResourceKey(ModBlocks.CUT_EMBER_METAL_BLOCK.asItem()))
            .add(ModItems.EMBER_ANVIL_HAMMER.getKey())
            .add(ModItems.EMBER_METAL_AXE.getKey())
            .add(ModItems.EMBER_METAL_HOE.getKey())
            .add(ModItems.EMBER_METAL_INGOT.getKey())
            .add(ModItems.EMBER_METAL_NUGGET.getKey())
            .add(ModItems.EMBER_METAL_SHOVEL.getKey())
            .add(ModItems.EMBER_METAL_SWORD.getKey())
            .add(ModItems.EMBER_METAL_PICKAXE.getKey())
            .add(ModItems.NEUTRONIUM_INGOT.getKey())
            .add(ModItems.STABLE_NEUTRONIUM_INGOT.getKey())
            .add(ModItems.CHARGED_NEUTRONIUM_INGOT.getKey());
    }

    private static ResourceKey<Item> findResourceKey(Item item) {
        return ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item));
    }
}
