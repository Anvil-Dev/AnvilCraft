package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.util.mixin.ProvidenceRef;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
abstract class EnchantmentHelperMixin {
    @WrapOperation(
        method = "runIterationOnItem("
                 + "Lnet/minecraft/world/item/ItemStack;"
                 + "Lnet/minecraft/world/entity/EquipmentSlot;"
                 + "Lnet/minecraft/world/entity/LivingEntity;"
                 + "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;"
                 + ")V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;"
                     + "accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"
        )
    )
    private static void checkShouldTriggerProvidence(
        EnchantmentHelper.EnchantmentInSlotVisitor instance,
        Holder<Enchantment> holder,
        int i,
        EnchantedItemInUse enchantedItemInUse,
        Operation<Void> original,
        @Local Holder<Enchantment> enchantment
    ) {
        if (!enchantment.is(ModEnchantmentTags.PROVIDENCE_BONUS)) {
            original.call(instance, holder, i, enchantedItemInUse);
            return;
        }
        ProvidenceRef.shouldTrigger();
        original.call(instance, holder, i, enchantedItemInUse);
        ProvidenceRef.reset();
    }
}
