package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.index;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorage;
import org.lwjgl.system.MemoryUtil;

public class GlQuadIndexBuffer implements GlIndexBuffer {
    private final MemoryUtil.MemoryAllocator alloc = MemoryUtil.getAllocator();
    private long ptr = alloc.calloc(1024, 4);
    private final VertexFormat.IndexType indexType;
    private final GlBufferStorage<?> backedBuffer;
    private int indexCount;

    protected GlQuadIndexBuffer(VertexFormat.IndexType indexType) {
        this.indexType = indexType;
        this.backedBuffer = GlIndexBufferStorage.create();
    }

    private void write(int index, int value) {
        if (indexType == VertexFormat.IndexType.SHORT) {
            MemoryUtil.memPutShort(ptr + index * 2L, (short) value);
            return;
        }
        if (indexType == VertexFormat.IndexType.INT) {
            MemoryUtil.memPutInt(ptr + index * 4L, value);
        }
    }

    private boolean hasEnoughStorage(int required) {
        return indexCount >= required;
    }

    private void ensureStorage(int required) {
        alloc.free(ptr);
        this.ptr = alloc.calloc(required, 4);
        this.indexCount = required;
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    @Override
    public void fillContents(int indexCount) {
        if (hasEnoughStorage(indexCount)) return;
        ensureStorage(indexCount);
        int quadCount = indexCount / 4;
        for (int i = 0, j = 0; i < quadCount; i++) {
            write(j++, i + 0);
            write(j++, i + 1);
            write(j++, i + 2);
            write(j++, i + 2);
            write(j++, i + 3);
            write(j++, i + 0);
        }
        backedBuffer.bind();
        backedBuffer.upload(ptr, (long) indexCount * indexType.bytes);
    }

    @Override
    public void dispose() {
        if (ptr != 0) {
            alloc.free(ptr);
            ptr = 0;
        }
        backedBuffer.dispose();
    }

    @Override
    public void fromSorted(int[] sortedIndex) {
        if (!hasEnoughStorage(sortedIndex.length)) {
            ensureStorage(sortedIndex.length);
        }
        for (int i = 0; i < sortedIndex.length; i++) {
            write(i, sortedIndex[i]);
        }
        backedBuffer.bind();
        backedBuffer.upload(ptr, (long) indexCount * indexType.bytes);
    }

    @Override
    public void bind() {
        backedBuffer.bind();
    }

    @Override
    public void unbind() {
        backedBuffer.unbind();
    }
}
