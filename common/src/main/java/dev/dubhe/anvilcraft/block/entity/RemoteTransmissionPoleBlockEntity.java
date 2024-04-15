package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerTransmitter;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.RemoteTransmissionPoleBlock;
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
public class RemoteTransmissionPoleBlockEntity extends BlockEntity implements IPowerTransmitter {
    private PowerGrid grid;

    public RemoteTransmissionPoleBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.REMOTE_TRANSMISSION_POLE.get(), pos, blockState);
    }

    public static @NotNull RemoteTransmissionPoleBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new RemoteTransmissionPoleBlockEntity(type, pos, blockState);
    }

    private RemoteTransmissionPoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public @NotNull PowerComponentType getComponentType() {
        if (this.getLevel() == null) return PowerComponentType.INVALID;
        BlockState state = this.getLevel().getBlockState(this.getPos());
        if (!state.is(ModBlocks.REMOTE_TRANSMISSION_POLE.get())) return PowerComponentType.INVALID;
        if (state.getValue(RemoteTransmissionPoleBlock.HALF) != Half.TOP) return PowerComponentType.INVALID;
        return PowerComponentType.TRANSMITTER;
    }
}
