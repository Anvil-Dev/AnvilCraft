package dev.dubhe.anvilcraft.mixin;

import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Objects;

@Mixin(EnchantmentsPredicate.class)
public class EnchantmentsPredicateMixin {
    @Shadow
    @Final
    private List<EnchantmentPredicate> enchantments;

    @Override
    public boolean equals(Object obj) {
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        return this.enchantments.equals(((EnchantmentsPredicateMixin) obj).enchantments);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.enchantments);
    }
}
