package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class HeatCollectorBlockItem extends PlacementIntervalsBlockItem {
    public HeatCollectorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean doRangeNoOverlap() {
        return true;
    }

    @Override
    public int getIntervalsRadius() {
        return 4;
    }

    @Override
    protected int getIntervalsRadius(BlockState state) {
        return state.is(ModBlocks.INFINITE_COLLECTOR) ? 5 : super.getIntervalsRadius(state);
    }

    @Override
    protected List<Block> getIntervalBlocks() {
        return List.of(ModBlocks.HEAT_COLLECTOR.get(), ModBlocks.INFINITE_COLLECTOR.get());
    }

    @Override
    protected int getMaxIntervalsRadius() {
        return 5;
    }
}
