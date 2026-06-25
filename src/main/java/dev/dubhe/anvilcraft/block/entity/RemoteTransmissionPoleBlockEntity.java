package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.power.transmitting.RemoteTransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RemoteTransmissionPoleBlockEntity extends AbstractTransmissionPoleBlockEntity {
    public RemoteTransmissionPoleBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.REMOTE_TRANSMISSION_POLE.get(), pos, blockState);
    }

    private RemoteTransmissionPoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static RemoteTransmissionPoleBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new RemoteTransmissionPoleBlockEntity(type, pos, blockState);
    }

    @Override
    public boolean isHead(BlockState state) {
        return state.getValue(RemoteTransmissionPoleBlock.HALF) == Vertical4PartHalf.TOP;
    }

    @Override
    public BlockPos getHeadPos(BlockPos bottom) {
        return bottom.offset(Vertical4PartHalf.TOP.getOffset());
    }

    @Override
    public int getRange() {
        return AnvilCraft.CONFIG.remotePowerTransmitterRange;
    }
}
