package dev.dubhe.anvilcraft.block.cauldron;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ObsidianCauldronBlock extends BaseCauldronBlock implements IHammerRemovable {
    public ObsidianCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.OBSIDIAN);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return simpleCodec(ObsidianCauldronBlock::new);
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return 0.9375;
    }

    @Override
    public boolean isFull(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return 3;
    }
}
