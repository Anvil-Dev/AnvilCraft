package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public record SilencerSyncPacket(BlockPos pos, List<ResourceLocation> sounds) implements IInsensitiveBiPacket {
    public static final Type<SilencerSyncPacket> TYPE = IPacket.type(AnvilCraft.of("silencer_sync"));
    public static final StreamCodec<ByteBuf, SilencerSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        SilencerSyncPacket::pos,
        ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC),
        SilencerSyncPacket::sounds,
        SilencerSyncPacket::new
    );

    @Override
    public Type<SilencerSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (!player.level().isLoaded(this.pos)) {
            return;
        }
        BlockEntity entity = player.level().getBlockEntity(this.pos);
        if (entity instanceof ActiveSilencerBlockEntity silencer) {
            silencer.sync(player, this.sounds);
        }
    }
}
