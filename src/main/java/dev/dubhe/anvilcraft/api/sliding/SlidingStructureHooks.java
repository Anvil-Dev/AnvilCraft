package dev.dubhe.anvilcraft.api.sliding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 滑轨结构解析扩展：推动反应改写与解析成功后的额外收集。
 * 在服务器线程调用；{@link #modifyPushReaction} 不得改变世界状态。
 */
public final class SlidingStructureHooks {
    private static final List<SlidingStructureExtension> EXTENSIONS = new CopyOnWriteArrayList<>();

    private SlidingStructureHooks() {
    }

    public static void register(SlidingStructureExtension extension) {
        EXTENSIONS.add(extension);
    }

    public static PushReaction modifyPushReaction(
        Level level,
        BlockPos pos,
        BlockState state,
        PushReaction original,
        Direction pushDirection
    ) {
        PushReaction reaction = original;
        for (SlidingStructureExtension extension : EXTENSIONS) {
            reaction = extension.modifyPushReaction(level, pos, state, reaction, pushDirection);
        }
        return reaction;
    }

    public static boolean expand(SlidingBlockStructureResolver resolver) {
        for (SlidingStructureExtension extension : EXTENSIONS) {
            if (!extension.expand(resolver)) return false;
        }
        return true;
    }
}
