package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

public final class FluidGeometryRecorder implements VertexConsumer {
    public record Vertex(float x, float y, float z, int color, int light, float nx, float ny, float nz) {
    }

    private final List<Vertex> vertices = new ArrayList<>();
    private boolean started;
    private float posX;
    private float posY;
    private float posZ;
    private int color;
    private int light;
    private float nx;
    private float ny;
    private float nz;

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.finish();
        this.started = true;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.color = alpha << 24 | red << 16 | green << 8 | blue;
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.light = u | v << 16;
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.nx = x;
        this.ny = y;
        this.nz = z;
        return this;
    }

    public VertexConsumer setLineWidth(float width) {
        return this;
    }

    private void finish() {
        if (!this.started) return;
        this.vertices.add(new Vertex(this.posX, this.posY, this.posZ, this.color, this.light, this.nx, this.ny, this.nz));
        this.started = false;
    }

    public List<Vertex> vertices() {
        this.finish();
        return List.copyOf(this.vertices);
    }
}
