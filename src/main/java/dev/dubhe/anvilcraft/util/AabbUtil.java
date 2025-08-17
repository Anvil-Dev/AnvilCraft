package dev.dubhe.anvilcraft.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AabbUtil {
    public static AABB create(Vec3i start, Vec3i end) {
        return new AABB(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
    }

    public static AABB centerSectionTo3x3x3(BlockPos pos) {
        return centerSectionTo3x3x3(SectionPos.of(pos));
    }

    public static AABB centerSectionTo3x3x3(SectionPos center) {
        return new AABB(
            center.minBlockX() - 16,
            center.minBlockY() - 16,
            center.minBlockZ() - 16,
            center.maxBlockX() + 16 + 1,
            center.maxBlockY() + 16 + 1,
            center.maxBlockZ() + 16 + 1
        );
    }

    public static AABB minmax(AABB a, Vec3i other) {
        return minmax(a, other.getX(), other.getY(), other.getZ());
    }

    public static AABB minmax(AABB a, double x, double y, double z) {
        double minX = Math.min(a.minX, x);
        double minY = Math.min(a.minY, y);
        double minZ = Math.min(a.minZ, z);
        double maxX = Math.max(a.maxX, x);
        double maxY = Math.max(a.maxY, y);
        double maxZ = Math.max(a.maxZ, z);
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
