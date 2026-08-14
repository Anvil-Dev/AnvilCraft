package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.client.gui.screen.CreativeLaserScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record CreativeLaserInitPacket(int level, LensType lensType, boolean gamma) implements IClientboundPacket {
    public static final Type<CreativeLaserInitPacket> TYPE = IPacket.type(AnvilCraft.of("creative_laser_init"));
    public static final StreamCodec<ByteBuf, CreativeLaserInitPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        CreativeLaserInitPacket::level,
        ByteBufCodecs.STRING_UTF8.map(LensType::valueOf, LensType::name),
        CreativeLaserInitPacket::lensType,
        ByteBufCodecs.BOOL,
        CreativeLaserInitPacket::gamma,
        CreativeLaserInitPacket::new
    );

    @Override
    public Type<CreativeLaserInitPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof CreativeLaserScreen screen)) return;
        screen.setValue(this.level, this.lensType, this.gamma);
    }
}
