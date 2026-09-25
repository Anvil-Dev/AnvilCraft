package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelRenderer.class)
public class MunWorldPassMixin {
    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @WrapMethod(method = "renderLevel")
    private void anvilcraft$moonWorld(
        GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, CameraRenderState camera,
        Matrix4fc view, GpuBufferSlice fog, Vector4f fogColor, boolean renderSky, ChunkSectionsToRender sections,
        Operation<Void> original
    ) {
        try {
            MunSurfaceRenderer.beginWorld(this.levelRenderState);
            original.call(allocator, delta, outline, camera, view, fog, fogColor, renderSky, sections);
        } finally {
            MunSurfaceRenderer.endWorld();
        }
    }
}
