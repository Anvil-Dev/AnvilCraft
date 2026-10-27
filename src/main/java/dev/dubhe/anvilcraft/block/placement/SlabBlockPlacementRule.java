package dev.dubhe.anvilcraft.block.placement;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;

/**
 * 台阶方块按单层或双层消耗对应数量的方块物品。
 */
public final class SlabBlockPlacementRule extends ClassBlockPlacementRule<SlabBlock> {
    public static final SlabBlockPlacementRule INSTANCE = new SlabBlockPlacementRule();

    private SlabBlockPlacementRule() {
        super(SlabBlock.class);
    }

    @Override
    public List<PlacementItem> getPlacementItems(BlockState state) {
        int count = state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE ? 2 : 1;
        return simplePlacementItems(state.getBlock()).stream()
            .map(item -> new PlacementItem(item.placeStack().copyWithCount(count), item.returnStack()))
            .toList();
    }
}
