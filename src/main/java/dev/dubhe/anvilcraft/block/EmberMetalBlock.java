package dev.dubhe.anvilcraft.block;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

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
    public boolean isRandomlyTicking(@NotNull BlockState state) {
        return true;
    }

    @Override
    public void randomTick(
            @NotNull BlockState state,
            @NotNull ServerLevel level,
            @NotNull BlockPos pos,
            @NotNull RandomSource random
    ) {
        if (random.nextDouble() <= waterAbsorptionChance) {
            tryAbsorbWater(level, pos);
        }
    }
}
