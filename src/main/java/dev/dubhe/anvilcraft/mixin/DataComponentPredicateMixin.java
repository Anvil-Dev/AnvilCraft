package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponentPredicate.class)
public abstract class DataComponentPredicateMixin {
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
            // TODO: 找到一种方式在现有条件下判断附魔是否在MERCILESS_PASSED标签内
            cir.setReturnValue(false);
        }
    }
}
