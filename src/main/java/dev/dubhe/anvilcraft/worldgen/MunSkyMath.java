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
    public static final double EARTH_ATMOSPHERE_THICKNESS = 0.0182;
    /** 仅压缩沿视线的深度差，保持横向尺寸，模拟拉远后放大的弱透视。 */
    public static final double EARTH_PERSPECTIVE = 0.25;
    public static final Vector UP = new Vector(0, 1, 0);
    public static final double SUN_BODY_HALF_SIZE = 0.05;
    public static final double SUN_PERSPECTIVE = 0.25;
    public static final Rotation SUN_ROTATION = new Rotation(new Vector(Math.sqrt(0.5), 0, Math.sqrt(0.5)), Math.toRadians(35));
    private static final List<Point> SUN_SILHOUETTE = sunSilhouette();
    public static final double SUN_DISC_HALF_SIZE = SUN_SILHOUETTE.stream()
        .mapToDouble(point -> Math.max(Math.abs(point.x), Math.abs(point.y))).max().orElseThrow();
    public static final double SUN_DISC_AREA = area(SUN_SILHOUETTE);
    /**
     * 地轴朝向：黄赤交角 23.4393°（真实地球值）。
     *
     * <p>本帧中太阳沿 XY 平面运行（{@link #referenceSun} 的 z 恒为 0），故黄道极是 Z；
     * 地球中心在 {@code (0, 1, 0)}，观察者在原点，看向地球的视线为 +Y。地轴须与黄道极成
     * 黄赤交角、且不偏向观察者，因此取 {@code (sin ε, 0, cos ε)}：既有 23.44° 的倾角，
     * 又因 Y 分量为 0 而不会让任意一极正对玩家（否则自转会看起来在绕视线打转）。</p>
     *
     * <p>该目标与 {@link #UP} 正交，故旋转角恰为 90°，轴取 {@code UP × 目标}。此处为平均朝向，
     * 天平动在 {@link #earthNormal} 中于此之上再叠加一次摆动。</p>
     */
    public static final Rotation EARTH_ROTATION = new Rotation(
        new Vector(Math.cos(Math.toRadians(23.4393)), 0, -Math.sin(Math.toRadians(23.4393))),
        Math.PI / 2
    );
    /** 纬天平动振幅（度）：月球赤道与轨道面有交角，使地球在天空中南北摆动，真实值约 6.69°。 */
    public static final double LIBRATION_LATITUDE_AMPLITUDE = 6.69;
    /** 经天平动振幅（度）：轨道偏心率使地球在天空中东西摆动，真实值约 7.90°。 */
    public static final double LIBRATION_LONGITUDE_AMPLITUDE = 7.90;
    /** 天平动使地球中心偏离平均方向的角度上界：两分量按平方和合成，实际最大约 7.9°。 */
    private static final double LIBRATION_MAX_OFFSET = Math.hypot(
        Math.toRadians(LIBRATION_LATITUDE_AMPLITUDE), Math.toRadians(LIBRATION_LONGITUDE_AMPLITUDE)
    );
    private static final double ECLIPSE_COSINE = Math.cos(
        Math.asin(Math.sqrt(3) * EARTH_HALF_SIZE) + Math.atan(Math.sqrt(2) * SUN_DISC_HALF_SIZE) + LIBRATION_MAX_OFFSET
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
        return cubeCorner(corner, EARTH_HALF_SIZE, earthSpin(dayTime, partialTick), libration(dayTime, partialTick));
    }

    public static Vector atmosphereCorner(int corner) {
        return atmosphereCorner(corner, 0, 0);
    }

    /**
     * 大气层外轮廓顶点，用于地平线与日影计算。
     *
     * <p>取平均朝向：地平线由观测几何决定，不随地球天平动摆动，否则天体投影会跟着一起晃。</p>
     */
    public static Vector atmosphereCorner(int corner, long dayTime, double partialTick) {
        return cubeCorner(corner, EARTH_HALF_SIZE + EARTH_ATMOSPHERE_THICKNESS, earthSpin(dayTime, partialTick));
    }

    /**
     * 天平动：月球公转使地球在月面天空中偏离平均位置的摆动。
     *
     * <p>纬天平动绕黄道面内的 X 轴，让地球朝黄道极方向摆动；经天平动绕黄道极 Z 轴，让地球沿黄道摆动。
     * 两者相差四分之一周期，地球在一个公转周期内画出一条椭圆轨迹，即通常所称的天平动图形。</p>
     */
    public static Libration libration(long dayTime, double partialTick) {
        double phase = (Math.floorMod(dayTime, DAY_LENGTH) + partialTick) / DAY_LENGTH * (Math.PI * 2);
        return new Libration(
            new Rotation(new Vector(1, 0, 0), Math.toRadians(LIBRATION_LATITUDE_AMPLITUDE) * Math.sin(phase)),
            new Rotation(new Vector(0, 0, 1), Math.toRadians(LIBRATION_LONGITUDE_AMPLITUDE) * Math.cos(phase))
        );
    }

    /**
     * 地球中心在月球本体坐标系中的位置。
     *
     * <p>无天平动时为 {@link #UP}；天平动让地球整体摆动，该点随之偏移，这是地球上看到的地球摆动。</p>
     */
    public static Vector earthCenter(long dayTime, double partialTick) {
        return libration(dayTime, partialTick).apply(UP);
    }

    /**
     * 地球本地法线到月球本体坐标系的完整变换，含地球自转、地轴朝向与天平动。
     *
     * <p>天平动对地球整体施加同一个刚体转动，故地轴朝向随之改变。</p>
     */
    public static Vector earthNormal(Vector local, long dayTime, double partialTick) {
        Rotation spin = earthSpin(dayTime, partialTick);
        return libration(dayTime, partialTick).apply(EARTH_ROTATION.apply(spin.apply(local)));
    }

    /**
     * 观察者在地球本地坐标系中的位置，供着色器把视线变换到地球本地坐标系。
     *
     * <p>观察者位于天平动的转轴上，是其不动点，故该位置与天平动无关，无需随时间重算。</p>
     */
    public static Vector observerInEarthFrame(long dayTime, double partialTick) {
        Rotation spin = earthSpin(dayTime, partialTick);
        return spin.inverse(EARTH_ROTATION.inverse(UP.scale(-1 / EARTH_PERSPECTIVE)));
    }

    private static Vector cubeCorner(int corner, double halfSize, Rotation spin) {
        Vector offset = EARTH_ROTATION.apply(spin.apply(cornerOffset(corner, halfSize)));
        return new Vector(offset.x, 1 + offset.y * EARTH_PERSPECTIVE, offset.z);
    }

    private static Vector cubeCorner(int corner, double halfSize, Rotation spin, Libration libration) {
        return libration.apply(cubeCorner(corner, halfSize, spin));
    }

    private static Vector cornerOffset(int corner, double halfSize) {
        return new Vector(
            (corner & 1) == 0 ? -halfSize : halfSize,
            (corner & 2) == 0 ? -halfSize : halfSize,
            (corner & 4) == 0 ? -halfSize : halfSize
        );
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

    public static Vector sunCorner(int corner, double scale) {
        Vector offset = SUN_ROTATION.apply(cornerOffset(corner, SUN_BODY_HALF_SIZE * scale));
        return new Vector(offset.x, 1 + offset.y * SUN_PERSPECTIVE, offset.z);
    }

    private static List<Point> sunSilhouette() {
        List<Point> points = new ArrayList<>(8);
        for (int corner = 0; corner < 8; corner++) {
            Vector point = sunCorner(corner, 1);
            points.add(new Point(point.x / point.y, point.z / point.y));
        }
        return List.copyOf(convexHull(points));
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
        List<Point> visible = clip(SUN_SILHOUETTE, horizontal, vertical, height);
        double area = area(visible);
        if (possibleEclipse) {
            List<Point> silhouette = earthSilhouette(sun, tangent, spin, libration(dayTime, partialTick));
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
        double fraction = area / SUN_DISC_AREA;
        return fraction < 1.0e-10 ? 0 : Math.min(fraction, 1);
    }

    /** 日面被主世界遮挡的凸多边形，供世界位置相关的月面光照裁切地平线。 */
    public static List<Vector> solarOcclusion(long dayTime, double partialTick) {
        Vector sun = referenceSun(dayTime, partialTick);
        if (sun.y < ECLIPSE_COSINE) return List.of();
        Vector tangent = new Vector(sun.y, -sun.x, 0);
        List<Point> silhouette = earthSilhouette(sun, tangent, earthSpin(dayTime, partialTick), libration(dayTime, partialTick));
        List<Point> covered = SUN_SILHOUETTE;
        for (int i = 0; i < silhouette.size(); i++) {
            Point first = silhouette.get(i);
            Point second = silhouette.get((i + 1) % silhouette.size());
            double dx = second.x - first.x;
            double dy = second.y - first.y;
            covered = clip(covered, -dy, dx, dy * first.x - dx * first.y);
        }
        return covered.stream().map(point -> new Vector(point.x, point.y, 0)).toList();
    }

    private static List<Point> earthSilhouette(Vector sun, Vector tangent, Rotation spin, Libration libration) {
        List<Point> points = new ArrayList<>(8);
        for (int corner = 0; corner < 8; corner++) {
            Vector point = cubeCorner(corner, EARTH_HALF_SIZE, spin, libration);
            double depth = point.dot(sun);
            points.add(new Point(point.dot(tangent) / depth, point.z / depth));
        }
        return convexHull(points);
    }

    private static List<Point> convexHull(List<Point> points) {
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
        Vector origin = observerInEarthFrame(dayTime, partialTick);
        Vector unshifted = libration(dayTime, partialTick).inverse(ray);
        Vector direction = spin.inverse(EARTH_ROTATION.inverse(
            new Vector(unshifted.x, unshifted.y / EARTH_PERSPECTIVE, unshifted.z)
        ));
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

    /**
     * 天平动，由两次绕正交轴的转动复合而成：{@code latitude} 负责南北摆动，{@code longitude} 负责东西摆动。
     *
     * <p>施加顺序固定为先生纬度后经度，与 {@link #libration} 的构造顺序一致。</p>
     */
    public record Libration(Rotation latitude, Rotation longitude) {
        public Vector apply(Vector vector) {
            return this.longitude.apply(this.latitude.apply(vector));
        }

        public Vector inverse(Vector vector) {
            return this.latitude.inverse(this.longitude.inverse(vector));
        }
    }
}
