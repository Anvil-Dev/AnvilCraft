package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.block.placement.FacingOnlyPlacementRule;
import dev.dubhe.anvilcraft.block.placement.SlabBlockPlacementRule;
import dev.dubhe.anvilcraft.block.placement.StairBlockPlacementRule;
import dev.dubhe.anvilcraft.block.placement.TrapDoorBlockPlacementRule;

/**
 * 注册代码层的放置规则 Fallback 类，用于数据包中不存在对应规则的方块。
 */
public final class ModBlockPlacementFallbacks {
    private ModBlockPlacementFallbacks() {
    }

    public static void register() {
        BlockPlacementRules.registerFallback(SlabBlockPlacementRule.INSTANCE);
        BlockPlacementRules.registerFallback(StairBlockPlacementRule.INSTANCE);
        BlockPlacementRules.registerFallback(TrapDoorBlockPlacementRule.INSTANCE);
        BlockPlacementRules.registerFallback(FacingOnlyPlacementRule.INSTANCE);
    }
}
