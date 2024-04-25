package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.item.enchantment.BeheadingEnchantment;
import dev.dubhe.anvilcraft.item.enchantment.FellingEnchantment;
import dev.dubhe.anvilcraft.item.enchantment.HarvestEnchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModEnchantments {
    public static final RegistryEntry<FellingEnchantment> FELLING = REGISTRATE
        .enchantment("felling", EnchantmentCategory.DIGGER, FellingEnchantment::new)
        .register();
    public static final RegistryEntry<HarvestEnchantment> HARVEST = REGISTRATE
        .enchantment("harvest", EnchantmentCategory.DIGGER, HarvestEnchantment::new)
        .register();
    public static final RegistryEntry<BeheadingEnchantment> BEHEADING = REGISTRATE
        .enchantment("beheading", EnchantmentCategory.DIGGER, BeheadingEnchantment::new)
        .register();

    /**
     * 注册附魔
     */
    public static void register() {
    }
}
