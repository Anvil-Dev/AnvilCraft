package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeResetManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
abstract class LevelMixin {
    @ModifyReturnValue(method = "getSkyDarken", at = @At("RETURN"))
    private int anvilcraft$addOverworldLikeEclipseDarken(int original) {
        Level level = (Level) (Object) this;
        // 无坐标的维度查询以月球原点为基准，局部方块光照在 LevelReader 中单独计算。
        if (level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) return MunSkyMath.skyDarken(0, 0, level.getDayTime());
        return OverworldLikeResetManager.modifySkyDarken(level, original);
    }

    @ModifyReturnValue(method = "isDay", at = @At("RETURN"))
    private boolean anvilcraft$munDaylight(boolean original) {
        Level level = (Level) (Object) this;
        if (!level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) return original;
        return MunSkyMath.sunlight(0, 0, level.getDayTime(), 0) > 0;
    }
}
