package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public record QuadSortingState(Vector3f[] quadCenters) {
    public static QuadSortingState fromMesh(MeshData meshData) {
        ByteBuffer byteBuffer = meshData.vertexBuffer();
        int vertexCount = meshData.drawState().vertexCount();
        VertexFormat format = meshData.drawState().format();
        int offset = format.getOffset(VertexFormatElement.POSITION);
        if (offset == -1) {
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");
        } else {
            FloatBuffer floatbuffer = byteBuffer.asFloatBuffer();
            int j = format.getVertexSize() / 4;
            int vertexSize = j * 4;
            int quadCount = vertexCount / 4;
            Vector3f[] avector3f = new Vector3f[quadCount];

            for (int i = 0; i < quadCount; i++) {
                int j1 = i * vertexSize + offset;
                int k1 = j1 + j * 2;
                float f = floatbuffer.get(j1 + 0);
                float f1 = floatbuffer.get(j1 + 1);
                float f2 = floatbuffer.get(j1 + 2);
                float f3 = floatbuffer.get(k1 + 0);
                float f4 = floatbuffer.get(k1 + 1);
                float f5 = floatbuffer.get(k1 + 2);
                avector3f[i] = new Vector3f((f + f3) / 2.0F, (f1 + f4) / 2.0F, (f2 + f5) / 2.0F);
            }

            return new QuadSortingState(avector3f);
        }
    }
}
