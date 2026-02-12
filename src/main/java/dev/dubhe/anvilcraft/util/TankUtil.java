package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;

public class TankUtil {
    public static boolean isMengerStructure(BlockPos pos, int size) {
        if (size <= 0) return false;
        if (size != 1 && size%3 != 0) return false;

        return false;
    }
}
