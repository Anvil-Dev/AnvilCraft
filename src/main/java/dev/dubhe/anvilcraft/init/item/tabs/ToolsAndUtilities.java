package dev.dubhe.anvilcraft.init.item.tabs;

import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.item.enchantment.Enchantments;

public class ToolsAndUtilities extends DisplayItemsGenerator {
    @Override
    public void accept() {
        // Guides, exploration and utility tools.
        this.plain(ModItems.GUIDE_BOOK);
        this.plain(ModItems.GEODE);
        this.plain(ModItems.MAGNET);
        this.plain(ModItems.CRAB_CLAW);
        this.plain(ModItems.DISK);
        this.plain(ModItems.STRUCTURE_DISK);
        this.plain(ModItems.FILTER);

        // Anvil hammers and enchanted starter tools.
        this.plain(ModItems.ANVIL_HAMMER);
        this.plain(ModItems.ROYAL_ANVIL_HAMMER);
        this.plain(ModItems.FROST_ANVIL_HAMMER);
        this.plain(ModItems.EMBER_ANVIL_HAMMER);
        this.plain(ModItems.TRANSCENDENCE_ANVIL_HAMMER);
        this.enchanting(ModItems.AMETHYST_PICKAXE, Enchantments.FORTUNE, 3);
        this.enchanting(ModItems.AMETHYST_AXE, ModEnchantments.FELLING_KEY, 1);
        this.enchanting(ModItems.AMETHYST_SHOVEL, Enchantments.EFFICIENCY, 3);
        this.enchanting(ModItems.AMETHYST_HOE, ModEnchantments.HARVEST_KEY, 1);
        this.enchanting(ModItems.AMETHYST_SWORD, ModEnchantments.BEHEADING_KEY, 1);

        // Tool sets are grouped by material and use the same tool order.
        this.plain(ModItems.ROYAL_STEEL_PICKAXE);
        this.plain(ModItems.ROYAL_STEEL_AXE);
        this.plain(ModItems.ROYAL_STEEL_SHOVEL);
        this.plain(ModItems.ROYAL_STEEL_HOE);
        this.plain(ModItems.ROYAL_STEEL_SWORD);
        this.plain(ModItems.FROST_METAL_PICKAXE);
        this.plain(ModItems.FROST_METAL_AXE);
        this.plain(ModItems.FROST_METAL_SHOVEL);
        this.plain(ModItems.FROST_METAL_HOE);
        this.plain(ModItems.FROST_METAL_SWORD);
        this.plain(ModItems.EMBER_METAL_PICKAXE);
        this.plain(ModItems.EMBER_METAL_AXE);
        this.plain(ModItems.EMBER_METAL_SHOVEL);
        this.plain(ModItems.EMBER_METAL_HOE);
        this.plain(ModItems.EMBER_METAL_SWORD);
        this.plain(ModItems.MULTITOOL_ITEM);

        // Specialized melee and resonance tools.
        this.plain(ModItems.DRAGON_ROD);
        this.plain(ModItems.ROYAL_DRAGON_ROD);
        this.plain(ModItems.FROST_DRAGON_ROD);
        this.plain(ModItems.EMBER_DRAGON_ROD);
        this.plain(ModItems.TRANSCENDENCE_DRAGON_ROD);
        this.plain(ModItems.FROST_METAL_RESONATOR);
        this.plain(ModItems.EMBER_METAL_RESONATOR);
        this.plain(ModItems.TRANSCENDENCE_RESONATOR);
        this.plain(ModItems.FROST_METAL_HEAVY_HALBERD);
        this.plain(ModItems.EMBER_METAL_HEAVY_HALBERD);
        this.plain(ModItems.TRANSCENDENCE_HEAVY_HALBERD);

        // Ranged and energy weapons.
        this.plain(ModItems.SPECTRAL_SLINGSHOT);
        this.plain(ModItems.ENERGY_WEAPON_PLATFORM);
        this.plain(ModItems.SPECTRAL_WEAPON_LAUNCHER);
        this.plain(ModItems.ANVIL_RAILGUN);
        this.plain(ModItems.CORRUPTED_BEACON_ACTIVATOR);
        this.plain(ModItems.TESLA_GUN);
        this.plain(ModItems.LASER_GUN);
        this.plain(ModItems.IONOCRAFT);
        this.ionoCraftBackpack(ModItems.IONOCRAFT_BACKPACK);

        // Storage terminals.
        this.plain(ModItems.LOCAL_TERMINAL); // 本地终端
        this.plain(ModItems.SHULKER_TERMINAL); // 潜影终端
        this.plain(ModItems.HYPERDIMENSION_TERMINAL); // 超维终端

        // Energy storage.
        this.plain(ModItems.CAPACITOR);
        this.plain(ModItems.CAPACITOR_EMPTY);
        this.plain(ModItems.SUPER_CAPACITOR);
        this.plain(ModItems.SUPER_CAPACITOR_EMPTY);

        this.plain(ModItems.FLUID_TANK_MINECART); // 流体储罐矿车

        // Recovery items and amulets.
        this.plain(ModItems.RECOVERY_PEARL);
        this.plain(ModItems.TOTEM_OF_RECOVERY);
        this.plain(ModItems.TOTEM_OF_RAGE);
        this.plain(ModItems.EMERALD_AMULET);
        this.plain(ModItems.TOPAZ_AMULET);
        this.plain(ModItems.RUBY_AMULET);
        this.plain(ModItems.SAPPHIRE_AMULET);
        this.plain(ModItems.ANVIL_AMULET);
        this.plain(ModItems.COMRADE_AMULET);
        this.plain(ModItems.FEATHER_AMULET);
        this.plain(ModItems.CAT_AMULET);
        this.plain(ModItems.DOG_AMULET);
        this.plain(ModItems.SILENCE_AMULET);
        this.plain(ModItems.ABNORMAL_AMULET);
        this.plain(ModItems.GEM_AMULET);
        this.plain(ModItems.NATURE_AMULET);
        this.plain(ModItems.AMULET_BOX);

        // Food and consumables. Ingredient foods also appear in Ingredients.
        this.plain(ModFoodItems.CURSED_GOLDEN_APPLE);
        this.plain(ModFoodItems.CHOCOLATE);
        this.plain(ModFoodItems.CHOCOLATE_BLACK);
        this.plain(ModFoodItems.CHOCOLATE_WHITE);
        this.plain(ModFoodItems.CREAMY_BREAD_ROLL);
        this.plain(ModFoodItems.BEEF_MUSHROOM_STEW);
        this.plain(ModFoodItems.PILL);
        this.plain(ModItems.PILL_BOX);
        this.plain(ModItems.TIN_CAN);
        this.plain(ModFoodItems.CANNED_FOOD);
        this.plain(ModItems.SEEDS_PACK);
        this.plain(ModItems.STRUCTURE_TOOL);
    }
}
