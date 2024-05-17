package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public class InductionLightBlockEntity extends BlockEntity implements IPowerConsumer {

    private PowerGrid grid;

    public InductionLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static InductionLightBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new InductionLightBlockEntity(type, pos, blockState);
    }

    public void tick(Level level1) {
        flushState(level1, getBlockPos());
    }

    @Override
    public int getInputPower() {
        return 1;
    }

    @Override
    public Level getCurrentLevel() {
        return level;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return getBlockPos();
    }
}
