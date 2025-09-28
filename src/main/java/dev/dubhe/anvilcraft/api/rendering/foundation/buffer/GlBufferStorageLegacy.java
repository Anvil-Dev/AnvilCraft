package dev.dubhe.anvilcraft.api.rendering.foundation.buffer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;

import static org.lwjgl.opengl.GL45.*;

public abstract class GlBufferStorageLegacy<C> extends GlBufferStorage<C> {

    protected GlBufferStorageLegacy(int target,C configureContext) {
        super(target, configureContext);
        bind();
        this.setupBufferState(configureContext);
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
        if (RenderSystem.isOnRenderThread()) {
            nglBufferData(this.target, size, ptr, GL_STATIC_DRAW);
            return;
        }
        RenderSystem.recordRenderCall(() -> {
                nglBufferData(this.target, size, ptr, GL_STATIC_DRAW);
            }
        );
    }
}
