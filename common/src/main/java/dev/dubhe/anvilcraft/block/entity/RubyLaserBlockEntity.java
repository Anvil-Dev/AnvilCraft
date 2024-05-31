package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.RubyLaserBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RubyLaserBlockEntity extends BaseLaserBlockEntity implements IPowerConsumer {
    @Getter
    @Setter
    private PowerGrid grid;
    private Direction facing = Direction.UP;

    private RubyLaserBlockEntity(BlockEntityType<?> type,
        BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public RubyLaserBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.OVERSEER.get(), pos, blockState);
    }

    public static @NotNull RubyLaserBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new RubyLaserBlockEntity(type, pos, blockState);
    }

    @Override
    public void tick(@NotNull Level level) {
        if (
            level.getBlockState(getPos().relative(getBlockState().getValue(RubyLaserBlock.AXIS), 1)).is(
                ModBlocks.SILVER_BLOCK.get())
        ) {
            if (level.getBlockState(getPos().relative(getBlockState().getValue(RubyLaserBlock.AXIS), -1)).is(
                ModBlocks.SILVER_BLOCK.get())) {
                level.destroyBlock(getPos(), true);
                return;
            }
            level.setBlock(getPos(), getBlockState().setValue(RubyLaserBlock.SWITCH, Switch.ON), 2);
            switch (getBlockState().getValue(RubyLaserBlock.AXIS)) {
                case X -> facing = Direction.WEST;
                case Y -> facing = Direction.NORTH;
                case Z -> facing = Direction.DOWN;
            }
        } else if (
            level.getBlockState(getPos().relative(getBlockState().getValue(RubyLaserBlock.AXIS), -1)).is(
                ModBlocks.SILVER_BLOCK.get())
        ) {
            if (level.getBlockState(getPos().relative(getBlockState().getValue(RubyLaserBlock.AXIS), 1)).is(
                ModBlocks.SILVER_BLOCK.get())) {
                level.destroyBlock(getPos(), true);
                return;
            }
            level.setBlock(getPos(), getBlockState().setValue(RubyLaserBlock.SWITCH, Switch.ON), 2);
            switch (getBlockState().getValue(RubyLaserBlock.AXIS)) {
                case X -> facing = Direction.EAST;
                case Y -> facing = Direction.SOUTH;
                case Z -> facing = Direction.UP;
            }
        } else level.setBlock(getPos(), getBlockState().setValue(RubyLaserBlock.SWITCH, Switch.OFF), 2);
        if (isSwitch()) emitLaser(facing);
        super.tick(level);
    }

    @Override
    public boolean isSwitch() {
        return getBlockState().getValue(RubyLaserBlock.SWITCH) == Switch.ON;
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
        if (level == null) return;
        level.destroyBlock(getPos(), true);
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
        return 16;
    }

    @Override
    public Direction getDirection() {
        return facing;
    }
}
