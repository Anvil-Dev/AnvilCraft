package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.renderer.MunSodiumShaders;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.gl.shader.ShaderLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ShaderLoader.class, remap = false)
abstract class EmbMunShaderLoaderMixin {
    @ModifyReturnValue(method = "getShaderSource", at = @At("RETURN"))
    private static String anvilcraft$munTerrainShader(String original, ResourceLocation name) {
        return MunSodiumShaders.patch(original, name);
    }
}
