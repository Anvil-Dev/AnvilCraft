package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.worldgen.MunSkyMath.Rotation;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath.Vector;

/** 从 Mun 的地月相对位置反推主世界天体，太阳仍与原版昼夜一致。 */
public record OverworldSkyState(Vector sun, Vector moon, Rotation frame, double illuminatedFraction) {
    private static final Vector NORTH = new Vector(0, 0, 1);
    private static final Rotation MODEL_ROTATION = new Rotation(new Vector(1, 0, 0), Math.PI / 2);

    public static OverworldSkyState at(long dayTime, double partialTick, double timeOfDay) {
        Rotation frame = new Rotation(NORTH, timeOfDay * Math.PI * 2 - MunSkyMath.solarAngle(dayTime, partialTick));
        Vector sun = frame.apply(oppositeView(MunSkyMath.referenceSun(dayTime, partialTick)));
        Vector moon = frame.apply(oppositeView(MunSkyMath.earthCenter(dayTime, partialTick).scale(-1)));
        return new OverworldSkyState(sun, moon, frame, Math.clamp((1 - sun.dot(moon)) / 2, 0, 1));
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
