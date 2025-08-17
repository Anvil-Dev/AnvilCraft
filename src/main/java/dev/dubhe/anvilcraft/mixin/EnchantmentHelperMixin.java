package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
import dev.dubhe.anvilcraft.util.mixin.ref.ProvidenceRef;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentHelper.class)
abstract class EnchantmentHelperMixin {
    @Unique
    private static final ThreadLocal<ResourceKey<Level>> anvilcraft$dimension = new ThreadLocal<>();

    @Inject(method = "doPostAttackEffectsWithItemSource", at = @At("HEAD"))
    private static void cachedServerLevel(
        @NotNull ServerLevel level,
        Entity entity,
        DamageSource damageSource,
        ItemStack itemSource,
        CallbackInfo ci
    ) {
        anvilcraft$dimension.set(level.dimension());
    }

    @WrapOperation(
        method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/entity/EquipmentSlot;"
            + "Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;"
                + "accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"))
    private static void checkShouldTriggerProvidence(
        EnchantmentHelper.EnchantmentInSlotVisitor instance,
        Holder<Enchantment> holder,
        int i,
        EnchantedItemInUse enchantedItemInUse,
        @NotNull Operation<Void> original,
        @Local @NotNull Holder<Enchantment> enchantment,
        @Local(argsOnly = true) LivingEntity entity
    ) {
        original.call(instance, holder, i, enchantedItemInUse);
        if (!enchantment.is(ModEnchantmentTags.PROVIDENCE_BONUS)) {
            anvilcraft$dimension.remove();
            return;
        }
        ProvidenceRef.shouldTrigger(anvilcraft$dimension.get(), i, enchantedItemInUse, entity.getId());
    }
}
