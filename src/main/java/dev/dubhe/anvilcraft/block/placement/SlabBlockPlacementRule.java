package dev.dubhe.anvilcraft.block.placement;

import net.minecraft.world.level.block.SlabBlock;

/**
 * 所有台阶方块的 Fallback：放置消耗 1 个对应 ID 的 BlockItem。
 */
public final class SlabBlockPlacementRule extends ClassBlockPlacementRule<SlabBlock> {
    public static final SlabBlockPlacementRule INSTANCE = new SlabBlockPlacementRule();

    private SlabBlockPlacementRule() {
        super(SlabBlock.class);
    }
}
