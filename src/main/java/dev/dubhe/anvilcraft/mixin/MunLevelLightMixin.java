package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class MunLevelLightMixin {
    @ModifyReturnValue(method = "getSkyDarken", at = @At("RETURN"))
    private int anvilcraft$moonDarkness(int original) {
        var level = (Level) (Object) this;
        return level.dimension().equals(CelestialTravelManager.MUN_LEVEL)
            ? MunSkyMath.skyDarken(0, 0, level.getOverworldClockTime()) : original;
    }

    @ModifyReturnValue(method = "isBrightOutside", at = @At("RETURN"))
    private boolean anvilcraft$moonDaylight(boolean original) {
        var level = (Level) (Object) this;
        return level.dimension().equals(CelestialTravelManager.MUN_LEVEL)
            ? MunSkyMath.sunlight(0, 0, level.getOverworldClockTime(), 0) > 0 : original;
    }
}
