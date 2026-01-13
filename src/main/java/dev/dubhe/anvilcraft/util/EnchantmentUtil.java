package dev.dubhe.anvilcraft.util;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Range;

public class EnchantmentUtil {
    public static ItemEnchantments merge(ItemEnchantments oldData, ItemEnchantments newData) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(oldData);
        for (var entry : newData.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            mutable.set(holder, Math.max(oldData.getLevel(holder), entry.getIntValue()));
        }
        return mutable.toImmutable();
    }

    public static ItemEnchantments.Mutable builderOf() {
        return new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    }

    public static ItemEnchantments builtOf(Holder<Enchantment> enchHolder, @Range(from = 1, to = 255) int level) {
        var builder = builderOf();
        builder.set(enchHolder, level);
        return builder.toImmutable();
    }
}