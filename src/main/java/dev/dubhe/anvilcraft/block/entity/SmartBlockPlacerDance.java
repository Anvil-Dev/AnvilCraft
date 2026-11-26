package dev.dubhe.anvilcraft.block.entity;

/** 只累计通电的游戏刻，使展示中的机械臂断电定格、恢复供电后继续跳舞。 */
public final class SmartBlockPlacerDance {
    private long ticks;
    private boolean playing;

    public void tick(boolean powered) {
        this.playing = powered;
        if (powered) {
            this.ticks++;
        }
    }

    public double getRenderTime(float partialTick, boolean powered) {
        return this.ticks - (this.playing && powered ? 1d - partialTick : 0);
    }
}
