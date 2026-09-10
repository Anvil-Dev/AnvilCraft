package dev.dubhe.anvilcraft.worldgen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 月球天空、日照与正背面边界共用的几何计算。 */
public final class MunSkyMath {
    public static final double NEAR_SIDE_HALF_SIZE = 2048.0;
    public static final long DAY_LENGTH = 24000L * 8;
    public static final long EARTH_ROTATION_PERIOD = 24000L;
    public static final double EARTH_HALF_SIZE = 0.09;
    public static final double EARTH_ATMOSPHERE_THICKNESS = 0.014;
    public static final double SUN_HALF_SIZE = 0.27;
    // 原版太阳贴图的中央 8 × 8 像素为日面，其余 32 × 32 区域为光晕。
    public static final double SUN_DISC_HALF_SIZE = SUN_HALF_SIZE / 4;
    public static final Vector UP = new Vector(0, 1, 0);
    public static final Rotation EARTH_ROTATION = new Rotation(new Vector(Math.sqrt(0.5), 0, Math.sqrt(0.5)), Math.toRadians(30));
    private static final double ECLIPSE_COSINE = Math.cos(
        Math.asin(Math.sqrt(3) * EARTH_HALF_SIZE) + Math.atan(Math.sqrt(2) * SUN_DISC_HALF_SIZE)
    );

    private MunSkyMath() {
    }

    public static boolean isFarSide(double x, double z) {
        return Math.max(Math.abs(x), Math.abs(z)) > NEAR_SIDE_HALF_SIZE;
    }

    public static Rotation skyRotation(double x, double z) {
        return skyRotation(x, z, 0, 0);
    }

    public static Rotation skyRotation(double x, double z, long dayTime, double partialTick) {
        return skyRotation(x, z, earthSpin(dayTime, partialTick));
    }

    private static Rotation skyRotation(double x, double z, Rotation spin) {
        double distance = Math.max(Math.abs(x), Math.abs(z));
        if (distance == 0) return new Rotation(new Vector(1, 0, 0), 0);
        double length = Math.hypot(x, z);
        double east = x / length;
        double south = z / length;
        double edgeAngle = 0;
        // 把大气层的外轮廓计入边界，确保边界四角也不会残留一角主世界。
        for (int corner = 0; corner < 8; corner++) {
            Vector point = cubeCorner(corner, EARTH_HALF_SIZE + EARTH_ATMOSPHERE_THICKNESS, spin);
            edgeAngle = Math.max(edgeAngle, Math.atan2(east * point.x + south * point.z, point.y));
        }
        double horizonAngle = Math.PI / 2 + edgeAngle;
        double angle = 2 * Math.atan(distance / NEAR_SIDE_HALF_SIZE * Math.tan(horizonAngle / 2));
        return new Rotation(new Vector(-south, 0, east), angle);
    }

    public static Vector earthCorner(int corner) {
        return earthCorner(corner, 0, 0);
    }

    public static Vector earthCorner(int corner, long dayTime, double partialTick) {
        return cubeCorner(corner, EARTH_HALF_SIZE, earthSpin(dayTime, partialTick));
    }

    public static Vector atmosphereCorner(int corner) {
        return atmosphereCorner(corner, 0, 0);
    }

    public static Vector atmosphereCorner(int corner, long dayTime, double partialTick) {
        return cubeCorner(corner, EARTH_HALF_SIZE + EARTH_ATMOSPHERE_THICKNESS, earthSpin(dayTime, partialTick));
    }

    private static Vector cubeCorner(int corner, double halfSize, Rotation spin) {
        Vector point = new Vector(
            (corner & 1) == 0 ? -halfSize : halfSize,
            (corner & 2) == 0 ? -halfSize : halfSize,
            (corner & 4) == 0 ? -halfSize : halfSize
        );
        return EARTH_ROTATION.apply(spin.apply(point)).add(UP);
    }

    public static Rotation earthSpin(long dayTime, double partialTick) {
        double angle = (Math.floorMod(dayTime, EARTH_ROTATION_PERIOD) + partialTick) / EARTH_ROTATION_PERIOD * (Math.PI * 2);
        return new Rotation(UP, angle);
    }

