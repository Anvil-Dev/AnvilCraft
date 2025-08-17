package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockInfo;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockSection;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.List;

public record SlidingEntitySyncPacket(int id, List<SlidingBlockInfo> infos, Direction moveDirection) implements CustomPacketPayload {
    public static final Type<SlidingEntitySyncPacket> TYPE = new Type<>(AnvilCraft.of("sliding_entity_sync"));
    public static final StreamCodec<ByteBuf, SlidingEntitySyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SlidingEntitySyncPacket::id,
        SlidingBlockInfo.SIMPLE_STREAM_CODEC.apply(ByteBufCodecs.list()), SlidingEntitySyncPacket::infos,
        Direction.STREAM_CODEC, SlidingEntitySyncPacket::moveDirection,
        SlidingEntitySyncPacket::new
    );
    public static final IPayloadHandler<SlidingEntitySyncPacket> HANDLER = SlidingEntitySyncPacket::clientHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void clientHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> Util.castSafely(ctx.player().level().getEntity(this.id), SlidingBlockEntity.class)
            .ifPresent(entity -> {
                entity.setSection(new SlidingBlockSection(this.infos));
                entity.setMoveDirection(this.moveDirection);
            }));
    }
}
