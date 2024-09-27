package dev.dubhe.anvilcraft.block;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EmberMetalBlock extends Block implements EmberBlock {
    private final double waterAbsorptionChance;

    @Getter
    @Setter
    private BlockState checkBlockState;

    public EmberMetalBlock(Properties properties, double waterAbsorptionChance) {
        super(properties);
        this.waterAbsorptionChance = waterAbsorptionChance;
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
        if (random.nextDouble() <= waterAbsorptionChance) {
            tryAbsorbWater(level, pos);
        }
    }
}
