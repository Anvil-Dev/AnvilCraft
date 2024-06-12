package dev.dubhe.anvilcraft.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class EnergyHelper {
    @ExpectPlatform
    public static void insertEnergy(Level level, BlockPos pos, Direction direction, int amount) {
        throw new AssertionError();
    }
}
