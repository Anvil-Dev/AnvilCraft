package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.ScreenShakeManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 屏幕（摄像机）震动包（Server → Client）。
 *
 * <p>
 * 告诉客户端在指定中心、指定半径内播放摄像机震动。振幅由客户端按距离衰减。
 * {@code shakeOrdinal} 为 {@link ShakeType} 的序号，决定振幅/时长/是否需站地面。
 * 由超新星爆发、巨型铁砧撼地等共用。
 * </p>
 */
public record ScreenShakePacket(double x, double y, double z, float radius, int shakeOrdinal)
    implements IClientboundPacket {

    /// 震动类型：决定时长、振幅、以及是否要求玩家站在地面上才震。
    /// 定义在通用（非客户端）代码中，以便服务端发包时引用，避免触发客户端类加载。
    public enum ShakeType {
        /// 超新星爆发：幅度较大、持续较久（约 1.2 秒），空中也会震（冲击波）。
        SUPERNOVA(24, 0.6f, 0.9f, 1.7f, false),
        /// 巨型铁砧撼地：幅度更小、结束更快（约 0.4 秒），仅当玩家站在地面上才震。
        GIANT_ANVIL_SHOCK(8, 0.28f, 0.4f, 2.2f, true);

        public final int durationTicks;
        public final float yawPitchAmplitude;
        public final float rollAmplitude;
        public final float frequency;
        public final boolean requireOnGround;

        ShakeType(int durationTicks, float yawPitchAmplitude, float rollAmplitude,
                  float frequency, boolean requireOnGround) {
            this.durationTicks = durationTicks;
            this.yawPitchAmplitude = yawPitchAmplitude;
            this.rollAmplitude = rollAmplitude;
            this.frequency = frequency;
            this.requireOnGround = requireOnGround;
        }
    }

    public static final Type<ScreenShakePacket> TYPE =
        IPacket.type(AnvilCraft.of("screen_shake"));

    public static final StreamCodec<ByteBuf, ScreenShakePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE,
        ScreenShakePacket::x,
        ByteBufCodecs.DOUBLE,
        ScreenShakePacket::y,
        ByteBufCodecs.DOUBLE,
        ScreenShakePacket::z,
        ByteBufCodecs.FLOAT,
        ScreenShakePacket::radius,
        ByteBufCodecs.VAR_INT,
        ScreenShakePacket::shakeOrdinal,
        ScreenShakePacket::new
    );

    /// 便捷构造：从中心 Vec3、半径、震动类型创建包。
    public static ScreenShakePacket of(Vec3 center, float radius, ShakeType type) {
        return new ScreenShakePacket(center.x, center.y, center.z, radius, type.ordinal());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        ShakeType[] types = ShakeType.values();
        if (this.shakeOrdinal < 0 || this.shakeOrdinal >= types.length) return;
        ScreenShakeManager.getInstance().trigger(new Vec3(this.x, this.y, this.z), this.radius, types[this.shakeOrdinal]);
    }
}
