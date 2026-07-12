package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.impl.RedstoneWireTooltipProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record RedstoneWirePowerResponsePacket(BlockPos pos, int nonDustPower) implements IClientboundPacket {
    public static final Type<RedstoneWirePowerResponsePacket> TYPE = IPacket.type(
        AnvilCraft.of("redstone_wire_power_response")
    );
    public static final StreamCodec<ByteBuf, RedstoneWirePowerResponsePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        RedstoneWirePowerResponsePacket::pos,
        ByteBufCodecs.VAR_INT,
        RedstoneWirePowerResponsePacket::nonDustPower,
        RedstoneWirePowerResponsePacket::new
    );

    @Override
    public Type<RedstoneWirePowerResponsePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        RedstoneWireTooltipProvider.receive(player.level(), this.pos, this.nonDustPower);
    }
}
