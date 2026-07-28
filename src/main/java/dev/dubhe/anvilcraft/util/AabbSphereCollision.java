package dev.dubhe.anvilcraft.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

final class AabbSphereCollision {
    private static final double EPSILON = 1.0e-12;
    private static final int BINARY_SEARCH_STEPS = 56;

    private AabbSphereCollision() {
    }

    /** 查找移动中的 AABB 首次进入球体的时刻。 */
    static @Nullable Hit findFirst(AABB box, Vec3 movement, Vec3 sphereCenter, double sphereRadius) {
        if (sphereRadius <= 0 || movement.lengthSqr() <= EPSILON) return null;

        double[] breakpoints = new double[8];
        int count = 0;
        breakpoints[count++] = 0;
        breakpoints[count++] = 1;
        count = addAxisBreakpoints(breakpoints, count, box.minX, box.maxX, sphereCenter.x, movement.x);
        count = addAxisBreakpoints(breakpoints, count, box.minY, box.maxY, sphereCenter.y, movement.y);
        count = addAxisBreakpoints(breakpoints, count, box.minZ, box.maxZ, sphereCenter.z, movement.z);
        Arrays.sort(breakpoints, 0, count);

        double radiusSqr = sphereRadius * sphereRadius;
        double tolerance = EPSILON * Math.max(1.0, radiusSqr);
        double intervalStart = breakpoints[0];
        for (int i = 1; i < count; i++) {
            double intervalEnd = breakpoints[i];
            if (intervalEnd - intervalStart <= EPSILON) continue;

            Hit hit = findInInterval(
                box,
                movement,
                sphereCenter,
                radiusSqr,
                tolerance,
                intervalStart,
                intervalEnd
            );
            if (hit != null) return hit;
            intervalStart = intervalEnd;
        }
        return createApproachingHit(box, movement, sphereCenter, 1, radiusSqr, tolerance);
    }

    static Vec3 removeInwardComponent(Vec3 vector, Vec3 outwardNormal) {
        double inwardSpeed = vector.dot(outwardNormal);
        return inwardSpeed < 0 ? vector.subtract(outwardNormal.scale(inwardSpeed)) : vector;
    }

    static boolean intersects(AABB box, Vec3 sphereCenter, double sphereRadius, double tolerance) {
        if (sphereRadius <= 0) return false;
        Vec3 closest = closestPoint(box, sphereCenter);
        double contactRadius = sphereRadius + Math.max(0, tolerance);
        return closest.distanceToSqr(sphereCenter) <= contactRadius * contactRadius;
    }

    private static @Nullable Hit findInInterval(
        AABB box,
        Vec3 movement,
        Vec3 sphereCenter,
        double radiusSqr,
        double tolerance,
        double intervalStart,
        double intervalEnd
    ) {
        Hit startHit = createApproachingHit(
            box,
            movement,
            sphereCenter,
            intervalStart,
            radiusSqr,
            tolerance
        );
        if (startHit != null) return startHit;

        double midpoint = (intervalStart + intervalEnd) * 0.5;
        Quadratic distance = distanceQuadratic(box, movement, sphereCenter, midpoint);
        if (distance.a <= EPSILON) return null;

        double minimumTime = Math.clamp(-distance.b / (2 * distance.a), intervalStart, intervalEnd);
        if (minimumTime <= intervalStart + EPSILON
            || distance.valueAt(minimumTime) > radiusSqr + tolerance) {
            return null;
        }

        double outside = intervalStart;
        double inside = minimumTime;
        for (int i = 0; i < BINARY_SEARCH_STEPS; i++) {
            double time = (outside + inside) * 0.5;
            if (distance.valueAt(time) <= radiusSqr) {
                inside = time;
            } else {
                outside = time;
            }
        }
        return createApproachingHit(box, movement, sphereCenter, inside, radiusSqr, tolerance);
    }

    private static @Nullable Hit createApproachingHit(
        AABB box,
        Vec3 movement,
        Vec3 sphereCenter,
        double time,
        double radiusSqr,
        double tolerance
    ) {
        AABB movedBox = box.move(movement.scale(time));
        Vec3 closest = closestPoint(movedBox, sphereCenter);
        Vec3 normal = closest.subtract(sphereCenter);
        double distanceSqr = normal.lengthSqr();
        if (distanceSqr > radiusSqr + tolerance || distanceSqr <= EPSILON) return null;

        normal = normal.scale(1 / Math.sqrt(distanceSqr));
        return movement.dot(normal) < -EPSILON ? new Hit(time, normal) : null;
    }

    private static int addAxisBreakpoints(
        double[] breakpoints,
        int count,
        double min,
        double max,
        double center,
        double movement
    ) {
        if (Math.abs(movement) <= EPSILON) return count;
        count = addBreakpoint(breakpoints, count, (center - min) / movement);
        return addBreakpoint(breakpoints, count, (center - max) / movement);
    }

    private static int addBreakpoint(double[] breakpoints, int count, double time) {
        if (time > 0 && time < 1) breakpoints[count++] = time;
        return count;
    }

    private static Quadratic distanceQuadratic(AABB box, Vec3 movement, Vec3 center, double sampleTime) {
        double[] coefficients = new double[3];
        addAxisDistance(
            coefficients,
            box.minX,
            box.maxX,
            center.x,
            movement.x,
            sampleTime
        );
        addAxisDistance(
            coefficients,
            box.minY,
            box.maxY,
            center.y,
            movement.y,
            sampleTime
        );
        addAxisDistance(
            coefficients,
            box.minZ,
            box.maxZ,
            center.z,
            movement.z,
            sampleTime
        );
        return new Quadratic(coefficients[0], coefficients[1], coefficients[2]);
    }

    private static void addAxisDistance(
        double[] coefficients,
        double min,
        double max,
        double center,
        double movement,
        double sampleTime
    ) {
        double offset;
        if (center < min + movement * sampleTime) {
            offset = min - center;
        } else if (center > max + movement * sampleTime) {
            offset = max - center;
        } else {
            return;
        }
        coefficients[0] += movement * movement;
        coefficients[1] += 2 * movement * offset;
        coefficients[2] += offset * offset;
    }

    private static Vec3 closestPoint(AABB box, Vec3 point) {
        return new Vec3(
            Math.clamp(point.x, box.minX, box.maxX),
            Math.clamp(point.y, box.minY, box.maxY),
            Math.clamp(point.z, box.minZ, box.maxZ)
        );
    }

    record Hit(double time, Vec3 normal) {
    }

    private record Quadratic(double a, double b, double c) {
        double valueAt(double time) {
            return (this.a * time + this.b) * time + this.c;
        }
    }
}
