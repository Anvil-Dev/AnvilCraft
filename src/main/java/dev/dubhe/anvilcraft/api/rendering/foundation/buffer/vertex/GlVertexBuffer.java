package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;
import dev.dubhe.anvilcraft.api.rendering.foundation.QuadSorter;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorage;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.index.GlIndexBuffer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL45.*;

public class GlVertexBuffer implements Disposable {
    private final int arrayObjectId;
    private final GlBufferStorage<VertexFormat> vertexBuffer;
    private final GlIndexBuffer indexBuffer;
    private final RenderType renderType;
    private final VertexFormat.IndexType indexType;
    private QuadSortingState sortingState;

    public GlVertexBuffer(RenderType renderType) {
        this(renderType, VertexFormat.IndexType.SHORT);
    }

    public GlVertexBuffer(RenderType renderType, VertexFormat.IndexType indexType) {
        if (renderType.mode != VertexFormat.Mode.QUADS) throw new UnsupportedOperationException();
        this.indexType = indexType;
        this.renderType = renderType;
        this.arrayObjectId = glGenVertexArrays();
        glBindVertexArray(arrayObjectId);
        this.vertexBuffer = GlVertexBufferStorage.create(renderType.format);
        this.indexBuffer = GlIndexBuffer.forQuad(indexType);
        glBindVertexArray(0);
    }

    public void upload(long ptr, int size, int indexCount, Disposable uploadSrc) {
        this.upload(ptr, size, indexCount, null, null, uploadSrc);
    }

    public void upload(long ptr, int size, int indexCount, @Nullable QuadSortingState sortingState, @Nullable Vector3f origin, Disposable uploadSrc) {
        this.sortingState = sortingState;
        if (sortingState == null) {
            indexBuffer.fillContents(indexCount);
        } else {
            resortVertices(origin);
        }
        this.vertexBuffer.upload(ptr, size, uploadSrc);
    }

    public void resortVertices(Vector3f origin) {
        int[] indexes = QuadSorter.buildSortedIndexByDistance(sortingState, origin);
        indexBuffer.fromSorted(indexes);
    }

    public void bind() {
        glBindVertexArray(arrayObjectId);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    @Override
    public void dispose() {
        vertexBuffer.dispose();
        indexBuffer.dispose();
        glDeleteVertexArrays(arrayObjectId);
    }

    public int getIndexType() {
        return indexType.asGLType;
    }
}
