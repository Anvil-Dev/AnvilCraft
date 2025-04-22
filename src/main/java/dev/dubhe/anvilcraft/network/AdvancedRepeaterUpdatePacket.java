package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AdvancedRepeaterBlockEntity;
import dev.dubhe.anvilcraft.inventory.AdvancedRepeaterMenu;
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
public record AdvancedRepeaterUpdatePacket(
    byte startMode, boolean outputInvert, int waitingTime, int signalDuration
) implements CustomPacketPayload {
    public static final Type<AdvancedRepeaterUpdatePacket> TYPE = new Type<>(AnvilCraft.of("advanced_repeater_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedRepeaterUpdatePacket> STREAM_CODEC =
        StreamCodec.ofMember(AdvancedRepeaterUpdatePacket::encode, AdvancedRepeaterUpdatePacket::new);
    public static final IPayloadHandler<AdvancedRepeaterUpdatePacket> HANDLER = AdvancedRepeaterUpdatePacket::serverHandler;

    public AdvancedRepeaterUpdatePacket(RegistryFriendlyByteBuf buf) {
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

    public static void serverHandler(AdvancedRepeaterUpdatePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof AdvancedRepeaterMenu menu)) return;
            AdvancedRepeaterBlockEntity repeater = menu.getBlockEntity();
            repeater.setStartMode(data.startMode);
            repeater.setOutputInvert(data.outputInvert);
            repeater.setWaitingTime(data.waitingTime);
            repeater.setSignalDuration(data.signalDuration);
        });
    }
}
