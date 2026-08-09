package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.laser.RubyLaserBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

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
        return this.isSwitchedOn() ? 1 : 0;
    }

    @Override
    public void tick(Level level) {
        this.resetState();
        if (this.getGrid() != null && this.getBlockState().getValue(RubyLaserBlock.OVERLOAD) == this.getGrid().isWorking()) {
            level.setBlock(
                this.getPos(),
                this.getBlockState().setValue(IPowerComponent.OVERLOAD, !this.getGrid().isWorking()),
                2
            );
        }
        if (level.hasNeighborSignal(this.getBlockPos()) == (this.getBlockState().getValue(IPowerComponent.SWITCH) == Switch.ON)) {
            level.setBlock(
                this.getPos(),
                this.getBlockState().setValue(
                    IPowerComponent.SWITCH,
                    level.hasNeighborSignal(this.getBlockPos())
                        ? Switch.OFF
                        : Switch.ON
                ),
                2);
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
        return this.getBlockState().getValue(RubyLaserBlock.SWITCH) == Switch.ON
               && !this.getBlockState().getValue(RubyLaserBlock.OVERLOAD);
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
        if (this.level == null) return 16;
        return this.getBlockState().getValue(RubyLaserBlock.SWITCH) == Switch.OFF ? 0 : 16;
    }

    @Override
    public float getLaserOffset() {
        return 0.489F;
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(RubyLaserBlock.FACING);
    }
}
