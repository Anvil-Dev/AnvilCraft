package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public record BatchCutterSelectPacket(int selecting, BlockPos pos) implements IInsensitiveBiPacket {
    public static final Type<BatchCutterSelectPacket> TYPE = IPacket.type(AnvilCraft.of("batch_cutter_select"));
    public static final StreamCodec<ByteBuf, BatchCutterSelectPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        BatchCutterSelectPacket::selecting,
        BlockPos.STREAM_CODEC,
        BatchCutterSelectPacket::pos,
        BatchCutterSelectPacket::new
    );

    @Override
    public Type<BatchCutterSelectPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        Level level = player.level();
        level.getBlockEntity(this.pos, ModBlockEntities.BATCH_CUTTER.get())
            .ifPresent(entity -> entity.setSelecting(this.selecting));
        if (player instanceof ServerPlayer) {
            PacketDistributor.sendToAllPlayers(new BatchCutterSelectPacket(this.selecting, this.pos));
        }
    }
}