    public static double solarAngle(long dayTime, double partialTick) {
        // 原版第 0 天为满月；以当天午夜的满月对齐为日月食基准。
        return (Math.floorMod(dayTime, DAY_LENGTH) - 18000.0 + partialTick) / DAY_LENGTH * (Math.PI * 2);
    }

    public static Vector referenceSun(long dayTime, double partialTick) {
        double angle = solarAngle(dayTime, partialTick);
        return new Vector(Math.sin(angle), Math.cos(angle), 0);
    }

    public static Vector sunDirection(double x, double z, long dayTime, double partialTick) {
        return skyRotation(x, z, dayTime, partialTick).apply(referenceSun(dayTime, partialTick));
    }

    public static double sunlight(double x, double z, long dayTime, double partialTick) {
        Rotation spin = earthSpin(dayTime, partialTick);
        Rotation rotation = skyRotation(x, z, spin);
        Vector sun = referenceSun(dayTime, partialTick);
        Vector tangent = new Vector(sun.y, -sun.x, 0);
        double height = rotation.apply(sun).y;
        double horizontal = rotation.apply(tangent).y;
        double vertical = rotation.apply(new Vector(0, 0, 1)).y;
        double extent = SUN_DISC_HALF_SIZE * (Math.abs(horizontal) + Math.abs(vertical));
        if (height + extent <= 0) return 0;
        boolean possibleEclipse = sun.y >= ECLIPSE_COSINE;
        if (height - extent >= 0 && !possibleEclipse) return 1;
        List<Point> visible = clip(List.of(
            new Point(-SUN_DISC_HALF_SIZE, -SUN_DISC_HALF_SIZE), new Point(SUN_DISC_HALF_SIZE, -SUN_DISC_HALF_SIZE),
            new Point(SUN_DISC_HALF_SIZE, SUN_DISC_HALF_SIZE), new Point(-SUN_DISC_HALF_SIZE, SUN_DISC_HALF_SIZE)
        ), horizontal, vertical, height);
        double area = area(visible);
        if (possibleEclipse) {
            List<Point> silhouette = earthSilhouette(sun, tangent, spin);
            List<Point> covered = visible;
            for (int i = 0; i < silhouette.size(); i++) {
                Point first = silhouette.get(i);
                Point second = silhouette.get((i + 1) % silhouette.size());
                double dx = second.x - first.x;
                double dy = second.y - first.y;
                covered = clip(covered, -dy, dx, dy * first.x - dx * first.y);
            }
            area -= area(covered);
        }
        double fraction = area / (4 * SUN_DISC_HALF_SIZE * SUN_DISC_HALF_SIZE);
        return fraction < 1.0e-10 ? 0 : Math.min(fraction, 1);
    }

    /** 日面被主世界遮挡的凸多边形，供世界位置相关的月面光照裁切地平线。 */
    public static List<Vector> solarOcclusion(long dayTime, double partialTick) {
        Vector sun = referenceSun(dayTime, partialTick);
        if (sun.y < ECLIPSE_COSINE) return List.of();
        Vector tangent = new Vector(sun.y, -sun.x, 0);
        List<Point> silhouette = earthSilhouette(sun, tangent, earthSpin(dayTime, partialTick));
        List<Point> covered = List.of(
            new Point(-SUN_DISC_HALF_SIZE, -SUN_DISC_HALF_SIZE), new Point(SUN_DISC_HALF_SIZE, -SUN_DISC_HALF_SIZE),
            new Point(SUN_DISC_HALF_SIZE, SUN_DISC_HALF_SIZE), new Point(-SUN_DISC_HALF_SIZE, SUN_DISC_HALF_SIZE)
        );
        for (int i = 0; i < silhouette.size(); i++) {
            Point first = silhouette.get(i);
            Point second = silhouette.get((i + 1) % silhouette.size());
            double dx = second.x - first.x;
            double dy = second.y - first.y;
            covered = clip(covered, -dy, dx, dy * first.x - dx * first.y);
        }
        return covered.stream().map(point -> new Vector(point.x, point.y, 0)).toList();
    }

