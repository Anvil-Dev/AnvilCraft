package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantmentPredicate.class)
abstract class EnchantmentPredicateMixin {
    @WrapMethod(method = "containedIn")
    private boolean checkProvidence(ItemEnchantments itemEnchantments, Operation<Boolean> original) {
        return original.call(itemEnchantments);
    }
}
