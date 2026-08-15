package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MassEnergyInverterBlockEntity extends BlockEntity implements IPowerConsumer {
    public static final int POWER_CONSUMPTION = 1024;
    public static final long MASS_PER_TICK = 5;

    @Getter
    @Setter
    private @Nullable PowerGrid grid;

    public MassEnergyInverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public MassEnergyInverterBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.MASS_ENERGY_INVERTER.get(), pos, blockState);
    }

    public static MassEnergyInverterBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState state
    ) {
        return new MassEnergyInverterBlockEntity(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MassEnergyInverterBlockEntity blockEntity) {
        if (level.isClientSide) return;
        if (!blockEntity.isGridWorking()) return;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            if (level.getBlockEntity(neighborPos) instanceof SpaceOvercompressorBlockEntity compressor) {
                compressor.injectMass(MASS_PER_TICK);
            }
        }
    }

    @Override
    public int getInputPower() {
        return POWER_CONSUMPTION;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }
}