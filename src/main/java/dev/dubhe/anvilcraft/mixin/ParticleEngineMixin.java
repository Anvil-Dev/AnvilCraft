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

    /**
        由于为具有空碰撞箱的加速环、偏转环中间部分增加了玩家持有相应物品时可互动的功能，该方法会对具有空形状的方块调用
        但是后续的 {@code net.minecraft.world.phys.shapes.VoxelShape#bounds()} 方法会抛出异常
        因此为避免崩溃，必须在这之前取消方法调用
     */
    @Inject(method = "addBlockHitEffects", at = @At("HEAD"), cancellable = true)
    private void cancelHitEffectForEmptyBlock(BlockPos pos, BlockHitResult target, CallbackInfo ci) {
        if (this.level.getBlockState(pos).getShape(this.level, pos).isEmpty()) {
            ci.cancel();
        }
    }
}
