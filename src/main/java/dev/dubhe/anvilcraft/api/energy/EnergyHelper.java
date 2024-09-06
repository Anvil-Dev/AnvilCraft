package dev.dubhe.anvilcraft.api.energy;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class EnergyHelper {
    @ExpectPlatform
    public static void insertEnergy(Level level, BlockPos pos, Direction direction, int amount) {
        throw new AssertionError();
    }
}
