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
import dev.dubhe.anvilcraft.client.init.ModRenderTargets;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.util.RenderHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    protected abstract void renderSectionLayer(RenderType renderType, double x, double y, double z, Matrix4f frustrumMatrix, Matrix4f projectionMatrix);

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            ordinal = 5,
            shift = At.Shift.AFTER
        )
    )
    void renderLayer(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        anvilcraft$renderLaser(deltaTracker, renderBlockOutline, camera, gameRenderer, lightTexture, frustumMatrix, projectionMatrix);
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            ordinal = 3,
            shift = At.Shift.AFTER
        )
    )
    void renderLayer1(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        anvilcraft$renderLaser(deltaTracker, renderBlockOutline, camera, gameRenderer, lightTexture, frustumMatrix, projectionMatrix);
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/Options;getCloudsType()Lnet/minecraft/client/CloudStatus;"
        )
    )
    void renderEnhancedTransmitterLines(
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
            PowerGridRenderer.renderEnhancedTransmitterLine(
                poseStack,
                bufferSource,
                camera.getPosition()
            );
        }
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"
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
        CallbackInfo ci
    ) {
        if (!RenderState.isEnhancedRenderingAvailable()) return;
        if (!RenderState.isBloomEffectEnabled()) return;

        RenderTarget mcInput = ModShaders.getBloomChain().getTempTarget("mcinput");
        mcInput.setClearColor(0, 0, 0, 0);
        mcInput.clear(Minecraft.ON_OSX);
        int oldTexture = GlStateManager._getActiveTexture();
        ModRenderTargets.getTempTarget().copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        ModShaders.getBloomChain().process(RenderHelper.getPartialTick());
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

    @Unique
    private void anvilcraft$renderLaser(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix
    ) {
        if (!RenderState.isEnhancedRenderingAvailable()) return;
        Vec3 vec3 = camera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        if (ModRenderTargets.getBloomTarget() != null && RenderState.isBloomEffectEnabled()) {
            ModRenderTargets.getBloomTarget().setClearColor(0, 0, 0, 0);
            ModRenderTargets.getBloomTarget().clear(Minecraft.ON_OSX);
            ModRenderTargets.getBloomTarget().copyDepthFrom(this.minecraft.getMainRenderTarget());
        }

        RenderState.levelStage();
        this.renderSectionLayer(ModRenderTypes.LASER, d0, d1, d2, frustumMatrix, projectionMatrix);
        if (!RenderState.isBloomEffectEnabled()) return;
        RenderState.bloomStage();
        this.renderSectionLayer(ModRenderTypes.LASER, d0, d1, d2, frustumMatrix, projectionMatrix);
    }


}
