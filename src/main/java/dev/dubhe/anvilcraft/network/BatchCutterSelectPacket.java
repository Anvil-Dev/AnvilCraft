package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.batch.BatchCutterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

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
        if (!level.isLoaded(this.pos)) {
            return;
        }
        Optional<BatchCutterBlockEntity> be = level.getBlockEntity(this.pos, ModBlockEntities.BATCH_CUTTER.get());
        if (be.isEmpty()) {
            return;
        }
        be.get().setSelecting(this.selecting);
    }
}
