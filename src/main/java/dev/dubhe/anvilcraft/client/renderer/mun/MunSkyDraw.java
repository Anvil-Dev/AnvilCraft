package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;
import java.util.OptionalInt;
import javax.annotation.Nullable;

/** Reuses native GPU buffers without changing global projection, blend or depth state. */
final class MunSkyDraw implements AutoCloseable {
    private final GpuBuffer sky = RenderSystem.getDevice().createBuffer(() -> "Mun sky uniforms",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, 320);
    private final GpuBuffer projection = RenderSystem.getDevice().createBuffer(() -> "Mun sky projection",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, 64);
    private GpuBuffer vertices = RenderSystem.getDevice().createBuffer(() -> "Mun sky vertices",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX, 65536);
    private int vertexCapacity = 65536;

    void upload(MunSkyRenderer.Frame frame, Matrix4fc view, Matrix4fc projection) {
        try (var stack = MemoryStack.stackPush()) {
            var data = stack.malloc(320);
            new Matrix4f(projection).invert().get(0, data);
            new Matrix4f(view).invert().get(64, data);
            frame.skyRotation().get(128, data);
            frame.earthRotation().get(192, data);
            data.putFloat(256, (float) frame.earth().x()).putFloat(260, (float) frame.earth().y())
                .putFloat(264, (float) frame.earth().z()).putFloat(268, (float) MunSkyMath.EARTH_HALF_SIZE);
            data.putFloat(272, (float) frame.sun().x()).putFloat(276, (float) frame.sun().y())
                .putFloat(280, (float) frame.sun().z()).putFloat(284, frame.daylight());
            data.putFloat(288, (float) MunSkyMath.EARTH_PERSPECTIVE)
                .putFloat(292, (float) MunSkyMath.EARTH_ATMOSPHERE_THICKNESS).putFloat(296, 0).putFloat(300, 0);
            frame.sunUv().get(304, data);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.sky.slice(), data);
        }
    }

    void draw(MeshData mesh, RenderPipeline pipeline, @Nullable Identifier texture, Matrix4fc view, Matrix4fc projection) {
        try (mesh; var stack = MemoryStack.stackPush()) {
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            int size = mesh.vertexBuffer().remaining();
            if (size > this.vertexCapacity) {
                this.vertices.close();
                this.vertexCapacity = size;
                this.vertices = RenderSystem.getDevice().createBuffer(() -> "Mun sky vertices",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX, size);
            }
            encoder.writeToBuffer(this.vertices.slice(0, size), mesh.vertexBuffer());
            @Nullable GpuBufferSlice transforms = null;
            if (pipeline != MunSkyPipelines.STANDARD) {
                var projectionData = stack.malloc(64);
                projection.get(0, projectionData);
                encoder.writeToBuffer(this.projection.slice(), projectionData);
                transforms = RenderSystem.getDynamicUniforms().writeTransform(view, new Vector4f(1), new Vector3f(), new Matrix4f());
            }
            var client = Minecraft.getInstance();
            var color = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride
                : Objects.requireNonNull(client.getMainRenderTarget().getColorTextureView());
            var indices = RenderSystem.getSequentialBuffer(mesh.drawState().mode());
            var indexBuffer = indices.getBuffer(mesh.drawState().indexCount());
            var first = pipeline == MunSkyPipelines.STANDARD
                ? client.getTextureManager().getTexture(MunSkyRenderer.EARTH_TEXTURE)
                : texture == null ? null : client.getTextureManager().getTexture(texture);
            var second = pipeline == MunSkyPipelines.STANDARD
                ? client.getTextureManager().getTexture(Sheets.BLOCKS_MAPPER.sheet()) : null;
            try (var pass = encoder.createRenderPass(() -> "Mun sky", color, OptionalInt.empty())) {
                pass.setPipeline(pipeline);
                if (pipeline == MunSkyPipelines.STANDARD) {
                    pass.setUniform("MunSky", this.sky);
                    if (first == null || second == null) throw new IllegalStateException("Missing Mun sky textures");
                    pass.bindTexture("Sampler0", first.getTextureView(), first.getSampler());
                    pass.bindTexture("Sampler1", second.getTextureView(), second.getSampler());
                } else {
                    pass.setUniform("Projection", this.projection);
                    pass.setUniform("DynamicTransforms", Objects.requireNonNull(transforms));
                    if (first != null) pass.bindTexture("Sampler0", first.getTextureView(), first.getSampler());
                }
                pass.setVertexBuffer(0, this.vertices);
                pass.setIndexBuffer(indexBuffer, indices.type());
                pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
            }
        }
    }

    @Override
    public void close() {
        this.sky.close();
        this.projection.close();
        this.vertices.close();
    }
}
