package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.worldgen.OverworldLikeResetManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
abstract class OverworldLikeLightMixin {
    @Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
    private void anvilcraft$eclipse(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(OverworldLikeResetManager.modifySkyDarken((Level) (Object) this, cir.getReturnValue()));
    }
}
