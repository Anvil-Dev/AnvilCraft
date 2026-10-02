package dev.dubhe.anvilcraft.client.renderer.mun;

import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.List;

/** 同一世界位置共享同一太阳光线，玩家坐标只参与浮点原点和阴影窗口选择。 */
final class MunSolarLighting {
    static final double REFERENCE_HEIGHT = 64;
    static final double MIN_SOLAR_HEIGHT = 0.025;
    private final double[] horizon = new double[16];
    private MunSkyMath.Vector reference = MunSkyMath.UP;
    private List<MunSkyMath.Vector> eclipse = List.of();
    private float eclipseCoverage;
    private Vec3 origin = Vec3.ZERO;
    private long shadowRevision;
    private double directionGradient = 4 / MunSkyMath.NEAR_SIDE_HALF_SIZE;

    void update(long dayTime, double partialTick, Vec3 origin) {
        this.origin = origin;
        MunSkyMath.Vector reference = MunSkyMath.referenceSun(dayTime, partialTick);
        boolean changed = !this.reference.equals(reference);
        this.reference = reference;
        double horizonLength = 0;
        for (int index = 0; index < 8; index++) {
            MunSkyMath.Vector corner = MunSkyMath.atmosphereCorner(index, dayTime, partialTick);
            double x = corner.x() / corner.y();
            double z = corner.z() / corner.y();
            changed |= this.horizon[index * 2] != x || this.horizon[index * 2 + 1] != z;
            this.horizon[index * 2] = x;
            this.horizon[index * 2 + 1] = z;
            horizonLength = Math.max(horizonLength, Math.hypot(x, z));
        }
        this.directionGradient = 4 * (Math.sqrt(1 + horizonLength * horizonLength) + 2 * horizonLength)
            / MunSkyMath.NEAR_SIDE_HALF_SIZE;
        if (changed) this.shadowRevision++;
        this.eclipse = MunSkyMath.solarOcclusion(dayTime, partialTick);
        double area = 0;
        for (int index = 0; index < this.eclipse.size(); index++) {
            MunSkyMath.Vector a = this.eclipse.get(index);
            MunSkyMath.Vector b = this.eclipse.get((index + 1) % this.eclipse.size());
            area += a.x() * b.y() - a.y() * b.x();
        }
        this.eclipseCoverage = (float) (Math.abs(area) / (2 * MunSkyMath.SUN_DISC_AREA));
    }

    long shadowRevision() {
        return this.shadowRevision;
    }

    MunSkyMath.Vector direction(double x, double z) {
        double length = Math.hypot(x, z);
        if (length < 1.0E-9) return this.reference;
        double east = x / length;
        double south = z / length;
        double edge = 0;
        for (int index = 0; index < 8; index++) {
            edge = Math.max(edge, east * this.horizon[index * 2] + south * this.horizon[index * 2 + 1]);
        }
        double tangent = Math.max(Math.abs(x), Math.abs(z)) / MunSkyMath.NEAR_SIDE_HALF_SIZE * (Math.sqrt(1 + edge * edge) + edge);
        double cosine = (1 - tangent * tangent) / (1 + tangent * tangent);
        double sine = 2 * tangent / (1 + tangent * tangent);
        double along = east * this.reference.x() + south * this.reference.z();
        double horizontal = along * (cosine - 1) - this.reference.y() * sine;
        return new MunSkyMath.Vector(this.reference.x() + east * horizontal,
            this.reference.y() * cosine + along * sine, this.reference.z() + south * horizontal);
    }

    Vec3 project(Vec3 position) {
        MunSkyMath.Vector sun = this.direction(position.x, position.z);
        double height = (position.y - REFERENCE_HEIGHT) / Math.max(sun.y(), MIN_SOLAR_HEIGHT);
        return position.add(-sun.x() * height, 0, -sun.z() * height);
    }

