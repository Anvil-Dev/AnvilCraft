package dev.dubhe.anvilcraft.client.building;

/** 只平滑显示姿态，目标坐标和方块状态始终保留精确值。 */
final class BuildingRodAnimation {
    private static final double FOLLOW_RATE = 18.0;
    private boolean initialized;
    private long lastFrame;
    private double displayedX;
    private double displayedY;
    private double displayedZ;
    private double yaw;
    private double targetYaw;
    private double previousYaw;
    private double queuedYaw;
    private double mirrorX = 1;
    private double mirrorZ = 1;

    void clear() {
        this.initialized = false;
        this.queuedYaw = 0;
    }

    void turn(int quarterTurns) {
        this.queuedYaw -= quarterTurns * 90.0;
    }

    Pose sample(double x, double y, double z, double yaw, double mirrorX, double mirrorZ, long now) {
        if (!this.initialized) {
            this.initialized = true;
            this.displayedX = x;
            this.displayedY = y;
            this.displayedZ = z;
            this.yaw = yaw;
            this.targetYaw = yaw;
            this.mirrorX = mirrorX;
            this.mirrorZ = mirrorZ;
        } else {
            // 保留快速连续按旋转键的方向，避免累计超过半圈后反向追赶。
            this.targetYaw += this.queuedYaw + wrapDegrees(yaw - this.previousYaw - this.queuedYaw);
            double seconds = Math.clamp((now - this.lastFrame) / 1_000_000_000.0, 0.0, 0.1);
            double follow = -Math.expm1(-FOLLOW_RATE * seconds);
            this.displayedX = approach(this.displayedX, x, follow);
            this.displayedY = approach(this.displayedY, y, follow);
            this.displayedZ = approach(this.displayedZ, z, follow);
            this.yaw = approach(this.yaw, this.targetYaw, follow);
            this.mirrorX = approach(this.mirrorX, mirrorX, follow);
            this.mirrorZ = approach(this.mirrorZ, mirrorZ, follow);
        }
        this.previousYaw = yaw;
        this.queuedYaw = 0;
        this.lastFrame = now;
        return new Pose(this.displayedX, this.displayedY, this.displayedZ, (float) this.yaw,
            nonzeroScale(this.mirrorX), nonzeroScale(this.mirrorZ));
    }

    private static double approach(double current, double target, double follow) {
        double value = current + (target - current) * follow;
        return Math.abs(value - target) < 0.0001 ? target : value;
    }

    private static double wrapDegrees(double angle) {
        return angle - Math.floor((angle + 180.0) / 360.0) * 360.0;
    }

    private static float nonzeroScale(double scale) {
        // 镜像经过薄面时不能让法线矩阵退化为奇异矩阵。
        return (float) Math.copySign(Math.max(0.001, Math.abs(scale)), scale);
    }

    record Pose(double x, double y, double z, float yaw, float mirrorX, float mirrorZ) {
    }
}
