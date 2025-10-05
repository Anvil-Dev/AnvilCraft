package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.index;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorage;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlQuadIndexBuffer implements GlIndexBuffer {
    private final Logger logger = LoggerFactory.getLogger("GlQuadIndexBuffer");
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
        logger.info("{}: {} -> {}", this, index, value);
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


    @Override
    public void dispose() {
        if (ptr != 0) {
            alloc.free(ptr);
            ptr = 0;
        }
        backedBuffer.dispose();
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    @Override
    public void fillContents(int vertexCount, int indexCount) {
        if (hasEnoughStorage(indexCount)) return;
        ensureStorage(indexCount);
        int quadCount = vertexCount / 4;
        for (int i = 0, j = 0, k = 0; i < quadCount; i++, k += 4) {
            write(j++, k + 0);
            write(j++, k + 1);
            write(j++, k + 2);
            write(j++, k + 2);
            write(j++, k + 3);
            write(j++, k + 0);
        }
        backedBuffer.bind();
        backedBuffer.upload(ptr, (long) indexCount * indexType.bytes);
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
