package dev.dubhe.anvilcraft.client.building;

final class BuildingRodMotion {
    private double lastTime = Double.NaN;
    private double speed;
    private double angle;
    private double aim;
    private double placedAt = Double.NEGATIVE_INFINITY;
    private double knockedAt = Double.NEGATIVE_INFINITY;

    void placed(double time) {
        this.placedAt = time;
    }

    void knock(double time) {
        this.knockedAt = time;
        this.aim = 0;
    }

    boolean isKnocking(double time) {
        return time - this.knockedAt < 0.4;
    }

    void stopKnock() {
        this.knockedAt = Double.NEGATIVE_INFINITY;
    }

    Pose sample(double time, boolean powered, boolean charging, boolean attacking) {
        double seconds = Double.isNaN(this.lastTime) ? 0 : Math.clamp(time - this.lastTime, 0, 0.1);
        this.lastTime = time;
        double targetSpeed = powered ? charging ? 1080 : 90 : 0;
        double decay = Math.exp(-5 * seconds);
        if (powered) {
            // 对平滑速度积分，避免不同帧率下旋转角度和加速时间发生偏移。
            this.angle = (this.angle + targetSpeed * seconds + (this.speed - targetSpeed) * (1 - decay) / 5) % 360;
            this.speed = targetSpeed + (this.speed - targetSpeed) * decay;
        } else {
            this.speed = 0;
        }
        this.aim += ((attacking ? 1 : 0) - this.aim) * -Math.expm1(-18 * seconds);
        double progress = Math.clamp((time - this.placedAt) / 0.28, 0, 1);
        double kick = progress < 0.2 ? smooth(progress / 0.2) : 1 - smooth((progress - 0.2) / 0.8);
        double swing = Math.clamp((time - this.knockedAt) / 0.4, 0, 1);
        double knock = swing < 0.25 ? -0.22 * smooth(swing * 4)
            : swing < 0.5 ? -0.22 + 1.22 * smooth((swing - 0.25) * 4) : 1 - smooth((swing - 0.5) * 2);
        return new Pose((float) this.angle, (float) this.aim, (float) kick, (float) knock);
    }

    private static double smooth(double value) {
        return value * value * (3 - 2 * value);
    }

    record Pose(float angle, float aim, float kick, float knock) {
    }
}
