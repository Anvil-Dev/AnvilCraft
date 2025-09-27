package dev.dubhe.anvilcraft.api.rendering.foundation.buffer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;

import static org.lwjgl.opengl.GL45.*;

public abstract class GlBufferStorageLegacy extends GlBufferStorage {

    protected GlBufferStorageLegacy(int target) {
        super(target);
        bind();
        this.setupBufferState();
        unbind();
    }

    @Override
    public void upload(long ptr, long size, Disposable disposable) {
        if (RenderSystem.isOnRenderThread()) {
            nglBufferData(this.target, size, ptr, GL_STATIC_DRAW);
            disposable.dispose();
            return;
        }
        RenderSystem.recordRenderCall(() -> {
                nglBufferData(this.target, size, ptr, GL_STATIC_DRAW);
                disposable.dispose();
            }
        );
    }

    public void upload(long ptr, long size) {

    }
}
