package dev.dubhe.anvilcraft.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyHelper {
    /**
     * 塞能量
     */
    public static void insertEnergy(Level level, BlockPos pos, Direction direction, int amount) {
        IEnergyStorage energyStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
        if (energyStorage == null) return;
        energyStorage.receiveEnergy(amount, false);
    }
}
