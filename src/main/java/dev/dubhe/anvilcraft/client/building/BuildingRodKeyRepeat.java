package dev.dubhe.anvilcraft.client.building;

import java.util.Arrays;

/** 首击立即执行，持续按住逐渐加速；物理状态轮询与原始事件共用去重状态。 */
final class BuildingRodKeyRepeat {
    private static final long INITIAL_DELAY = 180_000_000L;
    private static final double INITIAL_SPEED = 8.0;
    private static final double MAX_SPEED = 40.0;
    private static final double ACCELERATION_SECONDS = 3.0;
    private static final int MAX_STEPS_PER_UPDATE = 4;
    private final boolean[] held = new boolean[9];
    private final long[] started = new long[9];
    private final long[] emitted = new long[9];

    boolean press(int index, long now) {
        if (this.held[index]) return false;
        this.held[index] = true;
        this.started[index] = now;
        this.emitted[index] = 0;
        return true;
    }

    boolean isHeld(int index) {
        return this.held[index];
    }

    void release(int index) {
        this.held[index] = false;
    }

    int update(int index, boolean down, long now) {
        if (!down) {
            this.release(index);
            return 0;
        }
        if (this.press(index, now)) return 1;
        return this.repeatSteps(index, now);
    }

    private int repeatSteps(int index, long now) {
        if (index >= 6 || !this.held[index]) return 0;
        long elapsed = now - this.started[index] - INITIAL_DELAY;
        if (elapsed < 0) return 0;
        double seconds = elapsed / 1_000_000_000.0;
        double accelerating = Math.min(seconds, ACCELERATION_SECONDS);
        double acceleration = (MAX_SPEED - INITIAL_SPEED) / ACCELERATION_SECONDS;
        long total = 1 + (long) Math.floor(INITIAL_SPEED * accelerating
            + 0.5 * acceleration * accelerating * accelerating
            + MAX_SPEED * Math.max(0, seconds - ACCELERATION_SECONDS));
        long steps = Math.max(0, total - this.emitted[index]);
        this.emitted[index] = total;
        // 卡顿后丢弃过期位移，不在恢复帧瞬间补发一大段移动。
        return (int) Math.min(MAX_STEPS_PER_UPDATE, steps);
    }

    void clear() {
        Arrays.fill(this.held, false);
    }
}
