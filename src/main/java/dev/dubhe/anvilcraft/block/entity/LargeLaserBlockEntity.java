package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.laser.LargeLaserBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LargeLaserBlockEntity extends BaseLaserBlockEntity implements IPowerConsumer {
    @Getter
    @Setter
    @Nullable
    private PowerGrid grid;

    public LargeLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected int getBaseLaserLevel() {
        return this.isSwitchedOn() ? 16 : 0;
    }

    @Override
    public void tick(Level level) {
        this.resetState();
        if (this.level == null) {
            return;
        }
        if (this.grid == null) {
            return;
        }
        if (!this.getBlockState().getValue(LargeLaserBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
        if (!(this.getBlockState().getBlock() instanceof LargeLaserBlock block)) return;
        if (this.grid.isWorking() && this.getBlockState().getValue(LargeLaserBlock.OVERLOAD)) {
            block.updateState(this.level, this.getBlockPos(), LargeLaserBlock.OVERLOAD, false, 3);
        } else if (!this.grid.isWorking() && !this.getBlockState().getValue(LargeLaserBlock.OVERLOAD)) {
            block.updateState(this.level, this.getBlockPos(), LargeLaserBlock.OVERLOAD, true, 3);
        }
        if (this.isSwitchedOn()) {
            this.emitLaser(this.getFacing());
        } else {
            if (this.irradiateBlockPos != null
                && level.getBlockEntity(this.irradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity
            ) {
                irradiateBlockEntity.onCancelingIrradiation(this);
            }
            this.updateIrradiateBlockPos(null);
        }
        super.tick(level);
    }

    public boolean isSwitchedOn() {
        return this.getBlockState().getValue(LargeLaserBlock.SWITCH) == Switch.ON
               && !this.getBlockState().getValue(LargeLaserBlock.OVERLOAD);
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public int getInputPower() {
        if (this.level == null) return 256;
        return this.getBlockState().getValue(LargeLaserBlock.SWITCH) == Switch.OFF ? 0 : 256;
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(LargeLaserBlock.FACING);
    }

    @Override
    public PowerComponentType getComponentType() {
        if (this.level == null) return PowerComponentType.INVALID;
        if (!this.level.getBlockState(this.getBlockPos()).hasProperty(LargeLaserBlock.HALF)) {
            return PowerComponentType.INVALID;
        }
        if (this.level.getBlockState(this.getBlockPos()).getValue(LargeLaserBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) {
            return PowerComponentType.CONSUMER;
        } else {
            return PowerComponentType.INVALID;
        }
    }

    @Override
    public float getLaserOffset() {
        return 1f;
    }

    @Override
    public int getRange() {
        return 1;
    }
}
