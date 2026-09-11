package dev.dubhe.anvilcraft.client.renderer.mun;

import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;

final class MunRenderState {
    private volatile MunLightingQuality quality = MunLightingQuality.STANDARD;
    private volatile boolean failed;
    private volatile boolean loaded;

    boolean configure(MunLightingQuality configured) {
        boolean changed = (this.quality == MunLightingQuality.OFF) != (configured == MunLightingQuality.OFF);
        if (changed) {
            this.loaded = false;
            if (configured != MunLightingQuality.OFF) this.failed = false;
        }
        this.quality = configured;
        return changed;
    }

    boolean requested() {
        return !this.failed && this.quality != MunLightingQuality.OFF;
    }

    boolean enabled() {
        return this.requested() && this.loaded;
    }

    void beginReload(MunLightingQuality configured) {
        this.configure(configured);
        this.loaded = false;
    }

    void completeReload() {
        this.loaded = this.requested();
    }

    boolean fail() {
        if (this.failed) return false;
        this.failed = true;
        this.loaded = false;
        this.quality = MunLightingQuality.OFF;
        return true;
    }
}
