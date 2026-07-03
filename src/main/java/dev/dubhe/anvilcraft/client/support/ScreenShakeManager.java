package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.network.ScreenShakePacket.ShakeType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/// 屏幕（摄像机）震动管理器 —— 纯客户端。
/// 通过 ViewportEvent.ComputeCameraAngles 往 yaw/pitch/roll 上叠加噪声实现，
/// 不改动世界状态。由超新星爆发、巨型铁砧撼地等事件触发，各自有不同的振幅/时长参数
/// （见 {@link ShakeType}）。
public class ScreenShakeManager {

    private static final ScreenShakeManager INSTANCE = new ScreenShakeManager();

    public static ScreenShakeManager getInstance() {
        return INSTANCE;
    }

    /// 触发时的强度（0 表示未激活）。剩余刻数随客户端 tick 递减。
    private float intensity = 0f;
    private int remainingTicks = 0;
    private ShakeType activeType = ShakeType.SUPERNOVA;
    private final RandomSource random = RandomSource.create();
    /// 每次触发用随机相位，使不同次震动互不相同。
    private float phaseSeed = 0f;

    /// 触发一次以 center 为中心、radius 为半径、指定类型的屏幕震动。
    /// 振幅按本地玩家到中心的距离线性衰减，范围外不震动。
    /// 若该类型要求站在地面（撼地），则玩家不在地面上时不震。
    public void trigger(Vec3 center, float radius, ShakeType type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (type.requireOnGround && !mc.player.onGround()) return;
        double dist = mc.player.position().distanceTo(center);
        if (dist > radius) return;
        float falloff = (float) (1.0 - dist / radius);
        falloff = Mth.clamp(falloff, 0f, 1f);
        if (falloff <= 0.01f) return;
        // 取较强的一次（避免连续触发互相削弱）
        if (falloff >= this.intensity || this.remainingTicks <= 0) {
            this.intensity = falloff;
            this.activeType = type;
            this.remainingTicks = type.durationTicks;
            this.phaseSeed = this.random.nextFloat() * 1000f;
        }
    }

    /// 客户端每刻递减剩余时长。
    public void tick() {
        if (this.remainingTicks > 0) {
            this.remainingTicks--;
            if (this.remainingTicks <= 0) {
                this.intensity = 0f;
            }
        }
    }

    public boolean isActive() {
        return this.remainingTicks > 0 && this.intensity > 0.01f;
    }

    /// 当前帧的衰减系数（含 partialTick 平滑），随剩余时长线性消退。
    private float currentFalloff(float partialTick) {
        float t = (this.remainingTicks - partialTick) / this.activeType.durationTicks;
        return Mth.clamp(t, 0f, 1f) * this.intensity;
    }

    /// 计算当前帧应叠加到摄像机的 yaw/pitch/roll 偏移（度）。
    /// 使用多个不同频率的正弦叠加，产生不规则的快速颤动而非规则摆动。
    public float[] computeAngleOffsets(float partialTick) {
        if (!this.isActive()) return null;
        float falloff = this.currentFalloff(partialTick);
        if (falloff <= 0.01f) return null;

        Minecraft mc = Minecraft.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null) return null;

        // 基于客户端时间推进的相位（含 partialTick），频率较高以快速颤动。
        float gameTime = cam.tickCount + partialTick;
        float phase = this.phaseSeed + gameTime * this.activeType.frequency;

        float yaw = (float) (Math.sin(phase * 1.3) + 0.5 * Math.sin(phase * 2.7))
            * this.activeType.yawPitchAmplitude * falloff;
        float pitch = (float) (Math.sin(phase * 1.1 + 1.7) + 0.5 * Math.sin(phase * 3.1))
            * this.activeType.yawPitchAmplitude * falloff;
        float roll = (float) (Math.sin(phase * 0.9 + 0.6) + 0.5 * Math.sin(phase * 2.3))
            * this.activeType.rollAmplitude * falloff;
        return new float[]{yaw, pitch, roll};
    }
}
