package dev.dubhe.anvilcraft.client.renderer;

/** 阴影使用离散时间，太阳和面向光照仍按真实时间插值。 */
final class MunShadowClock {
    private long dayTime;
    private double partialTick;
    private long actualDayTime;
    private double actualPartialTick;
    private int direction;
    private boolean initialized;

    boolean update(long time, double partial, double sunHeight) {
        long whole = (long) Math.floor(partial);
        time += whole;
        partial -= whole;
        double elapsed = this.initialized ? (double) (time - this.actualDayTime) + partial - this.actualPartialTick : 0;
        int movement = elapsed > 0 ? 1 : (elapsed < 0 ? -1 : 0);
        final boolean discontinuity = !this.initialized || Math.abs(elapsed) > 20;
        boolean reset = discontinuity;
        int interval = sunHeight < 0.2 ? 1 : (sunHeight < 0.5 ? 2 : 4);
        long sample = time - Math.floorMod(time, interval);
        double fraction = sunHeight < 0.06 ? Math.floor(partial * 4) / 4 : 0;
        double advance = (double) (sample - this.dayTime) + fraction - this.partialTick;
        if (reset || movement > 0 && advance >= 0 || movement < 0 && advance <= 0) {
            int nextDirection = advance > 0 ? 1 : (advance < 0 ? -1 : 0);
            reset |= !discontinuity && nextDirection != 0 && this.direction != 0 && nextDirection != this.direction;
            this.dayTime = sample;
            this.partialTick = fraction;
            if (discontinuity) {
                this.direction = 0;
            } else if (nextDirection != 0) {
                this.direction = nextDirection;
            }
        }
        this.actualDayTime = time;
        this.actualPartialTick = partial;
        this.initialized = true;
        return reset;
    }

    long dayTime() {
        return this.dayTime;
    }

    double partialTick() {
        return this.partialTick;
    }

    void clear() {
        this.initialized = false;
        this.direction = 0;
    }
}
