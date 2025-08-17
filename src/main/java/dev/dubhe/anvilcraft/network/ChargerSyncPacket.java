package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record ChargerSyncPacket(BlockPos pos, int timeLeft, int timeTotal) implements CustomPacketPayload {
    public static final Type<ChargerSyncPacket> TYPE = new Type<>(AnvilCraft.of("charger_sync"));
    public static final StreamCodec<ByteBuf, ChargerSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ChargerSyncPacket::pos,
        ByteBufCodecs.VAR_INT, ChargerSyncPacket::timeLeft,
        ByteBufCodecs.VAR_INT, ChargerSyncPacket::timeTotal,
        ChargerSyncPacket::new
    );
    public static final IPayloadHandler<ChargerSyncPacket> HANDLER = ChargerSyncPacket::clientHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void clientHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().getBlockEntity(this.pos) instanceof ChargerBlockEntity charger) {
                charger.setTimeLeft(this.timeLeft);
                charger.setTimeTotalCache(this.timeTotal);
            }
        });
    }
}
