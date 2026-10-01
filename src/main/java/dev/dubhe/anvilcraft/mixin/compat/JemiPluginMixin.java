package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.AnvilCraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Pseudo
@Mixin(targets = "dev.emi.emi.jemi.JemiPlugin", remap = false)
abstract class JemiPluginMixin {
    @WrapOperation(
        method = "register",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z", ordinal = 0)
    )
    private boolean anvilcraft$keepJeiCategories(Set<?> namespaces, Object namespace, Operation<Boolean> original) {
        // AnvilCraft's native EMI plugin only supplies screen bounds, not recipe categories.
        return !AnvilCraft.MOD_ID.equals(namespace) && original.call(namespaces, namespace);
    }
}
