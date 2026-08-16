package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CursedGoldBlock extends Block {
    public CursedGoldBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return -1.0F;
    }
}
