package dev.dubhe.anvilcraft.block.placement;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Set;

/**
 * 仅有 {@code FACING}/{@code HORIZONTAL_FACING}/{@code ROTATION_16} 状态属性方块的 Fallback：
 * 放置消耗 1 个对应 ID 的 BlockItem。
 */
public final class FacingOnlyPlacementRule extends PropertyBlockPlacementRule {
    public static final FacingOnlyPlacementRule INSTANCE = new FacingOnlyPlacementRule();

    private FacingOnlyPlacementRule() {
        super(Set.of(
            BlockStateProperties.FACING,
            BlockStateProperties.HORIZONTAL_FACING,
            BlockStateProperties.ROTATION_16
        ));
    }
}
