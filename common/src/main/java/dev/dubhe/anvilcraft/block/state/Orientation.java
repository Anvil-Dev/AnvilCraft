package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum Orientation implements StringRepresentable {
    NORTH_UP("north_up"),
    SOUTH_UP("south_up"),
    WEST_UP("west_up"),
    EAST_UP("east_up"),
    UP_NORTH("up_north"),
    UP_SOUTH("up_south"),
    UP_WEST("up_west"),
    UP_EAST("up_east"),
    DOWN_NORTH("down_north"),
    DOWN_SOUTH("down_south"),
    DOWN_WEST("down_west"),
    DOWN_EAST("down_east");
    private final String name;

    Orientation(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    /**
     * 获取相反的方向
     */
    public Orientation opposite() {
        return switch (this) {
            default -> Orientation.SOUTH_UP;
            case SOUTH_UP -> Orientation.NORTH_UP;
            case WEST_UP -> Orientation.EAST_UP;
            case EAST_UP -> Orientation.WEST_UP;
            case UP_NORTH -> Orientation.DOWN_NORTH;
            case UP_SOUTH -> Orientation.DOWN_SOUTH;
            case UP_WEST -> Orientation.DOWN_WEST;
            case UP_EAST -> Orientation.DOWN_EAST;
            case DOWN_NORTH -> Orientation.UP_NORTH;
            case DOWN_SOUTH -> Orientation.UP_SOUTH;
            case DOWN_WEST -> Orientation.UP_WEST;
            case DOWN_EAST -> Orientation.UP_EAST;
        };
    }
}
