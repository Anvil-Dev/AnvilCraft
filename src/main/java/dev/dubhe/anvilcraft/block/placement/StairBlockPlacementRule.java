package dev.dubhe.anvilcraft.block.placement;

import net.minecraft.world.level.block.StairBlock;

/**
 * 所有楼梯方块的 Fallback：放置消耗 1 个对应 ID 的 BlockItem。
 */
public final class StairBlockPlacementRule extends ClassBlockPlacementRule<StairBlock> {
    public static final StairBlockPlacementRule INSTANCE = new StairBlockPlacementRule();

    private StairBlockPlacementRule() {
        super(StairBlock.class);
    }
}
