package dev.dubhe.anvilcraft.init;

import dev.anvilcraft.lib.registrator.entry.RegistryEntry;
import dev.dubhe.anvilcraft.item.enchantment.BeheadingEnchantment;
import dev.dubhe.anvilcraft.item.enchantment.FellingEnchantment;
import dev.dubhe.anvilcraft.item.enchantment.HarvestEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATOR;

public class ModEnchantments {
    public static final RegistryEntry<FellingEnchantment> FELLING = REGISTRATOR
        .enchantment("felling", EnchantmentCategory.DIGGER, FellingEnchantment::new)
        .rarity(Enchantment.Rarity.RARE)
        .register();
    public static final RegistryEntry<HarvestEnchantment> HARVEST = REGISTRATOR
        .enchantment("harvest", EnchantmentCategory.DIGGER, HarvestEnchantment::new)
        .rarity(Enchantment.Rarity.RARE)
        .register();
    public static final RegistryEntry<BeheadingEnchantment> BEHEADING = REGISTRATOR
        .enchantment("beheading", EnchantmentCategory.WEAPON, BeheadingEnchantment::new)
        .rarity(Enchantment.Rarity.RARE)
        .register();

    /**
     * 注册附魔
     */
    public static void register() {
    }
}
