package dev.dubhe.anvilcraft.client.renderer.mun;

import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;

record MunLightingProfile(
    int cascades, int resolution, int distance, int cacheRadius, int chunkVertices, int cacheVertices,
    int dynamicVertices, int dynamicEntities, int dynamicDistance, long buildBudgetNanos,
    boolean entityShadows, boolean translucentShadows,
    int effectDownsample, int aoSamples, float aoRadius, float aoStrength, float glareStrength, float ambientFloor
) {
    private static final MunLightingProfile POTATO = new MunLightingProfile(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false,
        2, 12, 0.9F, 0.65F, 0.065F, 0.30F
    );
    private static final MunLightingProfile STANDARD = new MunLightingProfile(
        3, 2048, 256, 320, 32768, 1_500_000, 49152, 64, 64, 500_000, true, true,
        2, 12, 0.9F, 0.65F, 0.065F, 0.30F
    );
    private static final MunLightingProfile OFF = new MunLightingProfile(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false,
        1, 0, 0, 0, 0, 0
    );

    static MunLightingProfile of(MunLightingQuality quality) {
        return switch (quality) {
            case POTATO -> POTATO;
            case STANDARD -> STANDARD;
            case OFF -> OFF;
        };
    }

    float radius(int cascade) {
        if (cascade >= this.cascades - 1) return this.distance;
        return cascade == 0 ? 32 : 128;
    }

    int resolution(int cascade) {
        return cascade == 2 ? Math.min(this.resolution, 1024) : this.resolution;
    }
}
