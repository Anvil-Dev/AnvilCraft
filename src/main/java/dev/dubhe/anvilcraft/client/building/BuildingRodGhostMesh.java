package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

final class BuildingRodGhostMesh implements AutoCloseable {
    private final GpuBuffer vertices;
    private final int indexCount;

    BuildingRodGhostMesh(MeshData mesh) {
        try (mesh) {
            this.vertices = RenderSystem.getDevice().createBuffer(() -> "AnvilCraft building projection",
                GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            this.indexCount = mesh.drawState().indexCount();
        }
    }

    void draw(Matrix4f pose) {
        var type = BuildingRodRenderTypes.BLOCK_GHOST;
        var target = type.outputTarget().getRenderTarget();
        var color = RenderSystem.outputColorTextureOverride != null
            ? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
        var depth = RenderSystem.outputDepthTextureOverride != null
            ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView();
        var transforms = RenderSystem.getDynamicUniforms().writeTransform(pose, new Vector4f(1), new Vector3f(), new Matrix4f());
        var sequential = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indices = sequential.getBuffer(this.indexCount);
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "AnvilCraft building projection", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
            pass.setPipeline(type.pipeline());
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            type.state.getTextures().forEach((name, texture) -> pass.bindTexture(name, texture.textureView(), texture.sampler()));
            pass.setVertexBuffer(0, this.vertices);
            pass.setIndexBuffer(indices, sequential.type());
            pass.drawIndexed(0, 0, this.indexCount, 1);
        }
    }

    @Override
    public void close() {
        this.vertices.close();
    }
}
