package dev.dubhe.anvilcraft.api.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A heater that damages entities standing on it or in a cauldron above it.
 */
public interface IDamagingHeater {
    boolean isActive(BlockState state);
}
