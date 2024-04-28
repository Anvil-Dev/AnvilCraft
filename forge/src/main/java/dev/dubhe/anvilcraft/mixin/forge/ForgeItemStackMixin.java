package dev.dubhe.anvilcraft.mixin.forge;

import dev.dubhe.anvilcraft.item.HasDefaultEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.extensions.IForgeItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IForgeItemStack.class, remap = false)
abstract class ForgeItemStackMixin implements IForgeItemStack {
    @Shadow
    protected abstract ItemStack self();

    @Inject(method = "getEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private void getEnchantmentLevel(Enchantment enchantment, CallbackInfoReturnable<Integer> cir) {
        ItemStack self = self();
        if (!(self.getItem() instanceof HasDefaultEnchantment item)) return;
        int level = cir.getReturnValue();
        int defaultLevel = item.getDefaultEnchantmentLevel(enchantment);
        cir.setReturnValue(Math.max(level, defaultLevel));
    }
}
