package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.MeshData;
import dev.dubhe.anvilcraft.client.renderer.mun.OverworldSkyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class OverworldSkyMixin {
    @WrapOperation(method = "renderSky", at = {
        @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader("
            + "Lcom/mojang/blaze3d/vertex/MeshData;)V", ordinal = 1),
        @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader("
            + "Lcom/mojang/blaze3d/vertex/MeshData;)V", ordinal = 2)
    })
    private void anvilcraft$replaceCelestialQuads(MeshData mesh, Operation<Void> original) {
        if (OverworldSkyRenderer.enabled()) {
            mesh.close();
        } else {
            original.call(mesh);
        }
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"))
    private void anvilcraft$captureAtmosphere(CallbackInfo ci) {
        OverworldSkyRenderer.captureAtmosphere();
    }

    // 天空底色在星点之前保存，日月在星点之后合成，暗面保留大气但遮挡恒星。
    @Inject(method = "renderSky", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V"))
    private void anvilcraft$renderCelestialBodies(
        Matrix4f view, Matrix4f projection, float partialTick, Camera camera, boolean foggy,
        Runnable skyFogSetup, CallbackInfo ci
    ) {
        OverworldSkyRenderer.render(view, projection, partialTick);
    }
}
