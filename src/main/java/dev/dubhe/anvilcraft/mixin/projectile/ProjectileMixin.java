package dev.dubhe.anvilcraft.mixin.projectile;

import dev.dubhe.anvilcraft.util.AccelerateManager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ????????????????
 *
 * <p>??????? tick ??????????????????????????
 * ?????????????????????</p>
 */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void anvilcraft$clampAcceleratedProjectileSpeed(CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        Vec3 movement = projectile.getDeltaMovement();
        Vec3 clamped = AccelerateManager.clampMovement(projectile, movement);
        if (clamped != movement) projectile.setDeltaMovement(clamped);
    }
}
