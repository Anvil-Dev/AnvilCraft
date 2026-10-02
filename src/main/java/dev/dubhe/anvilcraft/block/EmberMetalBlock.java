package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.IEmberBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EmberMetalBlock extends Block implements IEmberBlock {
    private final boolean cut;

    @Getter
    @Setter
    private BlockState checkBlockState;

    /**
     * @param cut 是否为切割变体，用于选择对应的吸水概率配置
     */
    public EmberMetalBlock(Properties properties, boolean cut) {
        super(properties);
        this.cut = cut;
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
        double chance = cut
            ? AnvilCraft.CONFIG.world.cutEmberMetalBlockWaterAbsorptionChance
            : AnvilCraft.CONFIG.world.emberBlockWaterAbsorptionChance;
        if (random.nextDouble() <= chance) {
            tryAbsorbWater(level, pos);
        }
    }
}
