package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.HasDefaultEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
abstract class EnchantmentHelperMixin {
    @Inject(method = "getItemEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private static void getItemEnchantmentLevel(
        Enchantment enchantment, @NotNull ItemStack stack, CallbackInfoReturnable<Integer> cir
    ) {
        if (!(stack.getItem() instanceof HasDefaultEnchantment item)) return;
        int level = cir.getReturnValue();
        int defaultLevel = item.getDefaultEnchantmentLevel(enchantment);
        cir.setReturnValue(Math.max(level, defaultLevel));
    }
}
