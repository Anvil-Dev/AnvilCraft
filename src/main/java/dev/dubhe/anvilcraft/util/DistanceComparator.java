package dev.dubhe.anvilcraft.util;

import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class DistanceComparator implements Comparator<Vec3> {

    private final Vec3 center;

    public DistanceComparator(Vec3 center) {
        this.center = center;
    }

    @Override
    public int compare(Vec3 o1, Vec3 o2) {
        return Double.compare(this.center.distanceTo(o1), this.center.distanceTo(o2));
    }
}
