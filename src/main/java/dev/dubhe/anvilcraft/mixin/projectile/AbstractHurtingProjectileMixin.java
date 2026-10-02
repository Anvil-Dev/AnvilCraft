package dev.dubhe.anvilcraft.mixin.projectile;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin {
    @ModifyExpressionValue(method = "applyInertia", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/AbstractHurtingProjectile;getInertia()F"))
    private float anvilcraft$airDrag(float drag) {
        return AtmosphereManager.drag((AbstractHurtingProjectile) (Object) this, drag);
    }
}
