package dev.dubhe.anvilcraft.util;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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

    public static boolean isInRange(double value, double min, double max) {
        if (min > max) {
            double min1 = min;
            min = max;
            max = min1;
        }

        return value > min && value < max;
    }

    public static boolean isInRange(double valueX, double valueY, double minX, double minY, double maxX, double maxY) {
        if (minX > maxX) {
            double minX1 = minX;
            minX = maxX;
            maxX = minX1;
        }
        if (minY > maxY) {
            double minY1 = minY;
            minY = maxY;
            maxY = minY1;
        }

        return valueX > minX && valueX < maxX && valueY > minY && valueY < maxY;
    }

    public static Vec3i dist(BlockPos a, BlockPos b) {
        return new Vec3i(a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
    }

    public static Direction getDirection(BlockPos from, BlockPos to) {
        return Direction.fromDelta(from.getX() - to.getX(), from.getY() - to.getY(), from.getZ() - to.getZ());
    }

    private static final Int2DoubleMap FACTORIAL_CACHE = new Int2DoubleOpenHashMap();

    public static double factorial(int value) {
        if (value < 1) return 1;
        if (FACTORIAL_CACHE.containsKey(value)) return FACTORIAL_CACHE.get(value);
        double result = 1;
        for (int i = 2; i <= value; i++) {
            result *= i;
        }
        FACTORIAL_CACHE.put(value, result);
        return result;
    }

    public static float clampWithProportion(float value, float min, float max) {
        float length = Math.abs(max - min);
        if (length == 0) throw new IllegalArgumentException("The min value " + min + " cannot be equal to the max value" + max + "!");

        if (value > max) {
            while (value > max + length) {
                value -= length;
            }
            return max - (max - value);
        } else if (value < min) {
            while (value < min + length) {
                value += length;
            }
            return min + (value - min);
        }
        return value;
    }
}
