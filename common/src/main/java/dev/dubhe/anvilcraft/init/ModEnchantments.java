package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.item.enchantment.FellingEnchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModEnchantments {
    public static final RegistryEntry<FellingEnchantment> FELLING = REGISTRATE
        .enchantment("felling", EnchantmentCategory.DIGGER, FellingEnchantment::new)
        .register();

    /**
     * 注册附魔
     */
    public static void register() {
    }
}
