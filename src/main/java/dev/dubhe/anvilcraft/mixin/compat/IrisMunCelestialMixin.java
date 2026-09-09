package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import dev.dubhe.anvilcraft.integration.iris.MunIrisCompat;
import net.irisshaders.iris.uniforms.CelestialUniforms;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CelestialUniforms.class, remap = false)
abstract class IrisMunCelestialMixin {
    @ModifyReturnValue(method = "getSunAngle", at = @At("RETURN"))
    private static float anvilcraft$localSunAngle(float original) {
        return MunClientSky.isMun() ? MunIrisCompat.sunAngle() : original;
    }

    @ModifyReturnValue(method = "isDay", at = @At("RETURN"))
    private static boolean anvilcraft$localDaylight(boolean original) {
        var level = Minecraft.getInstance().level;
        return level != null && MunClientSky.isMun() ? MunClientSky.sunlight(level) > 0 : original;
    }

    @ModifyReturnValue(method = "getCelestialPosition", at = @At("RETURN"))
    private Vector4f anvilcraft$localCelestialPosition(Vector4f original, float y) {
        return MunClientSky.isMun() ? MunIrisCompat.celestialPosition(y > 0, true) : original;
    }

    @ModifyReturnValue(method = "getCelestialPositionInWorldSpace", at = @At("RETURN"))
    private Vector4f anvilcraft$worldCelestialPosition(Vector4f original, float y) {
        return MunClientSky.isMun() ? MunIrisCompat.celestialPosition(y > 0, false) : original;
    }

    @ModifyReturnValue(method = "getShadowLightPosition", at = @At("RETURN"))
    private Vector4f anvilcraft$shadowLightPosition(Vector4f original) {
        return MunClientSky.isMun() ? MunIrisCompat.celestialPosition(true, true) : original;
    }

    @ModifyReturnValue(method = "getShadowLightPositionInWorldSpace", at = @At("RETURN"))
    private Vector4f anvilcraft$worldShadowLightPosition(Vector4f original) {
        return MunClientSky.isMun() ? MunIrisCompat.celestialPosition(true, false) : original;
    }
}
