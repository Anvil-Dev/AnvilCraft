package dev.dubhe.anvilcraft.block.item;

import net.minecraft.world.level.block.Block;

public class InfiniteCollectorBlockItem extends PlacementIntervalsBlockItem {
    public InfiniteCollectorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean doRangeNoOverlap() {
        return true;
    }

    @Override
    public int getIntervalsRadius() {
        return 6;
    }
}
