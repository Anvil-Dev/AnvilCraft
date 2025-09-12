package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.LaserReceiverBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class LaserReceiverBlockEntity extends BaseLaserBlockEntity implements IPowerProducer {
    @Getter
    @Setter
    private PowerGrid grid;
    private int delay = 0;
    private double efficiency = 0;
    private double tempEfficiency = 0;
    private int power = 0;

    public LaserReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getOutputPower() {
        return Math.min((int) Math.floor(efficiency), power);
    }

    @Override
    public void tick(Level level) {
        updateLaserLevel(calculateLaserLevel());
        if (changed) {
            if (laserLevel > 0) {
                level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(LaserReceiverBlock.ACTIVE, true));
            } else {
                level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(LaserReceiverBlock.ACTIVE, false));
            }
            efficiency = 0;
            tempEfficiency = 0;
            delay = 0;
            power = laserLevel * 15;
        }
        if (getBlockState().getValue(LaserReceiverBlock.ACTIVE) && !changed) {
            if (efficiency < power) {
                delay++;
                tempEfficiency += power * 0.005;
                if (delay >= 20) {
                    delay = 0;
                    efficiency = tempEfficiency;
                }
            }
        }
        super.tick(level);
        resetState();
    }
    
    @Override
    protected int getBaseLaserLevel() {
        return 0;
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(LaserReceiverBlock.FACING).getOpposite();
    }

    @Override
    public Set<Direction> getIgnoreFace() {
        return Set.of(getBlockState().getValue(LaserReceiverBlock.FACING));
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
