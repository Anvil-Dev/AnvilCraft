package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import dev.dubhe.anvilcraft.util.mixin.ProvidenceRef;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
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
                 + "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;"
                 + ")V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;"
                     + "accept(Lnet/minecraft/core/Holder;I)V"
        )
    )
    private static void filterHeavyHalberdEnchantments(
        EnchantmentHelper.EnchantmentVisitor instance,
        Holder<Enchantment> enchantment,
        int level,
        Operation<Void> original,
        @Local(argsOnly = true) ItemStack stack
    ) {
        if (stack.getItem() instanceof HeavyHalberdItem
            && !HeavyHalberdItem.isEnchantmentActive(stack, enchantment)) {
            return;
        }
        original.call(instance, enchantment, level);
    }

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
        @Local(name = "enchantment") Holder<Enchantment> enchantment
    ) {
        ItemStack stack = enchantedItemInUse.itemStack();
        if (stack.getItem() instanceof HeavyHalberdItem
            && !HeavyHalberdItem.isEnchantmentActive(stack, enchantment)) {
            return;
        }
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
        int value,
        ItemStack itemStack,
        Stream<Holder<Enchantment>> source
    ) {
        List<EnchantmentInstance> modified = new ArrayList<>(original);
        String path = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath();

        boolean isRoyal = path.contains("royal");
        boolean isFrost = path.contains("frost");
        boolean isEmber = path.contains("ember");

        if (!isFrost) {
            modified.removeIf(e -> e.enchantment().is(ModEnchantments.DISINTEGRATION_KEY));
        }
        if (!isEmber) {
            modified.removeIf(e -> e.enchantment().is(ModEnchantments.SMELTING_KEY));
        }

        if (isRoyal) {
            EnchantmentHelperMixin.anvilcraft$boostEnchantment(modified, Enchantments.SILK_TOUCH);
            EnchantmentHelperMixin.anvilcraft$boostEnchantment(modified, Enchantments.UNBREAKING);
        } else if (isFrost) {
            EnchantmentHelperMixin.anvilcraft$boostEnchantment(modified, ModEnchantments.DISINTEGRATION_KEY);
        } else if (isEmber) {
            EnchantmentHelperMixin.anvilcraft$boostEnchantment(modified, ModEnchantments.SMELTING_KEY);
            EnchantmentHelperMixin.anvilcraft$boostEnchantment(modified, Enchantments.FIRE_ASPECT);
        }

        if (path.contains("transcendence")) {
            EnchantmentHelperMixin.anvilcraft$boostEnchantment(modified, Enchantments.FORTUNE);
            EnchantmentHelperMixin.anvilcraft$boostEnchantment(modified, Enchantments.LOOTING);
            EnchantmentHelperMixin.anvilcraft$addHigherLevel(modified, Enchantments.FORTUNE);
            EnchantmentHelperMixin.anvilcraft$addHigherLevel(modified, Enchantments.LOOTING);
        }

        return modified;
    }

    @Unique
    private static void anvilcraft$boostEnchantment(List<EnchantmentInstance> list, ResourceKey<Enchantment> key) {
        EnchantmentInstance target = null;
        for (EnchantmentInstance instance : list) {
            if (instance.enchantment().is(key)) {
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
            if (instance.enchantment().is(key)) {
                list.add(new EnchantmentInstance(instance.enchantment(), 4));
                list.add(new EnchantmentInstance(instance.enchantment(), 5));
                return;
            }
        }
    }

    @WrapMethod(method = "has")
    private static boolean ignorePreventDropWhenEternal(ItemStack stack, DataComponentType<?> componentType, Operation<Boolean> original) {
        if (componentType.equals(EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP) && stack.has(ModComponents.ETERNAL)) {
            return false;
        }
        return original.call(stack, componentType);
    }
}
