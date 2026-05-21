package dev.dubhe.anvilcraft.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class EnergyHelper {
    /**
     * 塞能量
     */
    public static void insertEnergy(Level level, BlockPos pos, Direction direction, int amount) {
        EnergyHandler energyStorage = level.getCapability(Capabilities.Energy.BLOCK, pos, direction);
        if (energyStorage == null) return;
        Transaction transaction = Transaction.openRoot();
        energyStorage.insert(amount, transaction);
        transaction.commit();
    }
}
