package dev.dubhe.anvilcraft.api.energy.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class EnergyHelperImpl {
    public static void insertEnergy(Level level, BlockPos pos, Direction direction, int amount) {
        try {
            Class.forName("team.reborn.energy.api.EnergyStorage");
            EnergyInsert.insertEnergy(level, pos, direction, amount);
        } catch (ClassNotFoundException ignore) {

        }
    }
}
