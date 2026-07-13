package dev.dubhe.anvilcraft.block.entity.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pipe node block entity.
 *
 * <p>Pipe nodes are not fluid containers. The block entity only exists so node check-valve data
 * can be stored and synced for rendering.
 */
public class PipeNodeBlockEntity extends AbstractPipeBlockEntity {
    protected PipeNodeBlockEntity(BlockEntityType<PipeNodeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static PipeNodeBlockEntity create(BlockEntityType<PipeNodeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeNodeBlockEntity(type, pos, blockState);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
