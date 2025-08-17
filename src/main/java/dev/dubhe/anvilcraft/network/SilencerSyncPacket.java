package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.ArrayList;
import java.util.List;

public record SilencerSyncPacket(BlockPos pos, List<ResourceLocation> sounds) implements CustomPacketPayload {
    public static final Type<SilencerSyncPacket> TYPE = new Type<>(AnvilCraft.of("silencer_sync"));
    public static final IPayloadHandler<SilencerSyncPacket> HANDLER = SilencerSyncPacket::handler;
    public static final StreamCodec<ByteBuf, SilencerSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SilencerSyncPacket::pos,
        ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC), SilencerSyncPacket::sounds,
        SilencerSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     *
     */
    public void handler(IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity entity = context.player().level().getBlockEntity(this.pos);
            if (entity instanceof ActiveSilencerBlockEntity silencer) {
                silencer.sync(context.player(), this.sounds);
            }
        });
    }
}
