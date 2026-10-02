package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.StructureScannerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record StructureScannerStatusPacket(int containerId, Component message) implements IClientboundPacket {
    public static final Type<StructureScannerStatusPacket> TYPE = IPacket.type(AnvilCraft.of("structure_scanner_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureScannerStatusPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, StructureScannerStatusPacket::containerId,
        ComponentSerialization.STREAM_CODEC, StructureScannerStatusPacket::message,
        StructureScannerStatusPacket::new
    );

    @Override
    public Type<StructureScannerStatusPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (Minecraft.getInstance().screen instanceof StructureScannerScreen screen && screen.getMenu().containerId == this.containerId) {
            screen.showStatus(this.message);
        }
    }
}
