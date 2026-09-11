package dev.dubhe.anvilcraft.mixin.projectile;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 为火球类投射物应用所在维度的空气阻力。 */
@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin {
    /** 此处只调整空气中的惯性系数，水中分支使用液体惯性系数。 */
    @ModifyExpressionValue(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractHurtingProjectile;getInertia()F"
        )
    )
    private float anvilcraft$scaleAirDrag(float vanillaDrag) {
        return AtmosphereManager.drag((AbstractHurtingProjectile) (Object) this, vanillaDrag);
    }
}
