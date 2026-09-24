package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.MeshData;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

/** Shared native-layout buffer for static casters and reusable dynamic geometry. */
final class MunShadowVertexBuffer implements AutoCloseable {
    private final boolean dynamic;
    private final int buffer;
    private final int vertexArray;
    private int count;
    private boolean closed;

    MunShadowVertexBuffer(boolean dynamic) {
        this.dynamic = dynamic;
        try (var ignored = new MunShadowGlScope()) {
            this.buffer = GlStateManager._glGenBuffers();
            this.vertexArray = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(this.vertexArray);
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.buffer);
            GL20C.glEnableVertexAttribArray(0);
            GL20C.glEnableVertexAttribArray(1);
            GL20C.glEnableVertexAttribArray(2);
            GL20C.glVertexAttribPointer(0, 3, GL11C.GL_FLOAT, false, 24, 0L);
            GL20C.glVertexAttribPointer(1, 2, GL11C.GL_FLOAT, false, 24, 12L);
            GL20C.glVertexAttribPointer(2, 4, GL11C.GL_UNSIGNED_BYTE, true, 24, 20L);
        }
    }

    void upload(MeshData mesh) {
        try (mesh; var ignored = new MunShadowGlScope()) {
            if (this.closed) throw new IllegalStateException("Shadow buffer is closed");
            this.count = mesh.drawState().vertexCount();
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.buffer);
            GlStateManager._glBufferData(GL15C.GL_ARRAY_BUFFER, mesh.vertexBuffer(),
                this.dynamic ? GL15C.GL_DYNAMIC_DRAW : GL15C.GL_STATIC_DRAW);
        }
    }

    void draw() {
        if (this.closed) throw new IllegalStateException("Shadow buffer is closed");
        GlStateManager._glBindVertexArray(this.vertexArray);
        GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, this.count);
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        GL30C.glDeleteVertexArrays(this.vertexArray);
        GlStateManager._glDeleteBuffers(this.buffer);
    }
}
