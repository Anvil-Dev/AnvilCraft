package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnvironmentAttributeSystem.class)
public abstract class MunEnvironmentMixin {
    @Inject(method = "addDefaultLayers", at = @At("RETURN"))
    private static void anvilcraft$moonSolarAngle(EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        if (!level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) return;
        builder.addTimeBasedLayer(EnvironmentAttributes.SUN_ANGLE,
            (previous, tick) -> MunSkyMath.solarTime(level.getOverworldClockTime()) * 360);
    }
}
