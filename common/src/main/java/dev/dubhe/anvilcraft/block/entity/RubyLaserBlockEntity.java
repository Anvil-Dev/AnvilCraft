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
public class RubyLaserBlockEntity extends BaseLaserBlockEntity implements IPowerConsumer {
    @Setter
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
    public void tick(@NotNull Level level) {
        if (getBlockState().getValue(RubyLaserBlock.OVERLOAD)
            == getBlockState().getValue(RubyLaserBlock.SWITCH).equals(Switch.ON))
            level.setBlock(
                getPos(),
                getBlockState()
                    .setValue(SWITCH, getBlockState().getValue(RubyLaserBlock.OVERLOAD) ? Switch.OFF : Switch.ON),
                2);
        if (isSwitch()) emitLaser(getDirection());
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
        return this.getBlockState().getValue(RubyLaserBlock.FACING);
    }
}
