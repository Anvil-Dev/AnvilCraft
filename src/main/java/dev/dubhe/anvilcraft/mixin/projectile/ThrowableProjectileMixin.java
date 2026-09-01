package dev.dubhe.anvilcraft.mixin.projectile;

import dev.dubhe.anvilcraft.util.AirResistanceManager;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Applies the dimension air resistance to thrown projectiles. */
@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {
    /** Air resistance only; the water branch picks its own inertia. */
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.99f))
    private float anvilcraft$scaleAirDrag(float vanillaDrag) {
        return AirResistanceManager.drag((ThrowableProjectile) (Object) this, vanillaDrag);
    }
}
