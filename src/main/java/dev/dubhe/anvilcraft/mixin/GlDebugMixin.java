package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.platform.GlDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlDebug.class)
public class GlDebugMixin {
    @Inject(
        method = "printDebugLog",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V", shift = At.Shift.BEFORE),
        cancellable = true
    )
    private static void avoidSpam(int source, int type, int id, int severity, int messageLength, long message, long userParam, CallbackInfo ci) {
        if (severity != 37190) {
            ci.cancel();
            return;
        }
        new Throwable().printStackTrace();
    }
}
