package dev.dubhe.anvilcraft.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;

public record EnchantmentData(
    DataComponentType<ItemEnchantments> type,
    Holder<Enchantment> enchantment,
    int level
) implements Comparable<EnchantmentData> {
    public EnchantmentData(DataComponentType<ItemEnchantments> type, EnchantmentInstance inst) {
        this(type, inst.enchantment, inst.level);
    }

    public EnchantmentInstance toEnchantmentInst() {
        return new EnchantmentInstance(this.enchantment, this.level);
    }

    @Override
    public int compareTo(@NotNull EnchantmentData that) {
        if (this.type.equals(that.type)) {
            if (this.enchantment.equals(that.enchantment)) {
                return Integer.compare(this.level, that.level);
            } else {
                return this.enchantment.getRegisteredName().compareTo(that.enchantment.getRegisteredName());
            }
        } else {
            int thisWeight = EnchantmentData.weight(this.type);
            int thatWeight = EnchantmentData.weight(that.type);
            if (thisWeight == thatWeight) {
                return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(this.type)
                    .compareTo(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(that.type));
            } else {
                return Integer.compare(this.level, that.level);
            }
        }
    }

    private static int weight(DataComponentType<ItemEnchantments> type) {
        return switch (type) {
            case DataComponentType<ItemEnchantments> it when it == DataComponents.ENCHANTMENTS -> 2;
            case DataComponentType<ItemEnchantments> it when it == DataComponents.STORED_ENCHANTMENTS -> 1;
            default -> 0;
        };
    }
}
