package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.CelestialRenderingMode;

/** Independent failure latch for one optional celestial effect. */
final class CelestialRenderState {
    private volatile boolean loaded;
    private volatile boolean failed;

    void beginReload() {
        this.loaded = false;
        this.failed = false;
    }

    void completeReload() {
        if (!this.failed) this.loaded = true;
    }

    boolean standard(CelestialRenderingMode mode, boolean irisShaders) {
        return mode == CelestialRenderingMode.STANDARD && !this.failed && (this.loaded || irisShaders);
    }

    boolean fail() {
        if (this.failed) return false;
        this.failed = true;
        this.loaded = false;
        return true;
    }
}
