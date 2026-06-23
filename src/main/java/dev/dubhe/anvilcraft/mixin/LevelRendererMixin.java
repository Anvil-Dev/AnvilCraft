package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.support.GravitationalLensManager;
import dev.dubhe.anvilcraft.client.support.GravitationalLensManager.HoleProjection;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    private static final Identifier LENS_POST_CHAIN_ID = AnvilCraft.of("gravitational_lens");

    @Inject(
        method = "renderLevel",
        at = @At("TAIL")
    )
    void gravitationalLensPostProcess(
        GraphicsResourceAllocator resourceAllocator,
        DeltaTracker deltaTracker,
        boolean renderOutline,
        CameraRenderState cameraState,
        Matrix4fc modelViewMatrix,
        GpuBufferSlice terrainFog,
        Vector4f fogColor,
        boolean shouldRenderSky,
        ChunkSectionsToRender chunkSectionsToRender,
        CallbackInfo ci
    ) {
        if (!RenderState.isLensEffectEnabled()) return;

        PostChain lensChain = Minecraft.getInstance().getShaderManager()
            .getPostChain(LENS_POST_CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
        if (lensChain == null) return;

        float dir = (float) AnvilCraftClient.CONFIG.gravitationalLens.lensDirection;
        int maxCount = AnvilCraftClient.CONFIG.gravitationalLens.maxHoleCount;
        List<HoleProjection> holes = GravitationalLensManager.collectVisibleHoles(
            cameraState, cameraState.projectionMatrix, maxCount, dir, -dir
        );

        int count = Math.min(holes.size(), maxCount);

        GravitationalLensManager.uploadLensUbo(
            holes, count,
            (float) AnvilCraftClient.CONFIG.gravitationalLens.lensStrength,
            (float) AnvilCraftClient.CONFIG.gravitationalLens.eventHorizonRadius,
            (float) AnvilCraftClient.CONFIG.gravitationalLens.lensPerspectiveScale
        );

        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        lensChain.process(mainTarget, resourceAllocator);
        GravitationalLensManager.clearLensUboFlag();
    }
}
