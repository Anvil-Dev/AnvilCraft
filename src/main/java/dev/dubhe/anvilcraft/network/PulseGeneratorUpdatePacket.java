package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.inventory.PulseGeneratorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PulseGeneratorUpdatePacket(
    byte startMode,
    boolean outputInvert,
    int waitingTime,
    int signalDuration
) implements IServerboundPacket {
    public static final Type<PulseGeneratorUpdatePacket> TYPE = IPacket.type(AnvilCraft.of("advanced_repeater_update"));
    public static final StreamCodec<ByteBuf, PulseGeneratorUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE,
        PulseGeneratorUpdatePacket::startMode,
        ByteBufCodecs.BOOL,
        PulseGeneratorUpdatePacket::outputInvert,
        ByteBufCodecs.VAR_INT,
        PulseGeneratorUpdatePacket::waitingTime,
        ByteBufCodecs.VAR_INT,
        PulseGeneratorUpdatePacket::signalDuration,
        PulseGeneratorUpdatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PulseGeneratorUpdatePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof PulseGeneratorMenu menu)) return;
        PulseGeneratorBlockEntity repeater = menu.getBlockEntity();
        repeater.setStartMode(this.startMode);
        repeater.setOutputMode(this.outputInvert);
        repeater.setWaitingTime(this.waitingTime);
        repeater.setSignalDuration(this.signalDuration);
        repeater.syncToClient();
    }
}
