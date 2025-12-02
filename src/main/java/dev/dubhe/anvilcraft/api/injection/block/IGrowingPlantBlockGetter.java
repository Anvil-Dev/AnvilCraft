package dev.dubhe.anvilcraft.api.injection.block;

import net.minecraft.core.Direction;

public interface IGrowingPlantBlockGetter {
    default Direction anvilcraft$getGrowthDirection() {
        throw new UnsupportedOperationException();
    }
}
