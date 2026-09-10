package dev.dubhe.anvilcraft.client.renderer;

import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;

/** 额外帧耗时的设计目标分配到网格构建、阴影和后处理，不把毫秒数当作跨硬件保证。 */
record MunLightingProfile(
    int cascades, int resolution, int distance, int cacheRadius, int chunkVertices, int cacheVertices,
    int dynamicVertices, int dynamicEntities, int dynamicDistance, long buildBudgetNanos,
    int effectDownsample, int aoSamples, float aoRadius, float aoStrength, float glareStrength, float ambientFloor
) {
    private static final MunLightingProfile POTATO = new MunLightingProfile(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        4, 0, 0, 0, 0.045F, 0.42F
    );
    private static final MunLightingProfile STANDARD = new MunLightingProfile(
        2, 2048, 128, 192, 32768, 1_500_000, 49152, 64, 64, 500_000,
        2, 12, 0.9F, 0.65F, 0.065F, 0.30F
    );

    static MunLightingProfile of(MunLightingQuality quality) {
        return switch (quality) {
            case POTATO -> POTATO;
            case STANDARD, ULTRA -> STANDARD;
        };
    }

    float radius(int cascade) {
        if (cascade >= this.cascades - 1) return this.distance;
        return cascade == 0 ? 32 : 96;
    }
}
