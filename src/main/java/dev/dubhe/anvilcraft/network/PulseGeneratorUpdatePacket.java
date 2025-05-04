package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.inventory.PulseGeneratorMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record PulseGeneratorUpdatePacket(
    byte startMode, boolean outputInvert, int waitingTime, int signalDuration
) implements CustomPacketPayload {
    public static final Type<PulseGeneratorUpdatePacket> TYPE = new Type<>(AnvilCraft.of("advanced_repeater_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PulseGeneratorUpdatePacket> STREAM_CODEC =
        StreamCodec.ofMember(PulseGeneratorUpdatePacket::encode, PulseGeneratorUpdatePacket::new);
    public static final IPayloadHandler<PulseGeneratorUpdatePacket> HANDLER = PulseGeneratorUpdatePacket::serverHandler;

    public PulseGeneratorUpdatePacket(RegistryFriendlyByteBuf buf) {
        this(buf.readByte(), buf.readBoolean(), buf.readInt(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeByte(this.startMode);
        buf.writeBoolean(this.outputInvert);
        buf.writeInt(this.waitingTime);
        buf.writeInt(this.signalDuration);
    }

    public static void serverHandler(PulseGeneratorUpdatePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof PulseGeneratorMenu menu)) return;
            PulseGeneratorBlockEntity repeater = menu.getBlockEntity();
            repeater.setStartMode(data.startMode);
            repeater.setOutputInvert(data.outputInvert);
            repeater.setWaitingTime(data.waitingTime);
            repeater.setSignalDuration(data.signalDuration);
        });
    }
}
