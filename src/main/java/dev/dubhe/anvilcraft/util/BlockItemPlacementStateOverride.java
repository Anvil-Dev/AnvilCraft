package dev.dubhe.anvilcraft.util;

import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class BlockItemPlacementStateOverride {
    private static final ThreadLocal<@Nullable BlockState> OVERRIDE = new ThreadLocal<>();

    private BlockItemPlacementStateOverride() {
    }

    public static void set(BlockState state) {
        BlockItemPlacementStateOverride.OVERRIDE.set(state);
    }

    public static void clear() {
        BlockItemPlacementStateOverride.OVERRIDE.remove();
    }

    public static @Nullable BlockState get() {
        return BlockItemPlacementStateOverride.OVERRIDE.get();
    }
}
