package dev.dubhe.anvilcraft.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AabbUtil {
    /**
     * 从方块坐标创建
     */
    public static AABB create(BlockPos a, BlockPos b) {
        return new AABB(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }
}
