package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.block.IEmberBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

@Getter
@Setter
public class EmberMetalPillarBlock extends RotatedPillarBlock implements IEmberBlock {
    private BlockState checkBlockState;

    public EmberMetalPillarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        if (random.nextDouble() <= 0.1) {
            tryAbsorbWater(level, pos);
        }
    }
}
