package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.init.ModRenderTargets;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.renderer.MunSurfaceRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import javax.annotation.Nullable;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {

    @ModifyReturnValue(method = "getRendertypeSolidShader", at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munSolid(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.terrain(original, 0);
    }

    @ModifyReturnValue(method = "getRendertypeCutoutMippedShader", at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munCutoutMipped(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.terrain(original, 0.5F);
    }

    @ModifyReturnValue(method = "getRendertypeCutoutShader", at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munCutout(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.terrain(original, 0.1F);
    }

    @ModifyReturnValue(method = "getRendertypeTranslucentShader", at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munTranslucent(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.terrain(original, 0);
    }

    @ModifyReturnValue(method = "getRendertypeTripwireShader", at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munTripwire(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.terrain(original, 0.1F);
    }

    @ModifyReturnValue(method = "getRendertypeEntitySolidShader", at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munEntitySolid(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.entity(original, 0, 1);
    }

    @ModifyReturnValue(method = {
        "getRendertypeEntityCutoutShader", "getRendertypeEntityCutoutNoCullShader",
        "getRendertypeEntityCutoutNoCullZOffsetShader", "getRendertypeEntityTranslucentShader",
        "getRendertypeEntitySmoothCutoutShader"
    }, at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munEntityCutout(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.entity(original, 0.1F, 1);
    }

    @ModifyReturnValue(method = {
        "getRendertypeArmorCutoutNoCullShader", "getRendertypeItemEntityTranslucentCullShader",
        "getRendertypeEntityTranslucentCullShader"
    }, at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munEquipment(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.entity(original, 0.1F, 0);
    }

    @ModifyReturnValue(method = "getRendertypeEntityNoOutlineShader", at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munEntityNoOutline(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.entity(original, 0, 0);
    }

    @ModifyReturnValue(method = "getRendertypeEntityDecalShader", at = @At("RETURN"))
    private static @Nullable ShaderInstance anvilcraft$munEntityDecal(@Nullable ShaderInstance original) {
        return MunSurfaceRenderer.entity(original, 0.1F, 2);
    }

    @Inject(
        method = "reloadShaders",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;loadBlurEffect(Lnet/minecraft/server/packs/resources/ResourceProvider;)V"
        )
    )
    void loadBloomEffect(ResourceProvider resourceProvider, CallbackInfo ci) throws IOException {
        ModShaders.loadBloomEffect(resourceProvider);
        ModShaders.loadLensEffect(resourceProvider);
    }

    @Inject(
        method = "resize",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;resize(II)V"
        )
    )
    void resize(int width, int height, CallbackInfo ci) {
        ModShaders.resize(width, height);
        if (ModRenderTargets.getTempTarget() != null) {
            ModRenderTargets.getTempTarget().resize(width, height, Minecraft.ON_OSX);
        }
    }
}
