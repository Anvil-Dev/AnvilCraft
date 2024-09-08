package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.RubyLaserBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class RubyLaserBlockEntity extends BaseLaserBlockEntity implements IPowerConsumer {
    private PowerGrid grid;

    private RubyLaserBlockEntity(BlockEntityType<?> type,
                                 BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static @NotNull RubyLaserBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new RubyLaserBlockEntity(type, pos, blockState);
    }

    @Override
    protected int getBaseLaserLevel() {
        return isSwitch() ? 1 : 0;
    }

    @Override
    public void tick(@NotNull Level level) {
        if (getGrid() != null
                && getBlockState().getValue(RubyLaserBlock.OVERLOAD) == getGrid().isWork())
            level.setBlock(
                    getPos(),
                    getBlockState()
                            .setValue(OVERLOAD, !getGrid().isWork()),
                    2);
        if (level.hasNeighborSignal(getBlockPos()) == (getBlockState().getValue(SWITCH) == Switch.ON))
            level.setBlock(
                    getPos(),
                    getBlockState()
                            .setValue(SWITCH, level.hasNeighborSignal(getBlockPos()) ? Switch.OFF : Switch.ON),
                    2);
        if (isSwitch()) emitLaser(getDirection());
        else {
            if (irradiateBlockPos != null
                    && level.getBlockEntity(irradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity)
                irradiateBlockEntity.onCancelingIrradiation(irradiateBlockEntity);
            irradiateBlockPos = null;
        }
        super.tick(level);
    }

    @Override
    public boolean isSwitch() {
        return getBlockState().getValue(RubyLaserBlock.SWITCH) == Switch.ON
                && !getBlockState().getValue(RubyLaserBlock.OVERLOAD);
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
    }

    @Override
    public Level getCurrentLevel() {
        return level;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public int getInputPower() {
        if (level == null) return 16;
        return getBlockState().getValue(RubyLaserBlock.SWITCH) == Switch.OFF ? 0 : 16;
    }

    @Override
    public Direction getDirection() {
        return this.getBlockState().getValue(RubyLaserBlock.FACING);
    }
}
