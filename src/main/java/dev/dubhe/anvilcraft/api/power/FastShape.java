package dev.dubhe.anvilcraft.api.power;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class FastShape {
    private final List<AABB> shapes;

    public FastShape(List<AABB> shapes) {
        this.shapes = new ArrayList<>(shapes);
    }

    public static FastShape create(List<AABB> shapes) {
        return new FastShape(shapes);
    }

    public boolean inRange(Vec3 pos) {
        for (AABB aabb : shapes) {
            if (aabb.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean intersects(AABB box) {
        for (AABB aabb : shapes) {
            if (aabb.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    public void add(AABB shape) {
        shapes.add(shape);
    }
}
