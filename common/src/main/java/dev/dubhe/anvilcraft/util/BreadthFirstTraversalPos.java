package dev.dubhe.anvilcraft.util;

import net.minecraft.core.Vec3i;

public enum BreadthFirstTraversalPos {
    TOP_CENTER(0, 1, 0),
    TOP_WEST(-1, 1, 0),
    TOP_NORTH(0, 1, -1),
    TOP_EAST(1, 1, 0),
    TOP_SOUTH(0, 1, 1),
    TOP_NORTH_WEST(-1, 1, -1),
    TOP_NORTH_EAST(1, 1, -1),
    TOP_SOUTH_WEST(-1, 1, 1),
    TOP_SOUTH_EAST(1, 1, 1),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    NORTH_WEST(-1, 0, -1),
    NORTH_EAST(1, 0, -1),
    SOUTH_WEST(-1, 0, 1),
    SOUTH_EAST(1, 0, 1),
    BOTTOM_CENTER(0, -1, 0),
    BOTTOM_WEST(-1, -1, 0),
    BOTTOM_NORTH(0, -1, -1),
    BOTTOM_EAST(1, -1, 0),
    BOTTOM_SOUTH(0, -1, 1),
    BOTTOM_NORTH_WEST(-1, -1, -1),
    BOTTOM_NORTH_EAST(1, -1, -1),
    BOTTOM_SOUTH_WEST(-1, -1, 1),
    BOTTOM_SOUTH_EAST(1, -1, 1);
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    BreadthFirstTraversalPos(int offsetX, int offsetY, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public Vec3i getOffset() {
        return new Vec3i(offsetX, offsetY, offsetZ);
    }
}
