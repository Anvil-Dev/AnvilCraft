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
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class RubyLaserBlockEntity extends BaseLaserBlockEntity implements IPowerConsumer {
    @Nullable
    private PowerGrid grid;

    private RubyLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static RubyLaserBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new RubyLaserBlockEntity(type, pos, blockState);
    }

    @Override
    protected int getBaseLaserLevel() {
        return isSwitchedOn() ? 1 : 0;
    }

    @Override
    public void tick(Level level) {
        this.resetState();
        if (getGrid() != null && getBlockState().getValue(RubyLaserBlock.OVERLOAD) == getGrid().isWorking()) {
            level.setBlock(
                getPos(),
                getBlockState().setValue(OVERLOAD, !getGrid().isWorking()),
                2
            );
        }
        if (level.hasNeighborSignal(getBlockPos()) == (getBlockState().getValue(SWITCH) == Switch.ON)) {
            level.setBlock(
                getPos(),
                getBlockState().setValue(
                    SWITCH,
                    level.hasNeighborSignal(getBlockPos())
                        ? Switch.OFF
                        : Switch.ON
                ),
                2);
        }
        if (isSwitchedOn()) {
            emitLaser(getFacing());
        } else {
            if (irradiateBlockPos != null
                && level.getBlockEntity(irradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity
            ) {
                irradiateBlockEntity.onCancelingIrradiation(irradiateBlockEntity);
            }
            updateIrradiateBlockPos(null);
        }
        super.tick(level);
    }

    public boolean isSwitchedOn() {
        return getBlockState().getValue(RubyLaserBlock.SWITCH) == Switch.ON
            && !getBlockState().getValue(RubyLaserBlock.OVERLOAD);
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return level;
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public int getInputPower() {
        if (level == null) return 16;
        return getBlockState().getValue(RubyLaserBlock.SWITCH) == Switch.OFF ? 0 : 16;
    }

    @Override
    public float getLaserOffset() {
        return 0.489f;
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(RubyLaserBlock.FACING);
    }
}