    private static List<Point> earthSilhouette(Vector sun, Vector tangent, Rotation spin) {
        List<Point> points = new ArrayList<>(8);
        for (int corner = 0; corner < 8; corner++) {
            Vector point = cubeCorner(corner, EARTH_HALF_SIZE, spin);
            double depth = point.dot(sun);
            points.add(new Point(point.dot(tangent) / depth, point.z / depth));
        }
        points.sort(Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y));
        List<Point> hull = new ArrayList<>(8);
        for (Point point : points) {
            while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= 0) hull.removeLast();
            hull.add(point);
        }
        int lowerSize = hull.size();
        for (int i = points.size() - 2; i >= 0; i--) {
            Point point = points.get(i);
            while (hull.size() > lowerSize && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= 0) hull.removeLast();
            hull.add(point);
        }
        hull.removeLast();
        return hull;
    }

    private static double cross(Point first, Point second, Point third) {
        return (second.x - first.x) * (third.y - first.y) - (second.y - first.y) * (third.x - first.x);
    }

    private static List<Point> clip(List<Point> polygon, double a, double b, double c) {
        if (polygon.isEmpty()) return polygon;
        List<Point> clipped = new ArrayList<>(polygon.size() + 1);
        Point previous = polygon.getLast();
        double previousDistance = a * previous.x + b * previous.y + c;
        for (Point current : polygon) {
            double distance = a * current.x + b * current.y + c;
            if ((distance >= 0) != (previousDistance >= 0)) {
                double fraction = previousDistance / (previousDistance - distance);
                clipped.add(new Point(
                    previous.x + (current.x - previous.x) * fraction,
                    previous.y + (current.y - previous.y) * fraction
                ));
            }
            if (distance >= 0) clipped.add(current);
            previous = current;
            previousDistance = distance;
        }
        return clipped;
    }

    private static double area(List<Point> polygon) {
        double area = 0;
        for (int i = 0; i < polygon.size(); i++) {
            Point first = polygon.get(i);
            Point second = polygon.get((i + 1) % polygon.size());
            area += first.x * second.y - second.x * first.y;
        }
        return Math.abs(area) / 2;
    }

    private record Point(double x, double y) {
    }

    public static int skyDarken(double x, double z, long dayTime) {
        return (int) Math.round(15 * (1 - sunlight(x, z, dayTime, 0)));
    }

    public static boolean intersectsEarth(Vector ray) {
        return intersectsEarth(ray, 0, 0);
    }

    public static boolean intersectsEarth(Vector ray, long dayTime, double partialTick) {
        Rotation spin = earthSpin(dayTime, partialTick);
        Vector origin = spin.inverse(EARTH_ROTATION.inverse(UP.scale(-1)));
        Vector direction = spin.inverse(EARTH_ROTATION.inverse(ray));
        double near = 0;
        double far = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            double start = origin.component(axis);
            double delta = direction.component(axis);
            if (Math.abs(delta) < 1.0e-12) {
                if (Math.abs(start) > EARTH_HALF_SIZE) return false;
                continue;
            }
            double first = (-EARTH_HALF_SIZE - start) / delta;
            double second = (EARTH_HALF_SIZE - start) / delta;
            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
            if (far < near) return false;
        }
        return far > 0;
    }

    public record Vector(double x, double y, double z) {
        public Vector add(Vector other) {
            return new Vector(this.x + other.x, this.y + other.y, this.z + other.z);
        }

        public Vector scale(double scale) {
            return new Vector(this.x * scale, this.y * scale, this.z * scale);
        }

        public double dot(Vector other) {
            return this.x * other.x + this.y * other.y + this.z * other.z;
        }

        public Vector cross(Vector other) {
            return new Vector(
                this.y * other.z - this.z * other.y,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x
            );
        }

        private double component(int axis) {
            return switch (axis) {
                case 0 -> this.x;
                case 1 -> this.y;
                default -> this.z;
            };
        }
    }

    public record Rotation(Vector axis, double angle) {
        public Vector apply(Vector vector) {
            double cosine = Math.cos(this.angle);
            return vector.scale(cosine)
                .add(this.axis.cross(vector).scale(Math.sin(this.angle)))
                .add(this.axis.scale(this.axis.dot(vector) * (1 - cosine)));
        }

        public Vector inverse(Vector vector) {
            return new Rotation(this.axis, -this.angle).apply(vector);
        }
    }
}
