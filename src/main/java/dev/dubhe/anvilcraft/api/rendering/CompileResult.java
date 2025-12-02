package dev.dubhe.anvilcraft.api.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.EqualsAndHashCode;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;

@EqualsAndHashCode
final class CompileResult {
    private static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
    private final RenderType renderType;
    private final int vertexCount;
    private final int vertexSize;
    private final long vertexBufferPtr;
    final int indexCount;
    private boolean freed = false;

    CompileResult(
        RenderType renderType,
        int vertexCount,
        int vertexSize,
        long vertexBufferPtr,
        int indexCount
    ) {
        this.renderType = renderType;
        this.vertexCount = vertexCount;
        this.vertexSize = vertexSize;
        this.vertexBufferPtr = vertexBufferPtr;
        this.indexCount = indexCount;
    }

    void upload(VertexBuffer vertexBuffer) {
        if (freed) return;
        final VertexFormat.Mode mode = renderType.mode;
        vertexBuffer.bind();
        if (vertexBuffer.format != null) {
            vertexBuffer.format.clearBufferState();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer.vertexBufferId);
        renderType.format.setupBufferState();
        vertexBuffer.format = renderType.format;
        GL15C.nglBufferData(GL15.GL_ARRAY_BUFFER, (long) vertexCount * vertexSize, vertexBufferPtr, GL15.GL_STATIC_DRAW);
        RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(mode);
        if (indexBuffer != vertexBuffer.sequentialIndices || !indexBuffer.hasStorage(indexCount)) {
            indexBuffer.bind(indexCount);
        }
        vertexBuffer.sequentialIndices = indexBuffer;
        VertexBuffer.unbind();
    }

    void free() {
        if (freed) return;
        ALLOCATOR.free(vertexBufferPtr);
        freed = true;
    }
}
