package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponentPredicate.class)
public abstract class DataComponentPredicateMixin {
    @Shadow public abstract boolean equals(Object other);

    @Inject(
        method = "test(Lnet/minecraft/core/component/DataComponentMap;)Z",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/core/component/DataComponentMap;get("
                     + "Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"),
        cancellable = true)
    private void cancelWhenMerciless(
        DataComponentMap components, CallbackInfoReturnable<Boolean> cir, @Local TypedDataComponent<?> component
    ) {
        if (components.has(ModComponents.MERCILESS)
            && component.type().equals(DataComponents.ENCHANTMENTS)
        ) {
            Holder<Enchantment> enchantmentHolder = Holder.direct((Enchantment) component.value());
            cir.setReturnValue(enchantmentHolder.is(ModEnchantmentTags.MERCILESS_PASSED));
        }
    }
}
