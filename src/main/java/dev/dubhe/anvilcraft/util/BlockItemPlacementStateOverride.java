package dev.dubhe.anvilcraft.util;

import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class BlockItemPlacementStateOverride {
    private static final ThreadLocal<BlockState> OVERRIDE = new ThreadLocal<>();

    private BlockItemPlacementStateOverride() {
    }

    public static void set(BlockState state) {
        OVERRIDE.set(state);
    }

    public static void clear() {
        OVERRIDE.remove();
    }

    public static @Nullable BlockState get() {
        return OVERRIDE.get();
    }
}
