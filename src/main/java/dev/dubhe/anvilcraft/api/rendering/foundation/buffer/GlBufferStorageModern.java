package dev.dubhe.anvilcraft.api.rendering.foundation.buffer;

import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL45.*;

public abstract class GlBufferStorageModern extends GlBufferStorage {
    public static final long BUFFER_SIZE = RenderType.SMALL_BUFFER_SIZE;
    public static final int FLAGS = ARBBufferStorage.GL_MAP_PERSISTENT_BIT
        | ARBBufferStorage.GL_MAP_COHERENT_BIT
        | GL_MAP_WRITE_BIT
        | GL_DYNAMIC_STORAGE_BIT;

    private long clientPtr;

    protected GlBufferStorageModern(int target) {
        super(target);
        bind();
        this.setupBufferState();
        ARBBufferStorage.glBufferStorage(
            target,
            BUFFER_SIZE,
            FLAGS
        );
        clientPtr = nglMapBufferRange(target, 0, BUFFER_SIZE, FLAGS);
        unbind();
    }

    @Override
    public void dispose() {
        if (!valid)return;
        bind();
        glUnmapBuffer(target);
        clientPtr = 0;
        super.dispose();
    }

    @Override
    public void upload(long ptr, long size) {
        MemoryUtil.memCopy(ptr, clientPtr, size);
    }
}
