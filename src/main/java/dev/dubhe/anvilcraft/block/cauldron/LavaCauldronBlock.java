package dev.dubhe.anvilcraft.block.cauldron;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class LavaCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable {
    public LavaCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.LAVA);
    }

    @Override
    protected void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effectApplier,
        boolean isPrecise
    ) {
        entity.lavaHurt();
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (level.isClientSide()) return;
        if (this.isFull(state)) level.setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        int layer = state.getValue(Layered4LevelCauldronBlock.LEVEL);
        return layer <= 2 ? layer : layer - 1;
    }
}
