package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.index;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;

public interface GlIndexBuffer extends Disposable {
    /**
     * Called on worker threads
     */
    void fillContents(int vertexCount, int indexCount);

    /**
     * Called on worker threads=
     */
    void fromSorted(int[] sortedIndex);

    void bind();

    void unbind();

    int getIndexCount();

    static GlIndexBuffer forQuad(VertexFormat.IndexType indexType){
        return new GlQuadIndexBuffer(indexType);
    }
}
