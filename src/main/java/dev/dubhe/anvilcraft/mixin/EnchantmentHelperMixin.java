package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.util.mixin.ProvidenceRef;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    @ModifyReturnValue(
        method = "getAvailableEnchantmentResults",
        at = @At("RETURN")
    )
    private static List<EnchantmentInstance> modifyEnchantmentResults(
        List<EnchantmentInstance> original,
        int level,
        ItemStack stack,
        Stream<Holder<Enchantment>> possibleEnchantments
    ) {
        List<EnchantmentInstance> modified = new ArrayList<>(original);
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        boolean isRoyal = path.contains("royal");
        boolean isFrost = path.contains("frost");
        boolean isEmber = path.contains("ember");

        if (!isFrost) {
            modified.removeIf(e -> e.enchantment.is(ModEnchantments.DISINTEGRATION_KEY));
        }
        if (!isEmber) {
            modified.removeIf(e -> e.enchantment.is(ModEnchantments.SMELTING_KEY));
        }

        if (isRoyal) {
            anvilcraft$boostEnchantment(modified, Enchantments.SILK_TOUCH);
            anvilcraft$boostEnchantment(modified, Enchantments.UNBREAKING);
        } else if (isFrost) {
            anvilcraft$boostEnchantment(modified, ModEnchantments.DISINTEGRATION_KEY);
        } else if (isEmber) {
            anvilcraft$boostEnchantment(modified, ModEnchantments.SMELTING_KEY);
            anvilcraft$boostEnchantment(modified, Enchantments.FIRE_ASPECT);
        }

        boolean isTranscendence = path.contains("transcendence");
        if (isTranscendence) {
            anvilcraft$boostEnchantment(modified, Enchantments.FORTUNE);
            anvilcraft$boostEnchantment(modified, Enchantments.LOOTING);
            anvilcraft$addHigherLevel(modified, Enchantments.FORTUNE);
            anvilcraft$addHigherLevel(modified, Enchantments.LOOTING);
        }

        return modified;
    }

    @Unique
    private static void anvilcraft$boostEnchantment(List<EnchantmentInstance> list, ResourceKey<Enchantment> key) {
        EnchantmentInstance target = null;
        for (EnchantmentInstance instance : list) {
            if (instance.enchantment.is(key)) {
                target = instance;
                break;
            }
        }
        if (target != null) {
            for (int i = 0; i < 10; i++) {
                list.add(target);
            }
        }
    }

    @Unique
    private static void anvilcraft$addHigherLevel(List<EnchantmentInstance> list, ResourceKey<Enchantment> key) {
        for (EnchantmentInstance instance : list) {
            if (instance.enchantment.is(key)) {
                list.add(new EnchantmentInstance(instance.enchantment, 4));
                list.add(new EnchantmentInstance(instance.enchantment, 5));
                return;
            }
        }
    }
}
