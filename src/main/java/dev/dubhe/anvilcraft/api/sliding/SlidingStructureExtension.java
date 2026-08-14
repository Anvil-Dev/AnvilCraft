package dev.dubhe.anvilcraft.api.sliding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * 附属可改写滑轨结构解析中的推动反应，并在原版解析成功后追加胶粘/粘性方块。
 * 返回 {@code false} 表示结构无法移动。
 */
public interface SlidingStructureExtension {
    default PushReaction modifyPushReaction(
        Level level,
        BlockPos pos,
        BlockState state,
        PushReaction original,
        Direction pushDirection
    ) {
        return original;
    }

    default boolean expand(SlidingBlockStructureResolver resolver) {
        return true;
    }
}
