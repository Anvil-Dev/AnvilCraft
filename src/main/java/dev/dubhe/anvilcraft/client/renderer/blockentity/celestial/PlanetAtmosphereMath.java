package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import org.joml.Vector3f;

/** Cube atmosphere integration shared by the portable renderer and its shader checks. */
public final class PlanetAtmosphereMath {
    public static final float HALF_SIZE = 0.5f;
    public static final float THICKNESS = 0.1875f;
    public static final float OUTER_SIZE = HALF_SIZE + THICKNESS;
    private static final int SAMPLES = 12;
    private static final int DENSITY_STEPS = 128;
    private static final float[] DENSITY = buildDensityTable();
    private static final float[] NEAR_GRID = {
        -OUTER_SIZE, -HALF_SIZE - THICKNESS * 0.65f, -HALF_SIZE - THICKNESS * 0.25f, -HALF_SIZE, -0.25f, 0,
        0.25f, HALF_SIZE, HALF_SIZE + THICKNESS * 0.25f, HALF_SIZE + THICKNESS * 0.65f, OUTER_SIZE
    };
    private static final float[] FAR_GRID = {
        -OUTER_SIZE, -HALF_SIZE - THICKNESS * 0.4f, -HALF_SIZE, 0, HALF_SIZE, HALF_SIZE + THICKNESS * 0.4f, OUTER_SIZE
    };

    private PlanetAtmosphereMath() {
    }

    static float[] grid(float distanceSquared) {
        return distanceSquared > 256.0f ? FAR_GRID : NEAR_GRID;
    }

    public static Haze sample(Vector3f origin, Vector3f direction, Vector3f light) {
        Interval outer = interval(origin, direction, OUTER_SIZE);
        float entry = Math.max(outer.near(), 0.0f);
        float exit = outer.far();
        Interval body = interval(origin, direction, HALF_SIZE);
        if (body.far() >= Math.max(body.near(), 0.0f)) exit = Math.min(exit, Math.max(body.near(), 0.0f));
        if (exit <= entry) return new Haze(0, 0);
        float step = (exit - entry) / SAMPLES;
        float column = 0;
        float scattering = 0;
        for (int index = 0; index < SAMPLES; index++) {
            float distance = entry + (index + 0.5f) * step;
            float x = origin.x + direction.x * distance;
            float y = origin.y + direction.y * distance;
            float z = origin.z + direction.z * distance;
            float altitude = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) - HALF_SIZE;
            float density = density(altitude);
            float normalLength = (float) Math.sqrt(x * x + y * y + z * z);
            float illumination = 0.38f + 0.62f * Math.max((x * light.x + y * light.y + z * light.z) / normalLength, 0.0f);
            column += density * step;
            scattering += density * step * illumination;
        }
        float opacity = (float) -Math.expm1(-column * 0.12f / (THICKNESS * 0.36f));
        return new Haze(scattering / Math.max(column, 1e-8f), opacity);
    }

    static float density(float altitude) {
        if (altitude <= 0) return 1;
        if (altitude >= THICKNESS) return 0;
        float position = altitude / THICKNESS * DENSITY_STEPS;
        int index = Math.min((int) position, DENSITY_STEPS - 1);
        return DENSITY[index] + (DENSITY[index + 1] - DENSITY[index]) * (position - index);
    }

    private static float[] buildDensityTable() {
        float[] table = new float[DENSITY_STEPS + 1];
        for (int index = 0; index <= DENSITY_STEPS; index++) table[index] = exactDensity(THICKNESS * index / DENSITY_STEPS);
        return table;
    }

    static float exactDensity(float altitude) {
        float edge = Math.clamp((altitude / THICKNESS - 0.65f) / 0.35f, 0, 1);
        return (float) Math.exp(-Math.max(altitude, 0) / (THICKNESS * 0.36f)) * (1 - edge * edge * (3 - 2 * edge));
    }

    private static Interval interval(Vector3f origin, Vector3f direction, float halfSize) {
        float near = Float.NEGATIVE_INFINITY;
        float far = Float.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            float start = origin.get(axis);
            float delta = direction.get(axis);
            if (Math.abs(delta) < 1e-8f) {
                if (Math.abs(start) > halfSize) return new Interval(1, -1);
                continue;
            }
            float first = (-halfSize - start) / delta;
            float second = (halfSize - start) / delta;
            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
        }
        return new Interval(near, far);
    }

    private record Interval(float near, float far) {
    }

    public record Haze(float light, float opacity) {
    }
}
