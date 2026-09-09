package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DimensionType.class)
abstract class DimensionTypeMixin {
    @ModifyReturnValue(method = "timeOfDay", at = @At("RETURN"))
    private float anvilcraft$munSolarTime(float original, long dayTime) {
        DimensionType type = (DimensionType) (Object) this;
        if (!type.effectsLocation().equals(AnvilCraft.of("mun"))) return original;
        double turns = MunSkyMath.solarAngle(dayTime, 0) / (Math.PI * 2);
        return (float) (turns - Math.floor(turns));
    }
}
