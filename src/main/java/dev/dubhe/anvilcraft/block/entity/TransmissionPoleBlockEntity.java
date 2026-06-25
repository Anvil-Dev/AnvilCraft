package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.power.transmitting.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Getter
@Setter
public class TransmissionPoleBlockEntity extends AbstractTransmissionPoleBlockEntity {
    public TransmissionPoleBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.TRANSMISSION_POLE.get(), pos, blockState);
    }

    private TransmissionPoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static TransmissionPoleBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new TransmissionPoleBlockEntity(type, pos, blockState);
    }

    @Override
    public boolean isHead(BlockState state) {
        return state.getValue(TransmissionPoleBlock.HALF) == Vertical3PartHalf.TOP;
    }

    @Override
    public BlockPos getHeadPos(BlockPos bottom) {
        return bottom.offset(Vertical3PartHalf.TOP.getOffset());
    }
}
