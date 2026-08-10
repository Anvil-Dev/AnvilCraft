package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.block.entity.ITickable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerTransmitter;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public abstract class AbstractTransmissionPoleBlockEntity extends BlockEntity implements IPowerTransmitter, ITickable {
    private @Nullable PowerGrid grid;

    public AbstractTransmissionPoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.getLevel();
    }

    @Override
    public BlockPos getPos() {
        return this.getHeadPos(this.getBlockPos());
    }

    @Override
    public PowerComponentType getComponentType() {
        if (this.getLevel() == null || !this.getType().isValid(this.getBlockState())) {
            return PowerComponentType.INVALID;
        }
        return PowerComponentType.TRANSMITTER;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean isHead(BlockState state);

    public abstract BlockPos getHeadPos(BlockPos bottom);

    @Override
    public void tick() {
        Level level = this.getLevel();
        if (level == null) return;
        BlockState state = level.getBlockState(this.getPos());
        if (!this.getType().isValid(state)) return;
        if (!this.isHead(state)) return;

        if (state.getValue(IPowerComponent.SWITCH) == Switch.OFF && this.getGrid() != null) {
            this.getGrid().remove(this);
        } else if (state.getValue(IPowerComponent.SWITCH) == Switch.ON && this.getGrid() == null) {
            PowerGrid.addComponent(this);
        }
        this.flushState(level, this.getPos());
    }
}
