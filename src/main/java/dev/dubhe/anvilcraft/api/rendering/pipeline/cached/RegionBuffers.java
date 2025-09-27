package dev.dubhe.anvilcraft.api.rendering.pipeline.cached;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;
import dev.dubhe.anvilcraft.api.rendering.foundation.FullyBufferedBufferSource;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.ring.RingBufferElement;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.GlVertexBuffer;
import dev.dubhe.anvilcraft.api.rendering.util.SyncSupport;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RegionBuffers implements RingBufferElement<RenderRegion>, FullyBufferedBufferSource.CompileContext {
    private final SyncSupport syncSupport = new SyncSupport();
    private final Map<RenderType, GlVertexBuffer> vertexBuffers = new ConcurrentHashMap<>();
    private final RenderRegion renderRegion;

    public RegionBuffers(RenderRegion renderRegion) {
        this.renderRegion = renderRegion;
    }

    @Override
    public void waitForReady() {
        if (!syncSupport.isSyncSet()){
            return;
        }
        if (!syncSupport.isSyncSignaled()){
            syncSupport.waitSync();
        }
        syncSupport.deleteSync();
        syncSupport.reset();
    }

    @Override
    public void setup() {
        syncSupport.setSync();
    }

    @Override
    public void dispose() {
        for (GlVertexBuffer value : vertexBuffers.values()) {
            value.dispose();
        }
    }

    @Override
    public GlVertexBuffer getOrCreateBuffer(RenderType renderType, VertexFormat.IndexType indexType) {
        GlVertexBuffer buffer = vertexBuffers.get(renderType);
        if (buffer != null) {
            return buffer;
        }
        buffer = new GlVertexBuffer(renderType, indexType);
        vertexBuffers.put(renderType, buffer);
        return buffer;
    }

    @Override
    public void submitUploadTask(Runnable runnable) {
        renderRegion.getPipeline().submitUploadTask(runnable);
    }

    @Nullable
    public GlVertexBuffer getBuffer(RenderType renderType) {
        return vertexBuffers.get(renderType);
    }

    public Collection<RenderType> allRenderPasses() {
        return vertexBuffers.keySet();
    }
}
