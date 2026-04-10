package dev.dubhe.anvilcraft.block.cfa.interfaces;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class CelestialForgingAnvilLogisticsInterfaceBlock extends CelestialForgingAnvilInterfaceBlock {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(CelestialForgingAnvilLogisticsInterfaceBlock::new);
    }

    public CelestialForgingAnvilLogisticsInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
