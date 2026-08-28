package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

public class ItemTagLoader {
    /**
     * 物品标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistrumTagsProvider<Item> provider) {
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

        provider.addTag(ModItemTags.FROST_METAL_PICKAXE_BASE)
            .add(ModItems.ROYAL_STEEL_PICKAXE.getKey());
        provider.addTag(ModItemTags.FROST_METAL_AXE_BASE)
            .add(ModItems.ROYAL_STEEL_AXE.getKey());
        provider.addTag(ModItemTags.FROST_METAL_HOE_BASE)
            .add(ModItems.ROYAL_STEEL_HOE.getKey());
        provider.addTag(ModItemTags.FROST_METAL_SWORD_BASE)
            .add(ModItems.ROYAL_STEEL_SWORD.getKey());
        provider.addTag(ModItemTags.FROST_METAL_SHOVEL_BASE)
            .add(ModItems.ROYAL_STEEL_SHOVEL.getKey());

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
        provider.addTag(ModItemTags.DEAD_CORAL_BLOCKS)
            .add(findResourceKey(Items.DEAD_BRAIN_CORAL_BLOCK))
            .add(findResourceKey(Items.DEAD_BUBBLE_CORAL_BLOCK))
            .add(findResourceKey(Items.DEAD_FIRE_CORAL_BLOCK))
            .add(findResourceKey(Items.DEAD_HORN_CORAL_BLOCK))
            .add(findResourceKey(Items.DEAD_TUBE_CORAL_BLOCK));
        provider.addTag(ModItemTags.SEEDS_PACK_CONTENT)
            .addOptionalTag(Tags.Items.SEEDS)
            .addOptionalTag(Tags.Items.FOODS_BERRY)
            .addOptionalTag(Tags.Items.FOODS_VEGETABLE);
        provider.addTag(Tags.Items.TOOLS_WRENCH)
            .addTag(ModItemTags.ANVIL_HAMMER);
        provider.addTag(ModItemTags.FIRE_STARTER)
            .add(findResourceKey(Items.TORCH))
            .add(findResourceKey(Items.SOUL_TORCH))
            .add(findResourceKey(Items.CAMPFIRE))
            .add(findResourceKey(Items.SOUL_CAMPFIRE))
            .add(findResourceKey(Items.BLAZE_POWDER));
        provider.addTag(ModItemTags.UNBROKEN_FIRE_STARTER)
            .add(findResourceKey(ModBlocks.REDHOT_NETHERITE_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.GLOWING_NETHERITE_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.HEATED_NETHERITE_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.INCANDESCENT_NETHERITE_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.REDHOT_TUNGSTEN_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.GLOWING_TUNGSTEN_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.HEATED_TUNGSTEN_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.INCANDESCENT_TUNGSTEN_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.asItem()));
        provider.addTag(ModItemTags.NETHERITE_BLOCK)
            .add(findResourceKey(ModBlocks.REDHOT_NETHERITE_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.GLOWING_NETHERITE_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.HEATED_NETHERITE_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.INCANDESCENT_NETHERITE_BLOCK.asItem()));
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
        provider.addTag(ModItemTags.TEMPLATES)
            .addTag(ItemTags.TRIM_TEMPLATES)
            .addTag(ModItemTags.MULTIPLE_TO_ONE_SMITHING_TEMPLATES)
            .add(findResourceKey(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        provider.addTag(ModItemTags.UNCHARGED_NEUTRONIUM_INGOTS)
            .add(ModItems.NEUTRONIUM_INGOT.getKey())
            .add(ModItems.STABLE_NEUTRONIUM_INGOT.getKey());
        provider.addTag(ModItemTags.HEATABLE_BLOCKS)
            .add(findResourceKey(Items.NETHERITE_BLOCK));
        provider.addTag(ModItemTags.DISALLOW_HAND_INSERT_INTO_TANK)
            .addTag(ModItemTags.ANVIL_HAMMER);

        provider.addTag(ModItemTags.AMULET)
            .addOptional(AnvilCraft.of("cogwheel_amulet"));

        provider.addTag(ModItemTags.CURIOS_HEAD)
            .replace(false)
            .addTag(ModItemTags.ANVIL_HAMMER);
        provider.addTag(ModItemTags.CURIOS_CHARM)
            .replace(false)
            .addTag(ModItemTags.AMULET);
        provider.addTag(ModItemTags.CURIOS_IONOCRAFT_BACKPACK)
            .replace(false)
            .add(ModItems.IONOCRAFT_BACKPACK.getKey());

        provider.addTag(ModItemTags.TOTEM)
            .add(findResourceKey(Items.TOTEM_OF_UNDYING));

        provider.addTag(ItemTags.SWORDS)
            .addTag(ModItemTags.HEAVY_HALBERD);
        provider.addTag(ItemTags.AXES)
            .addTag(ModItemTags.RESONATOR)
            .addTag(ModItemTags.HEAVY_HALBERD);
        provider.addTag(ItemTags.SHOVELS)
            .addTag(ModItemTags.RESONATOR);
        provider.addTag(ItemTags.HOES)
            .addTag(ModItemTags.RESONATOR);
        provider.addTag(ItemTags.PICKAXES)
            .addTag(ModItemTags.RESONATOR);

        provider.addTag(ItemTags.CLUSTER_MAX_HARVESTABLES)
            .addTag(ModItemTags.DRAGON_ROD)
            .addTag(ModItemTags.RESONATOR);

        provider.addTag(ItemTags.DURABILITY_ENCHANTABLE)
            .add(findResourceKey(ModItems.MAGNET.get()));

        provider.addTag(ModItemTags.COMPRESS_ITEM)
            .add(findResourceKey(Items.SNOW_BLOCK))
            .add(findResourceKey(Items.WHITE_WOOL))
            .add(findResourceKey(Items.MAGMA_BLOCK))
            .add(findResourceKey(ModBlocks.HEAVY_IRON_BLOCK.asItem()))
            .add(findResourceKey(ModBlocks.LEVITATION_POWDER_BLOCK.asItem()))
            .addTag(Tags.Items.INGOTS)
            .addTag(Tags.Items.STORAGE_BLOCKS);

        provider.addTag(ModItemTags.SUPER_HEATING_BOOST_PRODUCTION)
            .addTag(Tags.Items.RAW_MATERIALS)
            .addTag(Tags.Items.ORES)
            .add(findResourceKey(Items.SEA_PICKLE))
            .add(findResourceKey(Items.CACTUS));

        provider.addTag(ModItemTags.RAW_MUTTON)
            .add(findResourceKey(Items.MUTTON));

        provider.addTag(ModItemTags.RAW_BEEF)
            .add(findResourceKey(Items.BEEF));

        provider.addTag(ModItemTags.RAW_CHICKEN)
            .add(findResourceKey(Items.CHICKEN));

        provider.addTag(ModItemTags.RAW_PORKCHOP)
            .add(findResourceKey(Items.PORKCHOP));

        provider.addTag(ModItemTags.RAW_RABBIT)
            .add(findResourceKey(Items.RABBIT));

        provider.addTag(ModItemTags.SMALL_MEAT)
            .addTag(ModItemTags.RAW_CHICKEN)
            .addTag(Tags.Items.FOODS_RAW_FISH)
            .add(findResourceKey(Items.ROTTEN_FLESH))
            .add(findResourceKey(Items.SPIDER_EYE));
        provider.addTag(ModItemTags.MEDIUM_MEAT)
            .addTag(ModItemTags.RAW_BEEF)
            .addTag(ModItemTags.RAW_PORKCHOP)
            .addTag(ModItemTags.RAW_MUTTON)
            .addTag(ModItemTags.RAW_RABBIT);
        provider.addTag(ModItemTags.LARGE_MEAT)
            .add(findResourceKey(Items.ZOMBIE_HEAD))
            .add(findResourceKey(Items.PIGLIN_HEAD));

        provider.addTag(ModItemTags.DISINTEGRATION_SUPPORTED)
            .addTag(ItemTags.MINING_LOOT_ENCHANTABLE)
            .add(ModItems.LASER_GUN.getKey())
            .add(ModItems.FROST_METAL_PICKAXE.getKey())
            .add(ModItems.FROST_METAL_AXE.getKey())
            .add(ModItems.FROST_METAL_SHOVEL.getKey())
            .add(ModItems.FROST_METAL_HOE.getKey())
            .add(ModItems.FROST_METAL_SWORD.getKey())
            .add(ModItems.FROST_METAL_HEAVY_HALBERD.getKey());

        provider.addTag(ModItemTags.SMELTING_SUPPORTED)
            .addTag(ItemTags.MINING_LOOT_ENCHANTABLE)
            .add(ModItems.LASER_GUN.getKey())
            .add(ModItems.EMBER_METAL_PICKAXE.getKey())
            .add(ModItems.EMBER_METAL_AXE.getKey())
            .add(ModItems.EMBER_METAL_SHOVEL.getKey())
            .add(ModItems.EMBER_METAL_HOE.getKey())
            .add(ModItems.EMBER_METAL_SWORD.getKey())
            .add(ModItems.EMBER_METAL_HEAVY_HALBERD.getKey());

        addTwilightForestUncraftableTags(provider);
    }

    private static void addTwilightForestUncraftableTags(RegistrumTagsProvider<Item> provider) {
        TagKey<Item> bannedUncraftables = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("twilightforest", "banned_uncraftables")
        );
        TagKey<Item> bannedUncraftingIngredients = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("twilightforest", "banned_uncrafting_ingredients")
        );
        TagsProvider.TagAppender<Item> uncraftables = provider.addTag(bannedUncraftables);
        TagsProvider.TagAppender<Item> uncraftingIngredients = provider.addTag(bannedUncraftingIngredients);

        // 超限相关物品：不能被拆解，也不能通过拆解获得
        List<ItemLike> transcendenceItems = List.of(
            ModItems.TRANSCENDENCE_ANVIL_HAMMER,
            ModItems.TRANSCENDENCE_DRAGON_ROD,
            ModItems.TRANSCENDENCE_HEAVY_HALBERD,
            ModItems.TRANSCENDENCE_RESONATOR,
            ModItems.TRANSCENDIUM_INGOT,
            ModItems.TRANSCENDIUM_NUGGET,
            ModItems.MULTIPHASE_TRANSCENDIUM,
            ModBlocks.TRANSCENDENCE_ANVIL,
            ModBlocks.TRANSCENDENCE_GRINDSTONE,
            ModBlocks.TRANSCENDENCE_SMITHING_TABLE,
            ModBlocks.TRANSCENDIUM_BLOCK,
            ModBlocks.TRANSCENDENCE_DECO_BLOCK,
            ModBlocks.TRANSCENDENCE_DECO_OUTLINE
        );
        for (ItemLike item : transcendenceItems) {
            ResourceKey<Item> resourceKey = findResourceKey(item.asItem());
            uncraftables.add(resourceKey);
            uncraftingIngredients.add(resourceKey);
        }

        // 踏板：不能被拆解
        uncraftables.add(findResourceKey(ModBlocks.COPPER_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.EXPOSED_COPPER_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.WEATHERED_COPPER_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.OXIDIZED_COPPER_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.TUNGSTEN_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.TITANIUM_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.ZINC_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.TIN_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.LEAD_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.SILVER_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.URANIUM_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.PLUTONIUM_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.BRASS_PRESSURE_PLATE.asItem()))
            .add(findResourceKey(ModBlocks.BRONZE_PRESSURE_PLATE.asItem()));

        // 有多个合成途径的物品：不能被拆解
        uncraftables.add(findResourceKey(ModBlocks.HELIOSTATS.asItem()))
            .add(findResourceKey(ModBlocks.ACTIVE_SILENCER.asItem()));

        // 电容器类物品：不能通过拆解获得
        uncraftingIngredients.add(ModItems.CAPACITOR.getKey())
            .add(ModItems.CAPACITOR_EMPTY.getKey())
            .add(ModItems.SUPER_CAPACITOR.getKey())
            .add(ModItems.SUPER_CAPACITOR_EMPTY.getKey());
    }

    private static ResourceKey<Item> findResourceKey(Item item) {
        return ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item));
    }
}
