package dev.dubhe.anvilcraft.block.placement;

import net.minecraft.world.level.block.TrapDoorBlock;

/**
 * 所有活板门方块的 Fallback：放置消耗 1 个对应 ID 的 BlockItem。
 */
public final class TrapDoorBlockPlacementRule extends ClassBlockPlacementRule<TrapDoorBlock> {
    public static final TrapDoorBlockPlacementRule INSTANCE = new TrapDoorBlockPlacementRule();

    private TrapDoorBlockPlacementRule() {
        super(TrapDoorBlock.class);
    }
}
