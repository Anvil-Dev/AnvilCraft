package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantmentPredicate.class)
abstract class EnchantmentPredicateMixin {
    @WrapMethod(method = "containedIn")
    private boolean checkProvidence(
        ItemEnchantments enchantments,
        @NotNull Operation<Boolean> original
    ) {
        return original.call(enchantments);
    }
}
