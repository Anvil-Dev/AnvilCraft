package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.batch.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.inventory.BatchCrafterMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record BatchCrafterSelectPacket(int selecting, BlockPos pos) implements IInsensitiveBiPacket {
    public static final Type<BatchCrafterSelectPacket> TYPE = IPacket.type(AnvilCraft.of("batch_crafter_select"));
    public static final StreamCodec<ByteBuf, BatchCrafterSelectPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        BatchCrafterSelectPacket::selecting,
        BlockPos.STREAM_CODEC,
        BatchCrafterSelectPacket::pos,
        BatchCrafterSelectPacket::new
    );

    @Override
    public Type<BatchCrafterSelectPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        Level level = player.level();
        if (!level.isLoaded(this.pos)) return;

        Optional<BatchCrafterBlockEntity> be = level.getBlockEntity(this.pos, ModBlockEntities.BATCH_CRAFTER.get());
        if (be.isEmpty()) return;

        if (!level.isClientSide
            && (!(player.containerMenu instanceof BatchCrafterMenu menu) || menu.getBlockEntity() != be.get())) {
            return;
        }
        be.get().setSelecting(this.selecting);
        if (player.containerMenu instanceof BatchCrafterMenu menu
            && menu.getBlockEntity() == be.get()) {
            menu.onChanged();
        }
    }
}
