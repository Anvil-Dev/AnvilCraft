package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.StructureToolScreen;
import dev.dubhe.anvilcraft.item.property.component.StructureData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record StructureDataSyncPacket(StructureData structureData) implements IClientboundPacket {
    public static final Type<StructureDataSyncPacket> TYPE = new Type<>(AnvilCraft.of("structure_data_sync"));
    public static final StreamCodec<ByteBuf, StructureDataSyncPacket> STREAM_CODEC = StreamCodec.composite(
        StructureData.STREAM_CODEC,
        StructureDataSyncPacket::structureData,
        StructureDataSyncPacket::new
    );

    @Override
    public Type<StructureDataSyncPacket> type() {
        return StructureDataSyncPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof StructureToolScreen structureToolScreen) {
            AnvilCraft.LOGGER.info("Send data to client screen");
            structureToolScreen.setStructureData(this.structureData());
        }
    }
}
