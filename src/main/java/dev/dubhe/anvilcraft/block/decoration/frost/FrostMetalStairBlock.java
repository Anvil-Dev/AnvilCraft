package dev.dubhe.anvilcraft.block.decoration.frost;

import dev.dubhe.anvilcraft.api.block.IFrostBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FrostMetalStairBlock extends StairBlock implements IFrostBlock {
    public FrostMetalStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }
}
