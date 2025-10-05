package dev.dubhe.anvilcraft.api.rendering.foundation;

import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.GlVertexBuffer;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.QuadSortingState;
import lombok.EqualsAndHashCode;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

@EqualsAndHashCode
public final class CompileResult implements Disposable {
    private static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
    private final RenderType renderType;
    private final int vertexCount;
    private final int vertexSize;
    private final long vertexBufferPtr;
    private final int indexCount;
    @Nullable
    private final QuadSortingState sortingState;
    private boolean freed = false;

    public CompileResult(
        RenderType renderType,
        int vertexCount,
        int vertexSize,
        long vertexBufferPtr,
        int indexCount,
        @Nullable QuadSortingState sortingState
    ) {
        this.renderType = renderType;
        this.vertexCount = vertexCount;
        this.vertexSize = vertexSize;
        this.vertexBufferPtr = vertexBufferPtr;
        this.indexCount = indexCount;
        this.sortingState = sortingState;
    }

    public void upload(GlVertexBuffer vertexBuffer) {
        vertexBuffer.upload(vertexBufferPtr, vertexCount * vertexSize, vertexCount, indexCount, sortingState, new Vector3f(), this);
    }

    public void dispose() {
        if (freed) return;
        ALLOCATOR.free(vertexBufferPtr);
        freed = true;
    }
}
