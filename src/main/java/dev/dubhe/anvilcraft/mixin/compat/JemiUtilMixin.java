package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.AnvilCraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;
import java.util.Set;

@Pseudo
@Mixin(targets = "dev.emi.emi.jemi.JemiUtil", remap = false)
abstract class JemiUtilMixin {
    @ModifyReturnValue(method = "getHandledMods", at = @At("RETURN"))
    private static Set<String> anvilcraft$keepJeiPlugin(Set<String> namespaces) {
        // The 26.1 EMI bridge also filters plugins before JEI can register their categories.
        Set<String> result = new HashSet<>(namespaces);
        result.remove(AnvilCraft.MOD_ID);
        return result;
    }
}
