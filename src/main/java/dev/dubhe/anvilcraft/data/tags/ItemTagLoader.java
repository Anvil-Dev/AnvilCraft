package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

public class ItemTagLoader {
    /// 物品标签生成器初始化
    ///
    /// @param provider 提供器
    public static void init(RegistrumTagsProvider<Item> provider) {
        provider.rawBuilder(ModItemTags.AUTO_ENCHANTING_TABLE_PRIMERS)
            .addElement(ModItems.EMERALD_AMULET.getId())
            .addElement(ModBlocks.EMBER_ANVIL.getId())
            .addElement(ModBlocks.TRANSCENDENCE_ANVIL.getId());

        provider.rawBuilder(ModItemTags.PLAYER_WORKSTATIONS_STONECUTTERS)
            .addElement(ItemTagLoader.findId(Items.STONECUTTER))
            .addElement(ModBlocks.BATCH_CUTTER.getId());

        provider.rawBuilder(ModItemTags.PLATES)
            .addElement(ItemTagLoader.findId(Items.HEAVY_WEIGHTED_PRESSURE_PLATE))
            .addElement(ItemTagLoader.findId(Items.LIGHT_WEIGHTED_PRESSURE_PLATE));

        provider.rawBuilder(ModItemTags.IRON_PLATES)
            .addElement(ItemTagLoader.findId(Items.HEAVY_WEIGHTED_PRESSURE_PLATE));

        provider.rawBuilder(ModItemTags.GOLD_PLATES)
            .addElement(ItemTagLoader.findId(Items.LIGHT_WEIGHTED_PRESSURE_PLATE));

        provider.rawBuilder(ModItemTags.ROYAL_STEEL_PICKAXE_BASE)
            .addElement(ModItems.AMETHYST_PICKAXE.getId())
            .addElement(ItemTagLoader.findId(Items.GOLDEN_PICKAXE))
            .addElement(ItemTagLoader.findId(Items.IRON_PICKAXE))
            .addElement(ItemTagLoader.findId(Items.DIAMOND_PICKAXE));
        provider.rawBuilder(ModItemTags.ROYAL_STEEL_AXE_BASE)
            .addElement(ModItems.AMETHYST_AXE.getId())
            .addElement(ItemTagLoader.findId(Items.GOLDEN_AXE))
            .addElement(ItemTagLoader.findId(Items.IRON_AXE))
            .addElement(ItemTagLoader.findId(Items.DIAMOND_AXE));
        provider.rawBuilder(ModItemTags.ROYAL_STEEL_HOE_BASE)
            .addElement(ModItems.AMETHYST_HOE.getId())
            .addElement(ItemTagLoader.findId(Items.GOLDEN_HOE))
            .addElement(ItemTagLoader.findId(Items.IRON_HOE))
            .addElement(ItemTagLoader.findId(Items.DIAMOND_HOE));
        provider.rawBuilder(ModItemTags.ROYAL_STEEL_SWORD_BASE)
            .addElement(ModItems.AMETHYST_SWORD.getId())
            .addElement(ItemTagLoader.findId(Items.GOLDEN_SWORD))
            .addElement(ItemTagLoader.findId(Items.IRON_SWORD))
            .addElement(ItemTagLoader.findId(Items.DIAMOND_SWORD));
        provider.rawBuilder(ModItemTags.ROYAL_STEEL_SHOVEL_BASE)
            .addElement(ModItems.AMETHYST_SHOVEL.getId())
            .addElement(ItemTagLoader.findId(Items.GOLDEN_SHOVEL))
            .addElement(ItemTagLoader.findId(Items.IRON_SHOVEL))
            .addElement(ItemTagLoader.findId(Items.DIAMOND_SHOVEL));

        provider.rawBuilder(ModItemTags.FROST_METAL_PICKAXE_BASE)
            .addElement(ModItems.ROYAL_STEEL_PICKAXE.getId());
        provider.rawBuilder(ModItemTags.FROST_METAL_AXE_BASE)
            .addElement(ModItems.ROYAL_STEEL_AXE.getId());
        provider.rawBuilder(ModItemTags.FROST_METAL_HOE_BASE)
            .addElement(ModItems.ROYAL_STEEL_HOE.getId());
        provider.rawBuilder(ModItemTags.FROST_METAL_SWORD_BASE)
            .addElement(ModItems.ROYAL_STEEL_SWORD.getId());
        provider.rawBuilder(ModItemTags.FROST_METAL_SHOVEL_BASE)
            .addElement(ModItems.ROYAL_STEEL_SHOVEL.getId());

        provider.rawBuilder(ModItemTags.EMBER_METAL_PICKAXE_BASE)
            .addElement(ModItems.ROYAL_STEEL_PICKAXE.getId())
            .addElement(ItemTagLoader.findId(Items.NETHERITE_PICKAXE));
        provider.rawBuilder(ModItemTags.EMBER_METAL_AXE_BASE)
            .addElement(ModItems.ROYAL_STEEL_AXE.getId())
            .addElement(ItemTagLoader.findId(Items.NETHERITE_AXE));
        provider.rawBuilder(ModItemTags.EMBER_METAL_HOE_BASE)
            .addElement(ModItems.ROYAL_STEEL_HOE.getId())
            .addElement(ItemTagLoader.findId(Items.NETHERITE_HOE));
        provider.rawBuilder(ModItemTags.EMBER_METAL_SWORD_BASE)
            .addElement(ModItems.ROYAL_STEEL_SWORD.getId())
            .addElement(ItemTagLoader.findId(Items.NETHERITE_SWORD));
        provider.rawBuilder(ModItemTags.EMBER_METAL_SHOVEL_BASE)
            .addElement(ModItems.ROYAL_STEEL_SHOVEL.getId())
            .addElement(ItemTagLoader.findId(Items.NETHERITE_SHOVEL));

        provider.rawBuilder(ModItemTags.GEMS)
            .addElement(ItemTagLoader.findId(Items.EMERALD))
            .addElement(ModItems.RUBY.getId())
            .addElement(ModItems.SAPPHIRE.getId())
            .addElement(ModItems.TOPAZ.getId());
        provider.rawBuilder(ModItemTags.GEM_BLOCKS)
            .addElement(ItemTagLoader.findId(Items.EMERALD_BLOCK))
            .addElement(ItemTagLoader.findId(ModBlocks.RUBY_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.SAPPHIRE_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.TOPAZ_BLOCK.asItem()));

        provider.rawBuilder(ModItemTags.DEAD_CORALS)
            .addElement(ItemTagLoader.findId(Items.DEAD_BRAIN_CORAL))
            .addElement(ItemTagLoader.findId(Items.DEAD_BUBBLE_CORAL))
            .addElement(ItemTagLoader.findId(Items.DEAD_FIRE_CORAL))
            .addElement(ItemTagLoader.findId(Items.DEAD_HORN_CORAL))
            .addElement(ItemTagLoader.findId(Items.DEAD_TUBE_CORAL))
            .addElement(ItemTagLoader.findId(Items.DEAD_TUBE_CORAL_FAN))
            .addElement(ItemTagLoader.findId(Items.DEAD_BRAIN_CORAL_FAN))
            .addElement(ItemTagLoader.findId(Items.DEAD_BUBBLE_CORAL_FAN))
            .addElement(ItemTagLoader.findId(Items.DEAD_FIRE_CORAL_FAN))
            .addElement(ItemTagLoader.findId(Items.DEAD_HORN_CORAL_FAN));
        provider.rawBuilder(ModItemTags.DEAD_CORAL_BLOCKS)
            .addElement(ItemTagLoader.findId(Items.DEAD_BRAIN_CORAL_BLOCK))
            .addElement(ItemTagLoader.findId(Items.DEAD_BUBBLE_CORAL_BLOCK))
            .addElement(ItemTagLoader.findId(Items.DEAD_FIRE_CORAL_BLOCK))
            .addElement(ItemTagLoader.findId(Items.DEAD_HORN_CORAL_BLOCK))
            .addElement(ItemTagLoader.findId(Items.DEAD_TUBE_CORAL_BLOCK));
        provider.rawBuilder(ModItemTags.SEEDS_PACK_CONTENT)
            .addOptionalTag(Tags.Items.SEEDS.location())
            .addOptionalTag(Tags.Items.FOODS_BERRY.location())
            .addOptionalTag(Tags.Items.FOODS_VEGETABLE.location());
        provider.rawBuilder(Tags.Items.TOOLS_WRENCH)
            .addTag(ModItemTags.ANVIL_HAMMER.location());
        provider.rawBuilder(ModItemTags.FIRE_STARTER)
            .addElement(ItemTagLoader.findId(Items.TORCH))
            .addElement(ItemTagLoader.findId(Items.SOUL_TORCH))
            .addElement(ItemTagLoader.findId(Items.CAMPFIRE))
            .addElement(ItemTagLoader.findId(Items.SOUL_CAMPFIRE))
            .addElement(ItemTagLoader.findId(Items.BLAZE_POWDER));
        provider.rawBuilder(ModItemTags.UNBROKEN_FIRE_STARTER)
            .addElement(ItemTagLoader.findId(ModBlocks.REDHOT_NETHERITE_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.GLOWING_NETHERITE_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.HEATED_NETHERITE_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.INCANDESCENT_NETHERITE_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.REDHOT_TUNGSTEN_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.GLOWING_TUNGSTEN_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.HEATED_TUNGSTEN_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.INCANDESCENT_TUNGSTEN_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.asItem()));
        provider.rawBuilder(ModItemTags.NETHERITE_BLOCK)
            .addElement(ItemTagLoader.findId(Items.NETHERITE_BLOCK))
            .addElement(ItemTagLoader.findId(ModBlocks.REDHOT_NETHERITE_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.GLOWING_NETHERITE_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.HEATED_NETHERITE_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.INCANDESCENT_NETHERITE_BLOCK.asItem()));
        provider.rawBuilder(ModItemTags.EXPLOSION_PROOF)
            .addElement(ModBlocks.INFINITE_COLLECTOR.getId())
            .addElement(ItemTagLoader.findId(ModBlocks.EARTH_CORE_SHARD_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.EARTH_CORE_SHARD_ORE.asItem()))
            .addElement(ModItems.EARTH_CORE_SHARD.getId())
            .addElement(ItemTagLoader.findId(ModBlocks.EMBER_ANVIL.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.EMBER_SMITHING_TABLE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.EMBER_GRINDSTONE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.EMBER_METAL_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.EMBER_GLASS.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.CUT_EMBER_METAL_STAIRS.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.CUT_EMBER_METAL_SLAB.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.CUT_EMBER_METAL_PILLAR.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.CUT_EMBER_METAL_BLOCK.asItem()))
            .addElement(ModItems.EMBER_ANVIL_HAMMER.getId())
            .addElement(ModItems.EMBER_METAL_AXE.getId())
            .addElement(ModItems.EMBER_METAL_HOE.getId())
            .addElement(ModItems.EMBER_METAL_INGOT.getId())
            .addElement(ModItems.EMBER_METAL_NUGGET.getId())
            .addElement(ModItems.EMBER_METAL_SHOVEL.getId())
            .addElement(ModItems.EMBER_METAL_SWORD.getId())
            .addElement(ModItems.EMBER_METAL_PICKAXE.getId())
            .addElement(ModItems.NEUTRONIUM_INGOT.getId())
            .addElement(ModItems.STABLE_NEUTRONIUM_INGOT.getId())
            .addElement(ModItems.CHARGED_NEUTRONIUM_INGOT.getId());
        provider.rawBuilder(ModItemTags.TEMPLATES)
            .addTag(ModItemTags.TRIM_TEMPLATES.location())
            .addTag(ModItemTags.MULTIPLE_TO_ONE_SMITHING_TEMPLATES.location())
            .addElement(ItemTagLoader.findId(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        provider.rawBuilder(ModItemTags.TRIM_TEMPLATES)
            .addElement(ItemTagLoader.findId(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE))
            .addElement(ItemTagLoader.findId(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE));
        provider.rawBuilder(ModItemTags.UNCHARGED_NEUTRONIUM_INGOTS)
            .addElement(ModItems.NEUTRONIUM_INGOT.getId())
            .addElement(ModItems.STABLE_NEUTRONIUM_INGOT.getId());
        provider.rawBuilder(ModItemTags.HEATABLE_BLOCKS)
            .addElement(ItemTagLoader.findId(Items.NETHERITE_BLOCK));
        provider.rawBuilder(ModItemTags.DISALLOW_HAND_INSERT_INTO_TANK)
            .addTag(ModItemTags.ANVIL_HAMMER.location());

        provider.rawBuilder(ModItemTags.CURIOS_HEAD)
            .addTag(ModItemTags.ANVIL_HAMMER.location());
        provider.rawBuilder(ModItemTags.CURIOS_CHARM)
            .addElement(ModItems.LOCAL_TERMINAL.getId())
            .addElement(ModItems.SHULKER_TERMINAL.getId())
            .addElement(ModItems.HYPERDIMENSION_TERMINAL.getId())
            .addTag(ModItemTags.AMULET.location());
        provider.rawBuilder(ModItemTags.CURIOS_IONOCRAFT_BACKPACK)
            .addElement(ModItems.IONOCRAFT_BACKPACK.getId());

        provider.rawBuilder(ModItemTags.TOTEM)
            .addElement(ItemTagLoader.findId(Items.TOTEM_OF_UNDYING));

        provider.rawBuilder(ModItemTags.SWORD_ENCHANTABLE)
            .addTag(ItemTags.SWORDS.location())
            .addElement(ModItems.ANVIL_RAILGUN.getId())
            .addElement(ModItems.CORRUPTED_BEACON_ACTIVATOR.getId())
            .addElement(ModItems.LASER_GUN.getId())
            .addElement(ModItems.TESLA_GUN.getId());
        provider.rawBuilder(ItemTags.FIRE_ASPECT_ENCHANTABLE)
            .addTag(ModItemTags.SWORD_ENCHANTABLE.location());
        provider.rawBuilder(ItemTags.SWEEPING_ENCHANTABLE)
            .addTag(ModItemTags.SWORD_ENCHANTABLE.location());
        provider.rawBuilder(ItemTags.WEAPON_ENCHANTABLE)
            .addElement(ModItems.EMBER_ANVIL_HAMMER.getId())
            .addElement(ModItems.FROST_ANVIL_HAMMER.getId());
        provider.rawBuilder(Tags.Items.FOODS)
            .addElement(ModItems.CREAM.getId())
            .addElement(ModItems.FLOUR.getId())
            .addElement(ModItems.DOUGH.getId());
        provider.rawBuilder(Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
            .addElement(ModBlocks.TRANSPARENT_CRAFTING_TABLE.getId());
        provider.rawBuilder(ItemTags.SLABS).addElement(ModBlocks.PLYWOOD_SLAB.getId());
        provider.rawBuilder(ItemTags.WOODEN_SLABS).addElement(ModBlocks.PLYWOOD_SLAB.getId());
        provider.rawBuilder(ItemTags.STAIRS).addElement(ModBlocks.PLYWOOD_STAIRS.getId());
        provider.rawBuilder(ItemTags.WOODEN_STAIRS).addElement(ModBlocks.PLYWOOD_STAIRS.getId());

        provider.rawBuilder(ItemTags.SWORDS)
            .addTag(ModItemTags.HEAVY_HALBERD.location());
        provider.rawBuilder(ItemTags.AXES)
            .addTag(ModItemTags.RESONATOR.location())
            .addTag(ModItemTags.HEAVY_HALBERD.location());
        provider.rawBuilder(ItemTags.SHOVELS)
            .addTag(ModItemTags.RESONATOR.location());
        provider.rawBuilder(ItemTags.HOES)
            .addTag(ModItemTags.RESONATOR.location());
        provider.rawBuilder(ItemTags.PICKAXES)
            .addTag(ModItemTags.RESONATOR.location());

        provider.rawBuilder(ItemTags.CLUSTER_MAX_HARVESTABLES)
            .addTag(ModItemTags.DRAGON_ROD.location())
            .addTag(ModItemTags.RESONATOR.location());

        provider.rawBuilder(ItemTags.DURABILITY_ENCHANTABLE)
            .addElement(ItemTagLoader.findId(ModItems.MAGNET.get()));

        provider.rawBuilder(ModItemTags.COMPRESS_ITEM)
            .addElement(ItemTagLoader.findId(Items.AMETHYST_BLOCK))
            .addElement(ItemTagLoader.findId(Items.BRICKS))
            .addElement(ItemTagLoader.findId(Items.CLAY))
            .addElement(ItemTagLoader.findId(Items.DRIPSTONE_BLOCK))
            .addElement(ItemTagLoader.findId(Items.GLOWSTONE))
            .addElement(ItemTagLoader.findId(Items.HONEYCOMB_BLOCK))
            .addElement(ItemTagLoader.findId(Items.NETHER_BRICKS))
            .addElement(ItemTagLoader.findId(Items.PRISMARINE))
            .addElement(ItemTagLoader.findId(Items.QUARTZ_BLOCK))
            .addElement(ItemTagLoader.findId(Items.RED_SANDSTONE))
            .addElement(ItemTagLoader.findId(Items.SANDSTONE))
            .addElement(ModItems.NEGATIVE_MATTER.getId())
            .addElement(ItemTagLoader.findId(Items.SNOW_BLOCK))
            .addElement(ItemTagLoader.findId(Items.WHITE_WOOL))
            .addElement(ItemTagLoader.findId(Items.MAGMA_BLOCK))
            .addElement(ItemTagLoader.findId(ModBlocks.HEAVY_IRON_BLOCK.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.LEVITATION_POWDER_BLOCK.asItem()))
            .addTag(Tags.Items.INGOTS.location())
            .addTag(Tags.Items.STORAGE_BLOCKS.location());

        provider.rawBuilder(ModItemTags.SUPER_HEATING_BOOST_PRODUCTION)
            .addTag(Tags.Items.RAW_MATERIALS.location())
            .addTag(Tags.Items.ORES.location())
            .addElement(ItemTagLoader.findId(Items.SEA_PICKLE))
            .addElement(ItemTagLoader.findId(Items.CACTUS));

        provider.rawBuilder(ModItemTags.RAW_MUTTON)
            .addElement(ItemTagLoader.findId(Items.MUTTON));

        provider.rawBuilder(ModItemTags.RAW_BEEF)
            .addElement(ItemTagLoader.findId(Items.BEEF));

        provider.rawBuilder(ModItemTags.RAW_CHICKEN)
            .addElement(ItemTagLoader.findId(Items.CHICKEN));

        provider.rawBuilder(ModItemTags.RAW_PORKCHOP)
            .addElement(ItemTagLoader.findId(Items.PORKCHOP));

        provider.rawBuilder(ModItemTags.RAW_RABBIT)
            .addElement(ItemTagLoader.findId(Items.RABBIT));

        provider.rawBuilder(ModItemTags.SMALL_MEAT)
            .addTag(ModItemTags.RAW_CHICKEN.location())
            .addTag(Tags.Items.FOODS_RAW_FISH.location())
            .addElement(ItemTagLoader.findId(Items.ROTTEN_FLESH))
            .addElement(ItemTagLoader.findId(Items.SPIDER_EYE));
        provider.rawBuilder(ModItemTags.MEDIUM_MEAT)
            .addTag(ModItemTags.RAW_BEEF.location())
            .addTag(ModItemTags.RAW_PORKCHOP.location())
            .addTag(ModItemTags.RAW_MUTTON.location())
            .addTag(ModItemTags.RAW_RABBIT.location());
        provider.rawBuilder(ModItemTags.LARGE_MEAT)
            .addElement(ItemTagLoader.findId(Items.ZOMBIE_HEAD))
            .addElement(ItemTagLoader.findId(Items.PIGLIN_HEAD));

        provider.rawBuilder(ModItemTags.DISINTEGRATION_SUPPORTED)
            .addTag(ItemTags.MINING_LOOT_ENCHANTABLE.location())
            .addElement(ModItems.LASER_GUN.getId())
            .addElement(ModItems.FROST_METAL_PICKAXE.getId())
            .addElement(ModItems.FROST_METAL_AXE.getId())
            .addElement(ModItems.FROST_METAL_SHOVEL.getId())
            .addElement(ModItems.FROST_METAL_HOE.getId())
            .addElement(ModItems.FROST_METAL_SWORD.getId())
            .addElement(ModItems.FROST_METAL_HEAVY_HALBERD.getId());

        provider.rawBuilder(ModItemTags.SMELTING_SUPPORTED)
            .addTag(ItemTags.MINING_LOOT_ENCHANTABLE.location())
            .addElement(ModItems.LASER_GUN.getId())
            .addElement(ModItems.EMBER_METAL_PICKAXE.getId())
            .addElement(ModItems.EMBER_METAL_AXE.getId())
            .addElement(ModItems.EMBER_METAL_SHOVEL.getId())
            .addElement(ModItems.EMBER_METAL_HOE.getId())
            .addElement(ModItems.EMBER_METAL_SWORD.getId())
            .addElement(ModItems.EMBER_METAL_HEAVY_HALBERD.getId());

        provider.rawBuilder(ModItemTags.AMETHYST_TOOL_MATERIALS)
            .addElement(ItemTagLoader.findId(Items.AMETHYST_SHARD));

        provider.rawBuilder(ModItemTags.ROYAL_STEEL_TOOL_MATERIALS)
            .addElement(ModItems.ROYAL_STEEL_INGOT.getId());

        provider.rawBuilder(ModItemTags.WEATHERPROOF_REPAIR_MATERIALS)
            .addElement(ModItems.MULTIPHASE_MATTER.getId());
        provider.rawBuilder(ModItemTags.UNIVERSAL_REPAIR_MATERIALS)
            .addElement(ModItems.FROST_METAL_INGOT.getId());
        provider.rawBuilder(ModItemTags.FROST_METAL_TOOL_MATERIALS)
            .addElement(ModItems.FROST_METAL_INGOT.getId());

        provider.rawBuilder(ModItemTags.EMBER_METAL_TOOL_MATERIALS)
            .addElement(ModItems.EMBER_METAL_INGOT.getId());

        provider.rawBuilder(ModItemTags.TRANSCENDIUM_TOOL_MATERIALS)
            .addElement(ModItems.TRANSCENDIUM_INGOT.getId());

        ItemTagLoader.addTwilightForestUncraftableTags(provider);
    }

    private static void addTwilightForestUncraftableTags(RegistrumTagsProvider<Item> provider) {
        TagKey<Item> bannedUncraftables = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("twilightforest", "banned_uncraftables")
        );
        TagKey<Item> bannedUncraftingIngredients = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("twilightforest", "banned_uncrafting_ingredients")
        );
        var uncraftables = provider.rawBuilder(bannedUncraftables);
        var uncraftingIngredients = provider.rawBuilder(bannedUncraftingIngredients);

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
            Identifier id = ItemTagLoader.findId(item.asItem());
            uncraftables.addElement(id);
            uncraftingIngredients.addElement(id);
        }

        // 踏板：不能被拆解
        uncraftables.addElement(ItemTagLoader.findId(ModBlocks.COPPER_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.EXPOSED_COPPER_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.WEATHERED_COPPER_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.OXIDIZED_COPPER_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.TUNGSTEN_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.TITANIUM_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.ZINC_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.TIN_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.LEAD_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.SILVER_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.URANIUM_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.PLUTONIUM_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.BRASS_PRESSURE_PLATE.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.BRONZE_PRESSURE_PLATE.asItem()));

        // 有多个合成途径的物品：不能被拆解
        uncraftables.addElement(ItemTagLoader.findId(ModBlocks.HELIOSTATS.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.ACTIVE_SILENCER.asItem()))
            .addElement(ItemTagLoader.findId(ModBlocks.EXP_COLLECTOR.asItem()));

        // 电容器类物品：不能通过拆解获得
        uncraftingIngredients.addElement(ModItems.CAPACITOR.getId())
            .addElement(ModItems.CAPACITOR_EMPTY.getId())
            .addElement(ModItems.SUPER_CAPACITOR.getId())
            .addElement(ModItems.SUPER_CAPACITOR_EMPTY.getId());
    }

    private static Identifier findId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
