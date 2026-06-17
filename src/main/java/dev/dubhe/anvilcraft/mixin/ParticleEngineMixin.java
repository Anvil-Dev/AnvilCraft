package dev.dubhe.anvilcraft.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {

    @Shadow
    protected ClientLevel level;

    @Inject(method = "addBlockHitEffects", at = @At("HEAD"), cancellable = true)
    private void damage(BlockPos pos, BlockHitResult target, CallbackInfo ci) {
        if (this.level.getBlockState(pos).getShape(this.level, pos).isEmpty()) {
            ci.cancel();
        }
    }
}
