package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DaylightDetectorBlock.class)
public abstract class MunDaylightDetectorMixin {
    @ModifyExpressionValue(method = "updateSignalStrength", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;cos(D)F"))
    private static float anvilcraft$sourceMoonCosine(float original, BlockState state, Level level, BlockPos pos) {
        return level.dimension().equals(CelestialTravelManager.MUN_LEVEL)
            ? MunSkyMath.daylightDetectorCosine(level.getOverworldClockTime()) : original;
    }
}
