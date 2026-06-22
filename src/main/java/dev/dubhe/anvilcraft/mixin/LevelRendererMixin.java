package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTargets;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.support.GravitationalLensManager;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBuffers;crumblingBufferSource()"
                     + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
            ordinal = 2
        )
    )
    void renderBEBeforeTerrain(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci,
        @Local(index = 24) PoseStack poseStack,
        @Local(index = 25) MultiBufferSource.BufferSource bufferSource
    ) {
        if (RenderState.isEnhancedRenderingAvailable()) {
            CacheableBERenderingPipeline.getInstance().render(frustumMatrix, projectionMatrix);
        }
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;compileSections(Lnet/minecraft/client/Camera;)V"
        )
    )
    void uploadBuffers(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        CacheableBERenderingPipeline.getInstance().runTasks();
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug("
                     + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                     + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                     + "Lnet/minecraft/client/Camera;"
                     + ")V"
        )
    )
    void bloomPostProcess(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci,
        @Local(index = 24) PoseStack poseStack,
        @Local(index = 25) MultiBufferSource.BufferSource bufferSource
    ) {
        if (!RenderState.isEnhancedRenderingAvailable()) return;
        if (!RenderState.isBloomEffectEnabled()) return;
        if (ModRenderTargets.getBloomTarget() != null) {
            ModRenderTargets.getBloomTarget().copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        }
        PowerGridSupport.renderEnhancedTransmitterLine(
            poseStack,
            bufferSource,
            camera.getPosition()
        );
        CacheableBERenderingPipeline.getInstance().renderBloomed(frustumMatrix, projectionMatrix);
        RenderTarget mcInput = ModShaders.getBloomChain().getTempTarget("mcinput");
        mcInput.setClearColor(0, 0, 0, 0);
        mcInput.clear(Minecraft.ON_OSX);
        final int oldTexture = GlStateManager._getActiveTexture();
        if (ModRenderTargets.getTempTarget() != null) {
            ModRenderTargets.getTempTarget().copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        }
        ModShaders.getBloomChain().process(RenderSupport.getPartialTick());
        RenderSystem.clearColor(
            FogRenderer.fogRed,
            FogRenderer.fogGreen,
            FogRenderer.fogBlue,
            0f
        );
        RenderTarget result = ModShaders.getBloomChain().getTempTarget("result");
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        result.unbindRead();
        float width = main.width;
        float height = main.height;
        ShaderInstance blitShader = ModShaders.getBlitShader();
        RenderSystem.viewport(0, 0, (int) width, (int) height);
        blitShader.setSampler("DiffuseSampler", result);
        blitShader.safeGetUniform("ProjMat").set(ModShaders.getOrthoMatrix());
        blitShader.safeGetUniform("OutSize").set(width, height);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(0.0F, 0.0F, 500.0F);
        bufferbuilder.addVertex(width, 0.0F, 500.0F);
        bufferbuilder.addVertex(width, height, 500.0F);
        bufferbuilder.addVertex(0.0F, height, 500.0F);
        blitShader.apply();
        main.bindWrite(false);
        BufferUploader.draw(bufferbuilder.buildOrThrow());
        main.unbindWrite();
        result.unbindRead();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        ProgramManager.glUseProgram(0);
        Minecraft.getInstance().getMainRenderTarget().copyDepthFrom(ModRenderTargets.getTempTarget());
        RenderSystem.activeTexture(oldTexture);
        RenderSystem.enableDepthTest();
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    @Inject(
        method = "renderLevel",
        at = @At("TAIL")
    )
    void gravitationalLensPostProcess(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        if (!RenderState.isLensEffectEnabled()) return;
        if (ModShaders.getLensChain() == null) return;

        PostChain lensChain = ModShaders.getLensChain();
        java.util.List<PostPass> passes = ((PostChainAccessor) lensChain).getPasses();
        if (passes.isEmpty()) return;

        // Collect visible holes: black holes use positive direction, white holes negative
        float dir = (float) AnvilCraftClient.CONFIG.gravitationalLens.lensDirection;
        int maxCount = AnvilCraftClient.CONFIG.gravitationalLens.maxHoleCount;
        java.util.List<GravitationalLensManager.HoleProjection> holes =
            GravitationalLensManager.collectVisibleHoles(camera, projectionMatrix, maxCount, dir, -dir);

        int count = Math.min(holes.size(), maxCount);

        PostPass pass = passes.getFirst();

        GravitationalLensManager.uploadLensUbo(holes, count, pass.getEffect().getId());

        pass.getEffect().safeGetUniform("BlackHoleCount").set((float) count);
        pass.getEffect().safeGetUniform("LensStrength")
            .set((float) AnvilCraftClient.CONFIG.gravitationalLens.lensStrength);
        pass.getEffect().safeGetUniform("EventHorizonRadius")
            .set((float) AnvilCraftClient.CONFIG.gravitationalLens.eventHorizonRadius);
        pass.getEffect().safeGetUniform("PerspectiveScale")
            .set((float) AnvilCraftClient.CONFIG.gravitationalLens.lensPerspectiveScale);

        // Run the lens post chain (reads main target, writes to result target)
        lensChain.process(RenderSupport.getPartialTick());

        // Blit result back to the main render target
        RenderTarget result = lensChain.getTempTarget("result");
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

        float width = main.width;
        float height = main.height;

        ShaderInstance blitShader = ModShaders.getBlitShader();
        RenderSystem.viewport(0, 0, (int) width, (int) height);
        blitShader.setSampler("DiffuseSampler", result);
        blitShader.safeGetUniform("ProjMat").set(ModShaders.getOrthoMatrix());
        blitShader.safeGetUniform("OutSize").set(width, height);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        BufferBuilder bufferbuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(0.0F, 0.0F, 500.0F);
        bufferbuilder.addVertex(width, 0.0F, 500.0F);
        bufferbuilder.addVertex(width, height, 500.0F);
        bufferbuilder.addVertex(0.0F, height, 500.0F);

        blitShader.apply();
        main.bindWrite(false);
        BufferUploader.draw(bufferbuilder.buildOrThrow());
        main.unbindWrite();
        result.unbindRead();

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableDepthTest();
        main.bindWrite(false);
    }
}
