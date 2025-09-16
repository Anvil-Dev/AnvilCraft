package dev.dubhe.anvilcraft.block.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class UncontainableBlockItem extends BlockItem {
    public UncontainableBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
