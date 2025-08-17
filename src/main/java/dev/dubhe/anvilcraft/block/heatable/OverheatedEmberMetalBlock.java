package dev.dubhe.anvilcraft.block.heatable;

import dev.dubhe.anvilcraft.api.block.IOverheatedEmberBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class OverheatedEmberMetalBlock extends OverheatedBlock implements IOverheatedEmberBlock {
    public OverheatedEmberMetalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Optional<BlockState> getPrevTier(Level level, BlockPos pos, BlockState state) {
        if (level.random.nextFloat() <= 0.05f) return Optional.of(Blocks.NETHERITE_BLOCK.defaultBlockState());
        return super.getPrevTier(level, pos, state);
    }
}
