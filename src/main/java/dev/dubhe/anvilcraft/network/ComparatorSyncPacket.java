package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;

public record ComparatorSyncPacket(BlockPos pos, int output) implements IClientboundPacket {
    public static final Type<ComparatorSyncPacket> TYPE = IPacket.type(AnvilCraft.of("comparator_sync"));
    public static final StreamCodec<ByteBuf, ComparatorSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ComparatorSyncPacket::pos,
        ByteBufCodecs.VAR_INT,
        ComparatorSyncPacket::output,
        ComparatorSyncPacket::new
    );

    @Override
    public Type<ComparatorSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        Util.castSafely(player.level().getBlockEntity(this.pos), ComparatorBlockEntity.class)
            .ifPresent(entity -> entity.setOutputSignal(this.output));
    }
}
