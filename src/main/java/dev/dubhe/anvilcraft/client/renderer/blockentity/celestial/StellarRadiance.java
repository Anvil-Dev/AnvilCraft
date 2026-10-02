package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import org.joml.Vector3f;

/** Stellar exposure and corona geometry, independent of world lighting and gameplay energy. */
public final class StellarRadiance {
    public static final float MAX_EXPOSURE = 24.0f;
    public static final float MAX_HALO_SCALE = 2.4f;
    public static final float BROWN_DWARF_SURFACE_GLOW = 0.05f;
    public static final float BROWN_DWARF_HALO_SCALE = 1.25f;
    public static final float BROWN_DWARF_HALO_ALPHA = 0.12f;

    private StellarRadiance() {
    }

    public static float exposure(float temperature, float luminosity, float emission) {
        double thermal = Math.pow(finitePositive(temperature) / 5772.0, 2.0);
        double light = Math.log1p(finitePositive(luminosity));
        double activity = Math.log1p(finitePositive(emission));
        double relativeFlux = Math.clamp(thermal / (1.0 + thermal / 12.0) * (1.0 + 0.12 * light)
            + 0.15 * light + 0.12 * activity, 0.04, 16.0);
        // Even a cool photosphere exceeds display white; temperature controls the remaining exposure and corona.
        return 4.0f + 3.0f * (float) Math.sqrt(relativeFlux);
    }

    public static float eventExposure(float exposure, float emission) {
        return Math.clamp(exposure + 2.0f * (float) Math.log1p(finitePositive(emission)), 0.04f, MAX_EXPOSURE);
    }

    public static float haloScale(float exposure) {
        return 1.45f + (MAX_HALO_SCALE - 1.45f) * (float) Math.log1p(exposure) / (float) Math.log1p(MAX_EXPOSURE);
    }

    public static float haloStrength(float exposure) {
        return 0.65f * (float) Math.log1p(exposure);
    }

    public static float surfaceGain(float temperature) {
        float warm = Math.clamp((temperature - 3200.0f) / 800.0f, 0.0f, 1.0f);
        return 0.94f + 0.06f * warm * warm * (3.0f - 2.0f * warm);
    }

    public static float rimBoost(float temperature, float exposure) {
        float hot = Math.clamp((temperature - 6500.0f) / 3500.0f, 0.0f, 1.0f);
        float cool = 1.0f - hot * hot * (3.0f - 2.0f * hot);
        return Math.max(0.0f, 0.96f * surfaceGain(temperature) - haloStrength(exposure) * 0.45f) * cool;
    }

    public static float haloAlpha(float edgeAlpha, float rimBoost, float progress) {
        float remaining = Math.clamp(1.0f - progress, 0.0f, 1.0f);
        return Math.clamp(edgeAlpha * remaining * remaining * remaining
            + rimBoost * (float) Math.pow(remaining, 12), 0.0f, 1.0f);
    }

    public static float toneMap(float channel, float exposure) {
        return (float) Math.sqrt(1.0 - Math.exp(-Math.max(0.0f, channel) * exposure));
    }

    public static void normalizeColor(float[] color) {
        float peak = Math.max(0.00001f, Math.max(color[0], Math.max(color[1], color[2])));
        for (int channel = 0; channel < 3; channel++) color[channel] /= peak;
    }

    public static float[] coreColor(float[] color, float temperature, float exposure) {
        double thermal = Math.clamp(Math.log(Math.max(4000.0f, finitePositive(temperature)) / 4000.0) / Math.log(5.5), 0.0, 1.0);
        double hot = thermal * thermal * (3.0 - 2.0 * thermal);
        float burst = Math.clamp((exposure - 16.0f) / 8.0f, 0.0f, 1.0f);
        float white = 0.015f + 0.805f * (float) hot + 0.08f * burst;
        // Only hot photospheres approach display white; the corona keeps the unmodified temperature color.
        return new float[] {
            color[0] + (1.0f - color[0]) * white,
            color[1] + (1.0f - color[1]) * white,
            color[2] + (1.0f - color[2]) * white
        };
    }

    public static float encodeExposure(float exposure) {
        return (float) (Math.log1p(exposure) / Math.log1p(MAX_EXPOSURE));
    }

    static boolean silhouetteEdge(Vector3f camera, int corner, int axis) {
        int first = (axis + 1) % 3;
        int second = (axis + 2) % 3;
        boolean firstFacing = (corner & (1 << first)) == 0 ? camera.get(first) < 0 : camera.get(first) > 1;
        boolean secondFacing = (corner & (1 << second)) == 0 ? camera.get(second) < 0 : camera.get(second) > 1;
        return firstFacing != secondFacing;
    }

    private static float finitePositive(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
