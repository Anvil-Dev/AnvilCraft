package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 透明顶点消费者
 *
 * @param delegate 原始顶点消费者（委托对象）
 * @param alpha    目标半透明度（0-255，50% 对应 128）
 */
public record TranslucentVertexConsumer(VertexConsumer delegate, int alpha) implements VertexConsumer {
    @Override
    public VertexConsumer addVertex(float v, float v1, float v2) {
        return this.delegate.addVertex(v, v1, v2);
    }

    @Override
    public VertexConsumer setColor(int i, int i1, int i2, int i3) {
        return this.delegate.setColor(i, i1, i2, this.alpha);
    }

    @Override
    public VertexConsumer setColor(int color) {
        return this.delegate.setColor((this.alpha & 0xFF) << 24 | color & 0xFFFFFF);
    }

    @Override
    public VertexConsumer setUv(float v, float v1) {
        return this.delegate.setUv(v, v1);
    }

    @Override
    public VertexConsumer setUv1(int i, int i1) {
        return this.delegate.setUv1(i, i1);
    }

    @Override
    public VertexConsumer setUv2(int i, int i1) {
        return this.delegate.setUv2(i, i1);
    }

    @Override
    public VertexConsumer setNormal(float v, float v1, float v2) {
        return this.delegate.setNormal(v, v1, v2);
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return this.delegate.setLineWidth(width);
    }
}
