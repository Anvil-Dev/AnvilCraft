package dev.dubhe.anvilcraft.api.rendering.foundation.buffer;

import org.lwjgl.opengl.GL;

import static org.lwjgl.opengl.GL45.*;

public abstract class GlBufferStorage {
    public static final boolean BUFFER_STORAGE_SUPPORT = GL.getCapabilities().GL_ARB_buffer_storage;
    protected final int glBufferId;
    protected final int target;

    GlBufferStorage(int target) {
        this.target = target;
        this.glBufferId = glGenBuffers();
    }

    public abstract void setupBufferState();

    /**
     * Runs on worker thread
     */
    public abstract void upload(long ptr, long size);

    public void bind() {
        glBindBuffer(target, glBufferId);
    }

    public void unbind() {
        glBindBuffer(target, 0);
    }
}
