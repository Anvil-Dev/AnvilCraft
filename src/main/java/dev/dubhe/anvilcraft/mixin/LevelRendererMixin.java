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
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
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

        PostPass pass = passes.get(0);

        AnvilCraftClientConfig.LensingShape shape = AnvilCraftClient.CONFIG.lensingShape;

        // Collect visible black holes with optional polygon hull data
        java.util.List<GravitationalLensManager.HoleProjection> holes =
            GravitationalLensManager.collectVisibleBlackHoles(
                camera, projectionMatrix, 4, shape,
                AnvilCraftClient.CONFIG.cubicPolygonScale);

        int count = Math.min(holes.size(), 4);

        // Set dynamic black hole position and distance uniforms
        for (int i = 0; i < 4; i++) {
            String xName = "BlackHole" + (i + 1) + "X";
            String yName = "BlackHole" + (i + 1) + "Y";
            String dName = "BlackHole" + (i + 1) + "Dist";
            if (i < count) {
                GravitationalLensManager.HoleProjection h = holes.get(i);
                pass.getEffect().safeGetUniform(xName).set(h.centerU);
                pass.getEffect().safeGetUniform(yName).set(h.centerV);
                pass.getEffect().safeGetUniform(dName).set(h.cameraDistance);
            } else {
                pass.getEffect().safeGetUniform(xName).set(0.0f);
                pass.getEffect().safeGetUniform(yName).set(0.0f);
                pass.getEffect().safeGetUniform(dName).set(1.0f);
            }
        }

        // Set polygon vertex data (tightly packed across all holes)
        int globalVertIdx = 0;
        for (int i = 0; i < 4; i++) {
            pass.getEffect().safeGetUniform("PolyStart[" + i + "]").set((float) globalVertIdx);
            if (i < count && shape == AnvilCraftClientConfig.LensingShape.CUBIC) {
                GravitationalLensManager.HoleProjection h = holes.get(i);
                int vc = h.polyVertCount;
                pass.getEffect().safeGetUniform("PolyCount[" + i + "]").set((float) vc);
                for (int j = 0; j < vc && globalVertIdx < 24; j++) {
                    String uName = "PolyVerts[" + globalVertIdx + "]";
                    pass.getEffect().safeGetUniform(uName).set(h.polyU[j], h.polyV[j]);
                    globalVertIdx++;
                }
            } else {
                pass.getEffect().safeGetUniform("PolyCount[" + i + "]").set(0.0f);
            }
        }
        // Zero out remaining poly verts
        for (int k = globalVertIdx; k < 24; k++) {
            pass.getEffect().safeGetUniform("PolyVerts[" + k + "]").set(0.0f, 0.0f);
        }

        // Cubic mode: cubicEventHorizonRadius acts as a multiplier on the UV-based radius
        float horizonRadius = (float) (AnvilCraftClient.CONFIG.eventHorizonRadius
            * (shape == AnvilCraftClientConfig.LensingShape.CUBIC
                ? AnvilCraftClient.CONFIG.cubicEventHorizonRadius : 1.0));

        pass.getEffect().safeGetUniform("BlackHoleCount").set((float) count);
        pass.getEffect().safeGetUniform("LensStrength")
            .set((float) AnvilCraftClient.CONFIG.lensStrength);
        pass.getEffect().safeGetUniform("EventHorizonRadius").set(horizonRadius);
        pass.getEffect().safeGetUniform("LensingShape")
            .set(shape == AnvilCraftClientConfig.LensingShape.CUBIC ? 1.0f : 0.0f);
        pass.getEffect().safeGetUniform("PerspectiveScale")
            .set((float) AnvilCraftClient.CONFIG.lensPerspectiveScale);

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
