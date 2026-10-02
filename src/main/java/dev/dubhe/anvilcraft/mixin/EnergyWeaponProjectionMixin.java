package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.client.renderer.item.EnergyWeaponFirstPersonRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class EnergyWeaponProjectionMixin {
    @Shadow
    @Final
    private Projection hudProjection;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix("
            + "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V"))
    private void anvilcraft$captureWorldProjection(DeltaTracker deltaTracker, CallbackInfo callback,
                                                  @Local(name = "projectionMatrix") Matrix4f projection) {
        EnergyWeaponFirstPersonRenderer.worldProjection(projection);
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void anvilcraft$captureHandProjection(CameraRenderState camera, float partialTick, Matrix4fc view, CallbackInfo callback) {
        EnergyWeaponFirstPersonRenderer.beginHand(camera, this.hudProjection.getMatrix(new Matrix4f()), view);
    }
}
