package dev.dubhe.anvilcraft.block.state;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

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
    public String getSerializedName() {
        return this.name;
    }

    /**
     * 获取相反的方向
     */
    public Orientation opposite() {
        return switch (this) {
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
            default -> Orientation.SOUTH_UP;
        };
    }

    /**
     * 获取下一个的方向
     */
    public Orientation next() {
        return switch (this) {
            case SOUTH_UP -> Orientation.WEST_UP;
            case WEST_UP -> Orientation.EAST_UP;
            case EAST_UP -> Orientation.UP_NORTH;
            case UP_NORTH -> Orientation.UP_SOUTH;
            case UP_SOUTH -> Orientation.UP_WEST;
            case UP_WEST -> Orientation.UP_EAST;
            case UP_EAST -> Orientation.DOWN_NORTH;
            case DOWN_NORTH -> Orientation.DOWN_SOUTH;
            case DOWN_SOUTH -> Orientation.DOWN_WEST;
            case DOWN_WEST -> Orientation.DOWN_EAST;
            case DOWN_EAST -> Orientation.NORTH_UP;
            default -> Orientation.SOUTH_UP;
        };
    }

    public Orientation rotate(Rotation rotation) {
        switch (rotation) {
            case Rotation.NONE -> {
                return this;
            }
            case Rotation.CLOCKWISE_90 -> {
                return switch (this) {
                    case NORTH_UP -> Orientation.EAST_UP;
                    case SOUTH_UP -> Orientation.WEST_UP;
                    case WEST_UP -> Orientation.NORTH_UP;
                    case EAST_UP -> Orientation.SOUTH_UP;
                    case UP_NORTH -> Orientation.UP_EAST;
                    case UP_SOUTH -> Orientation.UP_WEST;
                    case UP_WEST -> Orientation.UP_NORTH;
                    case UP_EAST -> Orientation.UP_SOUTH;
                    case DOWN_NORTH -> Orientation.DOWN_EAST;
                    case DOWN_SOUTH -> Orientation.DOWN_WEST;
                    case DOWN_WEST -> Orientation.DOWN_NORTH;
                    case DOWN_EAST -> Orientation.DOWN_SOUTH;
                };
            }
            case Rotation.COUNTERCLOCKWISE_90 -> {
                return switch (this) {
                    case NORTH_UP -> Orientation.WEST_UP;
                    case SOUTH_UP -> Orientation.EAST_UP;
                    case WEST_UP -> Orientation.SOUTH_UP;
                    case EAST_UP -> Orientation.NORTH_UP;
                    case UP_NORTH -> Orientation.UP_WEST;
                    case UP_SOUTH -> Orientation.UP_EAST;
                    case UP_WEST -> Orientation.UP_SOUTH;
                    case UP_EAST -> Orientation.UP_NORTH;
                    case DOWN_NORTH -> Orientation.DOWN_WEST;
                    case DOWN_SOUTH -> Orientation.DOWN_EAST;
                    case DOWN_WEST -> Orientation.DOWN_SOUTH;
                    case DOWN_EAST -> Orientation.DOWN_NORTH;
                };
            }
            default -> {
                // rotate clockwise 180
                return switch (this) {
                    case NORTH_UP -> Orientation.SOUTH_UP;
                    case SOUTH_UP -> Orientation.NORTH_UP;
                    case WEST_UP -> Orientation.EAST_UP;
                    case EAST_UP -> Orientation.WEST_UP;
                    case UP_NORTH -> Orientation.UP_SOUTH;
                    case UP_SOUTH -> Orientation.UP_NORTH;
                    case UP_WEST -> Orientation.UP_EAST;
                    case UP_EAST -> Orientation.UP_WEST;
                    case DOWN_NORTH -> Orientation.DOWN_SOUTH;
                    case DOWN_SOUTH -> Orientation.DOWN_NORTH;
                    case DOWN_WEST -> Orientation.DOWN_EAST;
                    case DOWN_EAST -> Orientation.DOWN_WEST;
                };
            }
        }
    }

    /**
     * 朝向上下时翻转水平方向
     * 用于获取放置器假玩家的Orientation
     */
    public Orientation flipHorizontalIfVertical() {
        return switch (this) {
            case UP_NORTH, UP_SOUTH, UP_WEST, UP_EAST, DOWN_NORTH, DOWN_SOUTH, DOWN_WEST, DOWN_EAST -> this.rotate(Rotation.CLOCKWISE_180);
            default -> this;
        };
    }

    public Orientation mirror(Mirror mirror) {
        switch (mirror) {
            case NONE -> {
                return this;
            }
            case FRONT_BACK -> {
                return switch (this) {
                    case WEST_UP -> Orientation.EAST_UP;
                    case EAST_UP -> Orientation.WEST_UP;
                    case UP_WEST -> Orientation.UP_EAST;
                    case UP_EAST -> Orientation.UP_WEST;
                    case DOWN_WEST -> Orientation.DOWN_EAST;
                    case DOWN_EAST -> Orientation.DOWN_WEST;
                    default -> this;
                };
            }
            default -> {
                // mirror left_right (invert z)
                return switch (this) {
                    case NORTH_UP -> Orientation.SOUTH_UP;
                    case SOUTH_UP -> Orientation.NORTH_UP;
                    case UP_NORTH -> Orientation.UP_SOUTH;
                    case UP_SOUTH -> Orientation.UP_NORTH;
                    case DOWN_NORTH -> Orientation.DOWN_SOUTH;
                    case DOWN_SOUTH -> Orientation.DOWN_NORTH;
                    default -> this;
                };
            }
        }
    }

    /**
     * 获取对应的方向
     */
    public Direction getDirection() {
        return switch (this) {
            case SOUTH_UP -> Direction.SOUTH;
            case WEST_UP -> Direction.WEST;
            case EAST_UP -> Direction.EAST;
            case UP_NORTH, UP_SOUTH, UP_WEST, UP_EAST -> Direction.UP;
            case DOWN_NORTH, DOWN_SOUTH, DOWN_WEST, DOWN_EAST -> Direction.DOWN;
            default -> Direction.NORTH;
        };
    }

    /**
     * 获取水平旋转
     */
    public float getYRotation() {
        return switch (this) {
            case WEST_UP, UP_WEST, DOWN_WEST -> 90;
            case SOUTH_UP, UP_SOUTH, DOWN_SOUTH -> 0;
            case EAST_UP, UP_EAST, DOWN_EAST -> -90;
            case NORTH_UP, UP_NORTH, DOWN_NORTH -> 180;
        };
    }

    /**
     * 获取垂直旋转
     */
    public float getXRotation() {
        return switch (this) {
            case NORTH_UP, SOUTH_UP, WEST_UP, EAST_UP -> 0;
            case UP_NORTH, UP_SOUTH, UP_WEST, UP_EAST -> -90;
            case DOWN_NORTH, DOWN_SOUTH, DOWN_WEST, DOWN_EAST -> 90;
        };
    }
}
