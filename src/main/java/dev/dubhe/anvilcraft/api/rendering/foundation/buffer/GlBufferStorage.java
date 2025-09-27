package dev.dubhe.anvilcraft.api.rendering.foundation.buffer;

import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;
import org.lwjgl.opengl.GL;

import static org.lwjgl.opengl.GL45.*;

public abstract class GlBufferStorage implements Disposable {
    public static final boolean BUFFER_STORAGE_SUPPORT = GL.getCapabilities().GL_ARB_buffer_storage;
    protected final int glBufferId;
    protected final int target;
    protected boolean valid = true;

    GlBufferStorage(int target) {
        this.target = target;
        this.glBufferId = glGenBuffers();
    }

    public abstract void setupBufferState();

    public void upload(long ptr, long size, Disposable uploadSrc) {
        this.upload(ptr, size);
        uploadSrc.dispose();
    }

    /**
     * Runs on worker thread
     */
    public abstract void upload(long ptr, long size);

    public void bind() {
        glBindBuffer(target, glBufferId);
    }

    @Override
    public void dispose() {
        if (!valid) return;
        bind();
        glDeleteBuffers(glBufferId);
        unbind();
        valid = false;
    }

    public void unbind() {
        glBindBuffer(target, 0);
    }
}
