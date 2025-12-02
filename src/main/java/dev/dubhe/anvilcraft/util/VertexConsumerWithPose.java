package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;

public class VertexConsumerWithPose implements VertexConsumer {
    private final VertexConsumer parent;
    private final PoseStack.Pose pose;
    private final BlockPos originPos;

    public VertexConsumerWithPose(VertexConsumer parent, PoseStack.Pose pose, BlockPos originPos) {
        this.parent = parent;
        this.pose = pose;
        this.originPos = originPos;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        float dx = originPos.getX() & 15;
        float dy = originPos.getY() & 15;
        float dz = originPos.getZ() & 15;
        return parent.addVertex(pose, x - dx, y - dy, z - dz);
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return parent.setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return parent.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return parent.setUv1(u, v);
    }

    @Override
    public VertexConsumer setOverlay(int uv) {
        return parent.setUv1(uv & 65535, uv >> 16 & 65535);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return parent.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return parent.setNormal(pose, x, y, z);
    }

}