    AABB projectBounds(AABB bounds) {
        Vec3 center = bounds.getCenter();
        MunSkyMath.Vector sun = this.direction(center.x, center.z);
        double coordinate = Math.max(Math.abs(center.x), Math.abs(center.z));
        double radius = Math.hypot(bounds.getXsize(), bounds.getZsize()) / 2 + 4 * Math.ulp((float) coordinate);
        // H 为最大 horizon 长度，F = sqrt(1 + H²) + H；立体投影参数梯度不超过 2(F + H)/2048。
        // 旋转后的单位光线变化不超过参数变化的两倍。
        // 加入浮点余量；不能只投影角点，因为区块内部的太阳方向也在变化。
        double error = Math.min(2, this.directionGradient * radius) + 1.0E-5;
        double minDenominator = Math.max(MIN_SOLAR_HEIGHT, sun.y() - error);
        double maxDenominator = Math.max(MIN_SOLAR_HEIGHT, sun.y() + error);
        double low = bounds.minY - REFERENCE_HEIGHT;
        double high = bounds.maxY - REFERENCE_HEIGHT;
        double minHeight = Math.min(low / minDenominator, low / maxDenominator);
        double maxHeight = Math.max(high / minDenominator, high / maxDenominator);
        AABB projected = new AABB(
            bounds.minX - maxProduct(sun.x() - error, sun.x() + error, minHeight, maxHeight), bounds.minY,
            bounds.minZ - maxProduct(sun.z() - error, sun.z() + error, minHeight, maxHeight),
            bounds.maxX - minProduct(sun.x() - error, sun.x() + error, minHeight, maxHeight), bounds.maxY,
            bounds.maxZ - minProduct(sun.z() - error, sun.z() + error, minHeight, maxHeight)
        );
        double extent = Math.max(Math.max(Math.abs(projected.minX), Math.abs(projected.maxX)),
            Math.max(Math.abs(projected.minZ), Math.abs(projected.maxZ)));
        return projected.inflate(0.01 + 8 * Math.ulp((float) extent));
    }

    private static double minProduct(double minA, double maxA, double minB, double maxB) {
        return Math.min(Math.min(minA * minB, minA * maxB), Math.min(maxA * minB, maxA * maxB));
    }

    private static double maxProduct(double minA, double maxA, double minB, double maxB) {
        return Math.max(Math.max(minA * minB, minA * maxB), Math.max(maxA * minB, maxA * maxB));
    }

    void write(ByteBuffer data, Vec3 camera, float ambientFloor) {
        data.putFloat(0, (float) this.reference.x()).putFloat(4, (float) this.reference.y())
            .putFloat(8, (float) this.reference.z()).putFloat(12, 0);
        data.putFloat(16, (float) (this.origin.x / MunSkyMath.NEAR_SIDE_HALF_SIZE))
            .putFloat(20, (float) (this.origin.y - REFERENCE_HEIGHT))
            .putFloat(24, (float) (this.origin.z / MunSkyMath.NEAR_SIDE_HALF_SIZE)).putFloat(28, 0);
        data.putFloat(32, this.eclipse.size()).putFloat(36, this.eclipseCoverage).putFloat(40, ambientFloor).putFloat(44, 0);
        for (int index = 0; index < 8; index++) {
            int offset = 48 + index * 16;
            data.putFloat(offset, (float) this.horizon[index * 2]).putFloat(offset + 4, (float) this.horizon[index * 2 + 1])
                .putFloat(offset + 8, 0).putFloat(offset + 12, 0);
        }
        for (int index = 0; index < 12; index++) {
            MunSkyMath.Vector point = index < this.eclipse.size() ? this.eclipse.get(index) : MunSkyMath.UP;
            int offset = 176 + index * 16;
            data.putFloat(offset, (float) point.x()).putFloat(offset + 4, (float) point.y())
                .putFloat(offset + 8, 0).putFloat(offset + 12, 0);
        }
        Vec3 relative = camera.subtract(this.origin);
        data.putFloat(368, (float) relative.x).putFloat(372, (float) relative.y).putFloat(376, (float) relative.z).putFloat(380, 0);
    }
}
