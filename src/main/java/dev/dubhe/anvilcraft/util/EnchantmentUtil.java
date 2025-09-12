package dev.dubhe.anvilcraft.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Comparator;

public class EnchantmentUtil {
    public static EnchantmentInstance toInstance(Object2IntMap.Entry<Holder<Enchantment>> entry) {
        return new EnchantmentInstance(entry.getKey(), entry.getIntValue());
    }

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

    public static ItemEnchantments merge(ItemEnchantments oldData, ItemEnchantments newData) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(oldData);
        for (var entry : newData.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            mutable.set(holder, Math.max(oldData.getLevel(holder), entry.getIntValue()));
        }
        return mutable.toImmutable();
    }
}
