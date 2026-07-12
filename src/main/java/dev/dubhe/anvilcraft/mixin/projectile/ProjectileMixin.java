package dev.dubhe.anvilcraft.mixin.projectile;

import dev.dubhe.anvilcraft.util.AccelerateManager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 环形加速器的通用投掷物速度修正。
 *
 * <p>投掷物进入原版 tick 前会先限制速度，避免超大移动向量使碰撞区段坐标溢出，
 * 或让一次方块、实体碰撞查询扫描过大的范围。</p>
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
