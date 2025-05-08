package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
    @Inject(
        method = "lambda$modifyDamage$6",
        at = @At("HEAD"), cancellable = true)
    private static void cancelWhenMercilessAndTargetedDamage(
        ServerLevel level, ItemStack tool, Entity entity, DamageSource damageSource, MutableFloat mutablefloat,
        Holder<Enchantment> enchantment, int enchantmentLevel, CallbackInfo ci
    ) {
        if (tool.has(ModComponents.MERCILESS) && !enchantment.is(ModEnchantmentTags.MERCILESS_DAMAGE_PASSED))
            ci.cancel();
    }
}
