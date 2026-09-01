package dev.dubhe.anvilcraft.mixin.projectile;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.util.AirResistanceManager;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies the dimension air resistance to fireball-like projectiles. */
@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin {
    /** Only the airborne inertia goes through this method; the water branch uses the liquid inertia. */
    @ModifyExpressionValue(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractHurtingProjectile;getInertia()F"
        )
    )
    private float anvilcraft$scaleAirDrag(float vanillaDrag) {
        return AirResistanceManager.drag((AbstractHurtingProjectile) (Object) this, vanillaDrag);
    }
}
