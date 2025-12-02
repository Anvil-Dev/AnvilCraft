package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.inventory.AdvancedComparatorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record AdvancedComparatorUpdatePacket(
    byte compareMode, boolean outputInvert,
    boolean redstoneControl,
    int highLimit, int lowLimit,
    int inputSignal
) implements CustomPacketPayload {
    public static final Type<AdvancedComparatorUpdatePacket> TYPE = new Type<>(AnvilCraft.of("advanced_comparator_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedComparatorUpdatePacket> STREAM_CODEC =
        StreamCodec.ofMember(AdvancedComparatorUpdatePacket::encode, AdvancedComparatorUpdatePacket::new);
    public static final IPayloadHandler<AdvancedComparatorUpdatePacket> HANDLER = AdvancedComparatorUpdatePacket::serverHandler;

    public AdvancedComparatorUpdatePacket(RegistryFriendlyByteBuf buf) {
        this(buf.readByte(), buf.readBoolean(),
            buf.readBoolean(),
            buf.readInt(), buf.readInt(),
            buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeByte(this.compareMode);
        buf.writeBoolean(this.outputInvert);
        buf.writeBoolean(this.redstoneControl);
        buf.writeInt(this.highLimit);
        buf.writeInt(this.lowLimit);
        buf.writeInt(this.inputSignal);
    }

    public static void serverHandler(AdvancedComparatorUpdatePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof AdvancedComparatorMenu menu)) return;
            AdvancedComparatorBlockEntity repeater = menu.getBlockEntity();
            repeater.setCompareMode(AdvancedComparatorBlockEntity.Mode.fromIndex(data.compareMode));
            repeater.setOutputInvert(data.outputInvert);
            repeater.setRedstoneControl(data.redstoneControl);
            repeater.setHighLimit(data.highLimit);
            repeater.setLowLimit(data.lowLimit);
            repeater.setInputtingSignal(data.inputSignal);
        });
    }
}
