package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.client.renderer.MunRenderPipeline;
import dev.dubhe.anvilcraft.client.renderer.MunSurfaceRenderer;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = ShaderChunkRenderer.class, remap = false)
abstract class SodiumMunShaderRendererMixin {
    @Shadow
    @Final
    private Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programs;
    @Unique
    private boolean anvilcraft$munPrograms;

    @WrapMethod(method = "compileProgram")
    private GlProgram<ChunkShaderInterface> anvilcraft$recoverMunProgram(
        ChunkShaderOptions options, Operation<GlProgram<ChunkShaderInterface>> original
    ) {
        boolean lunar = MunRenderPipeline.requested();
        if (this.anvilcraft$munPrograms != lunar) {
            this.programs.values().forEach(GlProgram::delete);
            this.programs.clear();
            this.anvilcraft$munPrograms = lunar;
        }
        try {
            return original.call(options);
        } catch (RuntimeException exception) {
            if (!lunar) throw exception;
            MunRenderPipeline.fail();
            this.programs.values().forEach(GlProgram::delete);
            this.programs.clear();
            this.anvilcraft$munPrograms = false;
            return original.call(options);
        }
    }

    @Inject(method = "begin", at = @At("RETURN"))
    private void anvilcraft$munTerrainUniforms(TerrainRenderPass pass, CallbackInfo ci) {
        MunSurfaceRenderer.setupSodiumUniforms();
    }
}
