package dev.dubhe.anvilcraft.network;


import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

@Getter
public class CyclingValueSyncPacket implements CustomPacketPayload {

    public static final Type<CyclingValueSyncPacket> TYPE = new Type<>(AnvilCraft.of("cycling_value"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CyclingValueSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, CyclingValueSyncPacket::getIndex,
            ByteBufCodecs.STRING_UTF8, CyclingValueSyncPacket::getName,
            CyclingValueSyncPacket::new
        );
    public static final IPayloadHandler<CyclingValueSyncPacket> HANDLER = CyclingValueSyncPacket::serverHandler;

    private final int index;
    private final String name;

    public CyclingValueSyncPacket(int index, String name) {
        this.index = index;
        this.name = name;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     *
     */
    public static void serverHandler(CyclingValueSyncPacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (player.containerMenu instanceof ItemCollectorMenu menu) {
            menu.notify(data.index, data.name);
        }
    }
}
