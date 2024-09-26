package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.StructureToolScreen;
import dev.dubhe.anvilcraft.item.StructureToolItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import lombok.Getter;

public class StructureDataSyncPacket implements CustomPacketPayload {
    public static final Type<StructureDataSyncPacket> TYPE = new Type<>(AnvilCraft.of("structure_data_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureDataSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    StructureToolItem.StructureData.STREAM_CODEC,
                    StructureDataSyncPacket::getStructureData,
                    StructureDataSyncPacket::new);
    public static final IPayloadHandler<StructureDataSyncPacket> HANDLER = (packet, ctx) -> {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof StructureToolScreen structureToolScreen) {
            AnvilCraft.LOGGER.info("Send data to client screen");
            structureToolScreen.setStructureData(packet.getStructureData());
        }
    };

    @Getter
    private final StructureToolItem.StructureData structureData;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public StructureDataSyncPacket(StructureToolItem.StructureData structureData) {
        this.structureData = structureData;
    }
}
