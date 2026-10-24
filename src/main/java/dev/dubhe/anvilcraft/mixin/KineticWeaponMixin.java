package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KineticWeapon.class)
abstract class KineticWeaponMixin {
    @WrapOperation(
        method = "damageEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeBaseValue(Lnet/minecraft/core/Holder;)D"
        )
    )
    private double anvilcraft$applyHeavyHalberdDamage(
        LivingEntity entity, Holder<Attribute> attribute, Operation<Double> original, @Local(argsOnly = true) ItemStack stack
    ) {
        double damage = original.call(entity, attribute);
        if (stack.getItem() instanceof HeavyHalberdItem) {
            return stack.getAttributeModifiers().compute(attribute, damage, EquipmentSlot.MAINHAND);
        }
        return damage;
    }
}
