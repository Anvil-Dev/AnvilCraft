package dev.dubhe.anvilcraft.mixin.projectile;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ??????????????
 *
 * <p>????????????????????? tick ?????????????
 * ??????????????????????????????????????</p>
 */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void anvilcraft$moveToHighSpeedHitLocation(HitResult result, CallbackInfo ci) {
        ThrownEnderpearl pearl = (ThrownEnderpearl) (Object) this;
        Vec3 location = result.getLocation();
        pearl.setPosRaw(location.x, location.y, location.z);
    }
}
