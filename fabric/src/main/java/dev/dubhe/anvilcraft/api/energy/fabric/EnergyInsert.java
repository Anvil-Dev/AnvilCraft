package dev.dubhe.anvilcraft.api.energy.fabric;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.EnergyStorage;

public class EnergyInsert {
    /**
     * 能量插入
     */
    @SuppressWarnings("UnstableApiUsage")
    public static void insertEnergy(Level level, BlockPos pos, Direction direction, int amount) {
        EnergyStorage maybeStorage = EnergyStorage.SIDED.find(level, pos, direction);
        if (maybeStorage != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = maybeStorage.insert(amount, transaction);
                if (inserted > 0) {
                    transaction.commit();
                }
            }
        }
    }
}
