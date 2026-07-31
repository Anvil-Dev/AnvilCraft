package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record DeflectionRingUpdateLastSpeedPacket(BlockPos pos, double speed) implements IClientboundPacket {
    public static final Type<DeflectionRingUpdateLastSpeedPacket> TYPE = IPacket.type(AnvilCraft.of(
        "client_update_deflection_ring_last_speed"
    ));
    public static final StreamCodec<ByteBuf, DeflectionRingUpdateLastSpeedPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        DeflectionRingUpdateLastSpeedPacket::pos,
        ByteBufCodecs.DOUBLE,
        DeflectionRingUpdateLastSpeedPacket::speed,
        DeflectionRingUpdateLastSpeedPacket::new
    );

    @Override
    public Type<DeflectionRingUpdateLastSpeedPacket> type() {
        return DeflectionRingUpdateLastSpeedPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(player.level().getBlockEntity(this.pos) instanceof DeflectionRingBlockEntity deflectionRing)) return;
        deflectionRing.setLastEntitySpeed(this.speed);
    }
}
