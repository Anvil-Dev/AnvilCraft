package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelReader.class)
public interface MunLevelReaderMixin {
    @Inject(method = "getMaxLocalRawBrightness(Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$moonLocalLight(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof Level level) || !level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) return;
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        int direct = skyLight == 15 ? 15 - MunSkyMath.skyDarken(pos.getX(), pos.getZ(), level.getOverworldClockTime()) : 0;
        cir.setReturnValue(Math.max(blockLight, direct));
    }
}
