package dev.dubhe.anvilcraft.mixin.projectile;

import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修正高速末影珍珠的传送落点。
 *
 * <p>原版在移动珍珠前先处理命中，并使用珍珠上一 tick 的坐标传送玩家。高速情况下
 * 该坐标可能仍位于前一个偏转环，因此命中时先把珍珠的原始坐标更新到真实命中点。</p>
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
