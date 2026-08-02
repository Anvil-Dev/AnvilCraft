package dev.dubhe.anvilcraft.block.deco;

import dev.dubhe.anvilcraft.api.block.IEmberBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;

public class EmberDecoOutlineBlock extends StainedGlassBlock implements IEmberBlock {
    public EmberDecoOutlineBlock(Properties properties) {
        super(DyeColor.YELLOW, properties);
    }

    @Override
    public BlockState getCheckBlockState() {
        return this.defaultBlockState();
    }

    @Override
    public void setCheckBlockState(BlockState blockState) {
    }

    @Override
    public void tryAbsorbWater(Level level, BlockPos pos) {
    }
}
