package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record ComparatorSyncPacket(BlockPos pos, int output) implements CustomPacketPayload {
    public static final Type<ComparatorSyncPacket> TYPE = new Type<>(AnvilCraft.of("comparator_sync"));
    public static final StreamCodec<ByteBuf, ComparatorSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ComparatorSyncPacket::pos,
        ByteBufCodecs.VAR_INT, ComparatorSyncPacket::output,
        ComparatorSyncPacket::new
    );
    public static final IPayloadHandler<ComparatorSyncPacket> HANDLER = ComparatorSyncPacket::clientHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void clientHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Util.castSafely(ctx.player().level().getBlockEntity(this.pos), ComparatorBlockEntity.class)
                .ifPresent(entity -> entity.setOutputSignal(this.output));
        });
    }
}
