package dev.dubhe.anvilcraft.worldgen;

/** Shared, side-neutral orbital positions and eclipse sampling for the overworld-like sky. */
public final class OverworldLikeOrbitMath {
    public static final int MAX_SKY_DARKEN = 6;
    private static final int ECLIPSE_SAMPLES = 64;
    private static final double DEGREES_PER_TICK_4 = 0.012D;
    private static final double DEGREES_PER_TICK_5 = -0.008D;
    private static final double DEGREES_PER_TICK_6 = 0.005D;
    private static final double THICKNESS_4 = 1.55D;
    private static final double THICKNESS_5 = 1.25D;
    private static final double THICKNESS_6 = 1.05D;
    private static final double OPACITY_4 = 0.78D;
    private static final double OPACITY_5 = 0.64D;
    private static final double OPACITY_6 = 0.52D;
    private static final double THRESHOLD = 0.08D;

    private OverworldLikeOrbitMath() {
    }

    public static RingPose ringPose(int ring, long gameTime, float partialTick, long orbitEpochGameTime, long visualSeed) {
        double elapsed = gameTime + partialTick - orbitEpochGameTime;
        double phase4 = phase(4, elapsed, visualSeed);
        double phase5 = phase(5, elapsed, visualSeed);
        double phase6 = phase(6, elapsed, visualSeed);
        return switch (ring) {
            case 4 -> new RingPose(phase6, phase5, phase4);
            case 5 -> new RingPose(phase6, phase5, 0.0D);
            case 6 -> new RingPose(phase6, 0.0D, 0.0D);
            default -> throw new IllegalArgumentException("Unsupported overworld-like ring: " + ring);
        };
    }

    public static float eclipseFactor(long gameTime, long dayTime, long orbitEpochGameTime, long visualSeed) {
        Vector3 sun = sunDirection(dayTime);
        double coverage = 1.0D;
        for (int ring = 4; ring <= 5; ring++) {
            double value = eclipseCoverage(ring, gameTime, orbitEpochGameTime, visualSeed, sun);
            coverage *= 1.0D - value;
        }
        return (float) clamp(1.0D - coverage, 0.0D, 1.0D);
    }

    public static int additionalSkyDarken(long gameTime, long dayTime, long orbitEpochGameTime, long visualSeed) {
        return Math.round(eclipseFactor(gameTime, dayTime, orbitEpochGameTime, visualSeed) * MAX_SKY_DARKEN);
    }

    private static double eclipseCoverage(int ring, long gameTime, long orbitEpochGameTime, long visualSeed, Vector3 sun) {
        RingPose pose = ringPose(ring, gameTime, 0.0F, orbitEpochGameTime, visualSeed);
        double thickness = switch (ring) {
            case 4 -> THICKNESS_4;
            case 5 -> THICKNESS_5;
            case 6 -> THICKNESS_6;
            default -> 0.0D;
        };
        double opacity = switch (ring) {
            case 4 -> OPACITY_4;
            case 5 -> OPACITY_5;
            case 6 -> OPACITY_6;
            default -> 0.0D;
        };
        double sampleHalfArc = 180.0D / ECLIPSE_SAMPLES;
        double closest = Double.POSITIVE_INFINITY;
        for (int sample = 0; sample < ECLIPSE_SAMPLES; sample++) {
            double angle = sample * 360.0D / ECLIPSE_SAMPLES;
            Vector3 point = transformRingPoint(ring, pose, angle);
            double distance = Math.toDegrees(Math.acos(clamp(point.dot(sun), -1.0D, 1.0D)));
            closest = Math.min(closest, distance);
        }
        double sampledCoverage = clamp((thickness + sampleHalfArc - closest) / sampleHalfArc, 0.0D, 1.0D);
        double effective = sampledCoverage * opacity;
        return effective <= THRESHOLD ? 0.0D : (effective - THRESHOLD) / (1.0D - THRESHOLD);
    }

    private static Vector3 transformRingPoint(int ring, RingPose pose, double angle) {
        Vector3 point = new Vector3(Math.cos(Math.toRadians(angle)), 0.0D, Math.sin(Math.toRadians(angle)));
        if (ring == 4) point = point.rotateZ(pose.innerRotation());
        if (ring <= 5) point = point.rotateX(90.0D + pose.middleRotation());
        point = point.rotateZ(14.5109D).rotateY(-3.8411D).rotateX(14.5108D);
        return point.rotateY(-pose.outerRotation()).normalize();
    }

    private static Vector3 sunDirection(long dayTime) {
        double fraction = Math.floorMod(dayTime, 24000L) / 24000.0D;
        double shifted = fraction - 0.25D;
        shifted -= Math.floor(shifted);
        double daylightCurve = 0.5D - Math.cos(shifted * Math.PI) / 2.0D;
        double timeOfDay = (shifted * 2.0D + daylightCurve) / 3.0D;
        double angle = timeOfDay * Math.PI * 2.0D;
        return new Vector3(-Math.sin(angle), Math.cos(angle), 0.0D);
    }

    private static double phase(int ring, double elapsed, long visualSeed) {
        double velocity = switch (ring) {
            case 4 -> DEGREES_PER_TICK_4;
            case 5 -> DEGREES_PER_TICK_5;
            case 6 -> DEGREES_PER_TICK_6;
            default -> throw new IllegalArgumentException("Unsupported overworld-like ring: " + ring);
        };
        return wrapDegrees(phaseOffset(visualSeed, ring) + elapsed * velocity);
    }

    private static double phaseOffset(long seed, int ring) {
        long mixed = seed ^ ((long) ring * 0x9E3779B97F4A7C15L);
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return Math.floorMod(mixed, 36000L) / 100.0D;
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0D;
        return wrapped < 0.0D ? wrapped + 360.0D : wrapped;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record RingPose(double outerRotation, double middleRotation, double innerRotation) {
    }

    private record Vector3(double x, double y, double z) {
        private Vector3 rotateX(double degrees) {
            double radians = Math.toRadians(degrees);
            double cosine = Math.cos(radians);
            double sine = Math.sin(radians);
            return new Vector3(x, y * cosine - z * sine, y * sine + z * cosine);
        }

        private Vector3 rotateY(double degrees) {
            double radians = Math.toRadians(degrees);
            double cosine = Math.cos(radians);
            double sine = Math.sin(radians);
            return new Vector3(x * cosine + z * sine, y, -x * sine + z * cosine);
        }

        private Vector3 rotateZ(double degrees) {
            double radians = Math.toRadians(degrees);
            double cosine = Math.cos(radians);
            double sine = Math.sin(radians);
            return new Vector3(x * cosine - y * sine, x * sine + y * cosine, z);
        }

        private double dot(Vector3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        private Vector3 normalize() {
            double length = Math.sqrt(x * x + y * y + z * z);
            return length == 0.0D ? this : new Vector3(x / length, y / length, z / length);
        }
    }
}
