package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEnchantmentsPredicate.class)
public class ItemEnchantmentsPredicateMixin {
    @Inject(
        method = "matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/ItemEnchantments;)Z",
        at = @At("HEAD"), cancellable = true)
    private void cancelWhenHasMerciless(ItemStack stack, ItemEnchantments enchantments, CallbackInfoReturnable<Boolean> cir) {
        if (stack.has(ModComponents.MERCILESS)) cir.setReturnValue(false);
    }
}
