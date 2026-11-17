package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.worldgen.MunSkyMath.Rotation;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath.Vector;

/** 为天空渲染和登陆窗口提供天体位置，模型样式不参与轨道计算。 */
public record OverworldSkyState(
    Vector sun, Vector moon, Rotation frame, double illuminatedFraction, Vector moonLight, int moonPhase
) {
    private static final Vector NORTH = new Vector(0, 0, 1);
    private static final Rotation MODEL_ROTATION = new Rotation(new Vector(1, 0, 0), Math.PI / 2);
    /** 与原版午夜前后 1000 tick 接近的天顶角容差。 */
    private static final double OVERHEAD_COSINE = Math.cos(Math.toRadians(18));

    public static OverworldSkyState at(long dayTime, double partialTick, double timeOfDay) {
        Rotation frame = new Rotation(NORTH, timeOfDay * Math.PI * 2 - MunSkyMath.solarAngle(dayTime, partialTick));
        Vector sun = frame.apply(oppositeView(MunSkyMath.referenceSun(dayTime, partialTick)));
        Vector moon = frame.apply(oppositeView(MunSkyMath.earthCenter(dayTime, partialTick).scale(-1)));
        int phase = Math.floorMod((int) Math.round(MunSkyMath.solarAngle(dayTime, partialTick) * 4 / Math.PI), 8);
        return new OverworldSkyState(sun, moon, frame, Math.clamp((1 - sun.dot(moon)) / 2, 0, 1), sun, phase);
    }

    /** 原版轨道保持日月相对位置，月相光照独立于原版固定的日月夹角。 */
    public static OverworldSkyState vanilla(double sunAngle, double moonAngle, int moonPhase) {
        Rotation frame = new Rotation(NORTH, moonAngle);
        Vector sun = new Rotation(NORTH, sunAngle).apply(MunSkyMath.UP);
        Vector moon = frame.apply(MunSkyMath.UP.scale(-1));
        Vector light = new Rotation(NORTH, moonAngle + moonPhase * Math.PI / 4).apply(MunSkyMath.UP);
        return new OverworldSkyState(sun, moon, frame, (1 - light.dot(moon)) / 2, light, moonPhase);
    }

    public boolean isMoonOverhead() {
        return this.moon.y() >= OVERHEAD_COSINE;
    }

    /** 月面始终朝向地球，天平动来自观察方向的变化，不额外叠加自转。 */
    public Vector moonNormal(Vector normal) {
        return this.frame.apply(oppositeView(MODEL_ROTATION.apply(normal)));
    }

    /** 反向观察地月系时翻转黄道极，保证月球沿原版天空逐日东移、升起推迟。 */
    private static Vector oppositeView(Vector vector) {
        return new Vector(-vector.x(), vector.y(), -vector.z());
    }
}
