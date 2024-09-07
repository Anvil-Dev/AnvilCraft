package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class AABBUtil {
    public static AABB create(
        BlockPos a,
        BlockPos b
    ){
        return new AABB(
            a.getX(),
            a.getY(),
            a.getZ(),
            b.getX(),
            b.getY(),
            b.getZ()
        );
    }
}
