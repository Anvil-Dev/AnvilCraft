package dev.dubhe.anvilcraft.mixin.projectile;

import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** 为投掷物应用所在维度的空气阻力。 */
@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {
    /** 仅调整空气阻力，水中分支使用自身的惯性系数。 */
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.99f))
    private float anvilcraft$scaleAirDrag(float vanillaDrag) {
        return AtmosphereManager.drag((ThrowableProjectile) (Object) this, vanillaDrag);
    }
}
