package dev.dubhe.anvilcraft.event.anvil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface AnvilBehavior {
    List<AnvilBehavior> BEHAVIORS = new ArrayList<>();

    boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance);

    static void registerBehavior(AnvilBehavior behavior) {
        BEHAVIORS.add(behavior);
    }

    static void register() {
        registerBehavior((level, hitBlockPos, hitBlockState, fallDistance) -> {
            if (!hitBlockState.is(Blocks.COBBLESTONE)) return false;
            level.setBlockAndUpdate(hitBlockPos, Blocks.GRAVEL.defaultBlockState());
            return true;
        });
    }
}
