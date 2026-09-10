package dev.dubhe.anvilcraft.client.renderer;

import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL20C;

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

    void update(long dayTime, double partialTick, Vec3 origin) {
        this.origin = origin;
        MunSkyMath.Vector reference = MunSkyMath.referenceSun(dayTime, partialTick);
        boolean changed = !this.reference.equals(reference);
        this.reference = reference;
        for (int index = 0; index < 8; index++) {
            MunSkyMath.Vector corner = MunSkyMath.atmosphereCorner(index, dayTime, partialTick);
            double x = corner.x() / corner.y();
            double z = corner.z() / corner.y();
            changed |= this.horizon[index * 2] != x || this.horizon[index * 2 + 1] != z;
            this.horizon[index * 2] = x;
            this.horizon[index * 2 + 1] = z;
        }
        if (changed) this.shadowRevision++;
        this.eclipse = MunSkyMath.solarOcclusion(dayTime, partialTick);
        double area = 0;
        for (int index = 0; index < this.eclipse.size(); index++) {
            MunSkyMath.Vector a = this.eclipse.get(index);
            MunSkyMath.Vector b = this.eclipse.get((index + 1) % this.eclipse.size());
            area += a.x() * b.y() - a.y() * b.x();
        }
        this.eclipseCoverage = (float) (Math.abs(area) / (8 * MunSkyMath.SUN_DISC_HALF_SIZE * MunSkyMath.SUN_DISC_HALF_SIZE));
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

    void origin(ShaderInstance shader, Vec3 origin) {
        var uniform = shader.getUniform("SolarOrigin");
        if (uniform == null) throw new IllegalStateException("Missing lunar solar origin uniform");
        uniform.set((float) (origin.x / MunSkyMath.NEAR_SIDE_HALF_SIZE), (float) (origin.y - REFERENCE_HEIGHT),
            (float) (origin.z / MunSkyMath.NEAR_SIDE_HALF_SIZE));
        uniform.upload();
    }

    void applyShadow(ShaderInstance shader) {
        shader.safeGetUniform("ShadowSolarReference").set(
            (float) this.reference.x(), (float) this.reference.y(), (float) this.reference.z());
        shader.safeGetUniform("ShadowSolarOrigin").set((float) (this.origin.x / MunSkyMath.NEAR_SIDE_HALF_SIZE),
            (float) (this.origin.y - REFERENCE_HEIGHT), (float) (this.origin.z / MunSkyMath.NEAR_SIDE_HALF_SIZE));
        for (int index = 0; index < 8; index++) {
            shader.safeGetUniform("ShadowSolarHorizon" + index).set((float) this.horizon[index * 2], (float) this.horizon[index * 2 + 1]);
        }
    }

    void applyShadow(int program) {
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program, "ShadowSolarReference"),
            (float) this.reference.x(), (float) this.reference.y(), (float) this.reference.z());
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program, "ShadowSolarOrigin"),
            (float) (this.origin.x / MunSkyMath.NEAR_SIDE_HALF_SIZE), (float) (this.origin.y - REFERENCE_HEIGHT),
            (float) (this.origin.z / MunSkyMath.NEAR_SIDE_HALF_SIZE));
        for (int index = 0; index < 8; index++) {
            GL20C.glUniform2f(GL20C.glGetUniformLocation(program, "ShadowSolarHorizon" + index),
                (float) this.horizon[index * 2], (float) this.horizon[index * 2 + 1]);
        }
    }

    void apply(ShaderInstance shader) {
        shader.safeGetUniform("SolarReference").set((float) this.reference.x(), (float) this.reference.y(), (float) this.reference.z());
        shader.safeGetUniform("SolarOrigin").set((float) (this.origin.x / MunSkyMath.NEAR_SIDE_HALF_SIZE),
            (float) (this.origin.y - REFERENCE_HEIGHT), (float) (this.origin.z / MunSkyMath.NEAR_SIDE_HALF_SIZE));
        shader.safeGetUniform("SolarEclipseCount").set(this.eclipse.size());
        shader.safeGetUniform("SolarEclipseCoverage").set(this.eclipseCoverage);
        for (int index = 0; index < 8; index++) {
            shader.safeGetUniform("SolarHorizon" + index).set((float) this.horizon[index * 2], (float) this.horizon[index * 2 + 1]);
        }
        for (int index = 0; index < 12; index++) {
            MunSkyMath.Vector point = index < this.eclipse.size() ? this.eclipse.get(index) : MunSkyMath.UP;
            shader.safeGetUniform("SolarEclipse" + index).set((float) point.x(), (float) point.y());
        }
    }

    void apply(int program) {
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program, "SolarReference"),
            (float) this.reference.x(), (float) this.reference.y(), (float) this.reference.z());
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program, "SolarOrigin"),
            (float) (this.origin.x / MunSkyMath.NEAR_SIDE_HALF_SIZE), (float) (this.origin.y - REFERENCE_HEIGHT),
            (float) (this.origin.z / MunSkyMath.NEAR_SIDE_HALF_SIZE));
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "SolarEclipseCount"), this.eclipse.size());
        GL20C.glUniform1f(GL20C.glGetUniformLocation(program, "SolarEclipseCoverage"), this.eclipseCoverage);
        for (int index = 0; index < 8; index++) {
            GL20C.glUniform2f(GL20C.glGetUniformLocation(program, "SolarHorizon" + index),
                (float) this.horizon[index * 2], (float) this.horizon[index * 2 + 1]);
        }
        for (int index = 0; index < 12; index++) {
            MunSkyMath.Vector point = index < this.eclipse.size() ? this.eclipse.get(index) : MunSkyMath.UP;
            GL20C.glUniform2f(GL20C.glGetUniformLocation(program, "SolarEclipse" + index), (float) point.x(), (float) point.y());
        }
    }
}
