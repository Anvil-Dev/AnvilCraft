package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.Objects;

public record HeatableSyncPacket(BlockPos pos, int duration) implements CustomPacketPayload {
    public static final Type<HeatableSyncPacket> TYPE = new Type<>(AnvilCraft.of("heatable_sync"));
    public static final StreamCodec<ByteBuf, HeatableSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, HeatableSyncPacket::pos,
        ByteBufCodecs.VAR_INT, HeatableSyncPacket::duration,
        HeatableSyncPacket::new
    );
    public static final IPayloadHandler<HeatableSyncPacket> HANDLER = HeatableSyncPacket::clientHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void clientHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> Util.castSafely(
            Objects.requireNonNull(Minecraft.getInstance().level).getBlockEntity(this.pos),
            HeatableBlockEntity.class).ifPresent(heatable -> heatable.setDuration(this.duration)));
    }
}
