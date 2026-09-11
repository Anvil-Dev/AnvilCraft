package dev.dubhe.anvilcraft.client.renderer;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** 光线已转为世界位置相关的竖直投影，贴图只在固定 X/Z 网格上平移。 */
final class MunShadowProjection {
    static final float DEPTH = 1536;
    private final Matrix4f matrix = new Matrix4f();
    private final Vector3f right = new Vector3f(1, 0, 0);
    private final Vector3f up = new Vector3f(0, 0, -1);
    private double centerX;
    private double centerZ;
    private float span;
    private double texel;

    boolean update(Vec3 anchor, Vec3 origin, float span, int resolution) {
        double texel = span / resolution;
        this.texel = texel;
        // 固定亚像素相位，避免锁链等 45 度薄面的边线恰好穿过整行采样中心。
        double x = Math.rint(anchor.x / texel) * texel + texel / 256;
        double z = Math.rint(anchor.z / texel) * texel + texel / 128;
        final boolean moved = this.centerX != x || this.centerZ != z || this.span != span;
        this.centerX = x;
        this.centerZ = z;
        this.span = span;
        Matrix4f view = new Matrix4f().set(
            1, 0, 0, 0,
            0, 0, 1, 0,
            0, -1, 0, 0,
            (float) (origin.x - this.centerX), (float) (this.centerZ - origin.z),
            (float) (origin.y - MunSolarLighting.REFERENCE_HEIGHT - DEPTH / 2), 1
        );
        this.matrix.setOrtho(-span / 2, span / 2, -span / 2, span / 2, 0, DEPTH).mul(view);
        return moved;
    }

    Vector3f offset(Vec3 origin) {
        return new Vector3f((float) ((origin.x - this.centerX) * 2 / this.span),
            (float) ((this.centerZ - origin.z) * 2 / this.span),
            (float) ((MunSolarLighting.REFERENCE_HEIGHT - origin.y) * 2 / DEPTH));
    }

    boolean intersects(AABB projectedBounds) {
        double halfSpan = this.span / 2 + this.texel;
        return projectedBounds.maxX >= this.centerX - halfSpan && projectedBounds.minX <= this.centerX + halfSpan
            && projectedBounds.maxZ >= this.centerZ - halfSpan && projectedBounds.minZ <= this.centerZ + halfSpan
            && projectedBounds.maxY >= MunSolarLighting.REFERENCE_HEIGHT - DEPTH / 2
            && projectedBounds.minY <= MunSolarLighting.REFERENCE_HEIGHT + DEPTH / 2;
    }

    Matrix4f matrix() {
        return this.matrix;
    }

    Vector3f right() {
        return this.right;
    }

    Vector3f up() {
        return this.up;
    }
}
