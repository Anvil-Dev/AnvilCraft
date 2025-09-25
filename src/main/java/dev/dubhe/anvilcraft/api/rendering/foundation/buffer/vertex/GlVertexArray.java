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

public class GlVertexArray implements Disposable {
    private final int arrayObjectId;
    private final GlBufferStorage vertexBuffer;
    private final GlIndexBuffer indexBuffer;
    private final RenderType renderType;
    private final VertexFormat.IndexType indexType;
    private QuadSortingState sortingState;

    public GlVertexArray(RenderType renderType) {
        this(renderType, VertexFormat.IndexType.SHORT);
    }

    public GlVertexArray(RenderType renderType, VertexFormat.IndexType indexType) {
        this.indexType = indexType;
        if (renderType.mode != VertexFormat.Mode.QUADS) throw new UnsupportedOperationException();
        this.indexBuffer = GlIndexBuffer.forQuad(indexType);
        this.renderType = renderType;
        this.arrayObjectId = glGenVertexArrays();
        glBindVertexArray(arrayObjectId);
        this.vertexBuffer = GlVertexBufferStorage.create(renderType.format);
        glBindVertexArray(0);
    }

    public void upload(long ptr, int size, int indexCount) {
        this.upload(ptr, size, indexCount, null, null);
    }

    public void upload(long ptr, int size, int indexCount, @Nullable QuadSortingState sortingState, @Nullable Vector3f origin) {
        this.sortingState = sortingState;
        if (sortingState == null) {
            indexBuffer.fillContents(indexCount);
        } else {
            resortVerticles(origin);
        }
        this.vertexBuffer.upload(ptr, size);
    }

    public void resortVerticles(Vector3f origin) {
        int[] indexes = QuadSorter.buildSortedIndexByDistance(sortingState, origin);
        indexBuffer.fromSorted(indexes);
    }

    @Override
    public void dispose() {
        vertexBuffer.dispose();
        indexBuffer.dispose();
        glDeleteVertexArrays(arrayObjectId);
    }
}
