package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.index;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorage;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorageLegacy;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.GlBufferStorageModern;
import org.lwjgl.opengl.GL45;

public class GlIndexBufferStorage {
    public static GlBufferStorage create(){
        if (GlBufferStorage.BUFFER_STORAGE_SUPPORT){
            return new Modern();
        }
        return new Legacy();
    }

    public static class Legacy extends GlBufferStorageLegacy {

        public Legacy() {
            super(GL45.GL_ELEMENT_ARRAY_BUFFER);
        }

        @Override
        public void setupBufferState() {
        }
    }

    public static class Modern extends GlBufferStorageModern {
        public Modern() {
            super(GL45.GL_ELEMENT_ARRAY_BUFFER);
        }

        @Override
        public void setupBufferState() {
        }
    }
}
