package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorage;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorageLegacy;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorageModern;
import lombok.Getter;

public final class GlVertexBufferStorage {

    public static GlBufferStorage<VertexFormat> create(VertexFormat format) {
        if (GlBufferStorage.BUFFER_STORAGE_SUPPORT) {
            return new Modern(format);
        }
        return new Legacy(format);
    }

    public static class Legacy extends GlBufferStorageLegacy<VertexFormat> {
        @Getter
        private final VertexFormat format;

        public Legacy(VertexFormat format) {
            super(GlConst.GL_ARRAY_BUFFER, format);
            this.format = format;
        }

        @Override
        public void setupBufferState(VertexFormat vertexFormat) {
            vertexFormat.setupBufferState();
        }
    }

    public static class Modern extends GlBufferStorageModern<VertexFormat> {
        @Getter
        private final VertexFormat format;

        public Modern(VertexFormat format) {
            super(GlConst.GL_ARRAY_BUFFER, format);
            this.format = format;
        }

        @Override
        public void setupBufferState(VertexFormat vertexFormat) {
            vertexFormat.setupBufferState();
        }
    }
}
