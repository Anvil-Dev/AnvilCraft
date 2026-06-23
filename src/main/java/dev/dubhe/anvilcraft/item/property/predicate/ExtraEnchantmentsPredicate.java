package dev.dubhe.anvilcraft.item.property.predicate;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Objects;

public class ExtraEnchantmentsPredicate {
    public static MercilessEnchantments merciless(List<EnchantmentPredicate> enchantments) {
        return new MercilessEnchantments(enchantments);
    }

    public static DisabledEnchantments disabled(List<EnchantmentPredicate> enchantments) {
        return new DisabledEnchantments(enchantments);
    }

    public static class MercilessEnchantments extends EnchantmentsPredicate {
        public static final Codec<MercilessEnchantments> CODEC = codec(MercilessEnchantments::new);

        protected MercilessEnchantments(List<EnchantmentPredicate> enchantments) {
            super(enchantments);
        }

        @Override
        public DataComponentType<ItemEnchantments> componentType() {
            return ModComponents.MERCILESS_ENCHANTMENTS;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MercilessEnchantments merciless)) {
                return false;
            }
            return this.enchantments().equals(merciless.enchantments());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.enchantments());
        }
    }

    public static class DisabledEnchantments extends EnchantmentsPredicate {
        public static final Codec<DisabledEnchantments> CODEC = codec(DisabledEnchantments::new);

        protected DisabledEnchantments(List<EnchantmentPredicate> enchantments) {
            super(enchantments);
        }

        @Override
        public DataComponentType<ItemEnchantments> componentType() {
            return ModComponents.DISABLED_ENCHANTMENTS;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DisabledEnchantments disabled)) {
                return false;
            }
            return this.enchantments().equals(disabled.enchantments());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.enchantments());
        }
    }
}
