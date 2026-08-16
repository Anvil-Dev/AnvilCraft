package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantedGoldBlock extends Block {
    public EnchantedGoldBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return 3.0F;
    }
}
