package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.RipeningManager;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
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

    private int rangeSize = 5;
    private boolean registered = false;

    public InductionLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static InductionLightBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new InductionLightBlockEntity(type, pos, blockState);
    }

    /**
     *
     */
    public void tick(Level level1) {
        flushState(level1, getBlockPos());
        if (!registered) {
            registered = true;
            RipeningManager.addLightBlock(getBlockPos(), level);
        }
    }

    @Override
    public int getInputPower() {
        if (level == null) return 1;
        return getBlockState()
            .getValue(InductionLightBlock.POWERED)
            ? 0 : getBlockState().getValue(InductionLightBlock.COLOR).dissipation;
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
