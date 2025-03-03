package dev.dubhe.anvilcraft.util;

import org.joml.Vector2f;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class MathUtil {

    /**
     * Calc a vector2 that equals to a vector2 rotated an angle
     *
     * @param v   origin vector, wont be changed
     * @param deg angle rotated, in degrees
     * @return rotated vector2
     */
    public static Vector2f rotationDegrees(Vector2f v, float deg) {
        return rotate(v, (float) toRadians(deg));
    }

    /**
     * Calc a vector2 that equals to a vector2 rotated an angle
     *
     * @param v origin vector, wont be changed
     * @param d angle rotated, in radians
     * @return rotated vector2
     */
    public static Vector2f rotate(Vector2f v, float d) {
        return new Vector2f(
            (float) (v.x * cos(d) - v.y * sin(d)),
            (float) (v.x * sin(d) + v.y * cos(d))
        );
    }

    public static Vector2f copy(Vector2f v) {
        return new Vector2f(v.x, v.y);
    }

    /**
     * @return Angle in radians
     */
    public static float angle(Vector2f from, Vector2f to) {
        return (float) ((atan2(to.y, to.x) - atan2(from.y, from.x)) % (Math.PI * 2));
    }

    /**
     * @return Angle in degrees
     */
    public static float angleDegrees(Vector2f from, Vector2f to) {
        return (float) toDegrees(angle(from, to));
    }

    public static float safeDivide(float a, float b) {
        if (a == b) return 1;
        return a / b;
    }
}
