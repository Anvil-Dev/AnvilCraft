package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerTransmitter;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.state.Half;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class TransmissionPoleBlockEntity extends BlockEntity implements IPowerTransmitter {
    private PowerGrid grid;

    public TransmissionPoleBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.REMOTE_TRANSMISSION_POLE.get(), pos, blockState);
    }

    public static @NotNull TransmissionPoleBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new TransmissionPoleBlockEntity(type, pos, blockState);
    }

    private TransmissionPoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public @NotNull PowerComponentType getComponentType() {
        if (this.getLevel() == null) return PowerComponentType.INVALID;
        if (!this.getBlockState().is(ModBlocks.TRANSMISSION_POLE.get())) return PowerComponentType.INVALID;
        if (this.getBlockState().getValue(TransmissionPoleBlock.HALF) != Half.TOP) return PowerComponentType.INVALID;
        return PowerComponentType.TRANSMITTER;
    }
}
