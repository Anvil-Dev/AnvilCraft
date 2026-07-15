package dev.dubhe.anvilcraft.util;

public class TickDebouncer {
    private final Runnable action;
    private final int delayTicks;
    private int remainingTicks = -1;

    public TickDebouncer(int delayTicks, Runnable action) {
        this.delayTicks = delayTicks;
        this.action = action;
    }

    public void trigger() {
        this.remainingTicks = this.delayTicks;
    }

    public void tick() {
        if (this.remainingTicks > 0) {
            this.remainingTicks--;
            if (this.remainingTicks == 0) {
                this.action.run();
            }
        }
    }

    public void run() {
        if (this.remainingTicks > 0) {
            this.remainingTicks = 0;
            this.action.run();
        }
    }
}
