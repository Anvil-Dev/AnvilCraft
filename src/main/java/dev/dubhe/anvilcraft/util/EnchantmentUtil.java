package dev.dubhe.anvilcraft.util;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.Comparator;

public class EnchantmentUtil {
    public static int compareEnchantmentHolder(Holder<Enchantment> o1, Holder<Enchantment> o2) {
        return o1.getRegisteredName().compareTo(o2.getRegisteredName());
    }

    public static int compareEnchantmentInstance(EnchantmentInstance o1, EnchantmentInstance o2) {
        if (o1.enchantment.equals(o2.enchantment)) {
            return Comparator.<EnchantmentInstance>comparingInt(o -> o.level).compare(o1, o2);
        } else {
            return compareEnchantmentHolder(o1.enchantment, o2.enchantment);
        }
    }
}
