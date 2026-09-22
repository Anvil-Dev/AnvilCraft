package dev.dubhe.anvilcraft.mixin.projectile;

import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {
    @ModifyConstant(method = "applyInertia", constant = @Constant(floatValue = 0.99F))
    private float anvilcraft$airDrag(float drag) {
        return AtmosphereManager.drag((ThrowableProjectile) (Object) this, drag);
    }
}
