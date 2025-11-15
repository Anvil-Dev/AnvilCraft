package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;
import dev.dubhe.anvilcraft.api.rendering.foundation.QuadSorter;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorage;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.index.GlIndexBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46;

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

    public void upload(long ptr, int size, int vertexCount, int indexCount, Disposable uploadSrc) {
        this.upload(ptr, size, vertexCount, indexCount, null, null, uploadSrc);
    }

    public void upload(long ptr, int size, int vertexCount, int indexCount, @Nullable QuadSortingState sortingState, @Nullable Vector3f origin, Disposable uploadSrc) {
        this.sortingState = sortingState;
//        if (sortingState == null) {
//            indexBuffer.fillContents(vertexCount);
//        } else {
//            resortVertices(origin);
//        }
        indexBuffer.fillContents(vertexCount, indexCount);
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

    public void bindIndexBuffer() {
        indexBuffer.bind();
    }

    public void drawElements(Vec3 cameraPosition) {
        if (renderType.sortOnUpload) {
            //this.resortVertices(cameraPosition.toVector3f());
        }
        GL46.glBindVertexArray(arrayObjectId);
        GL46.glDrawElements(GL46.GL_TRIANGLES, indexBuffer.getIndexCount(), indexType.asGLType, 0L);
        GL46.glBindVertexArray(0);
        RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).bind(6);
    }
}
