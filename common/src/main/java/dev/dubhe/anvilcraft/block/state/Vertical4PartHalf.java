package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum Vertical4PartHalf implements MultiplePartBlockState<Vertical4PartHalf> {
    TOP("top", 0, 3, 0),
    MID_UPPER("upper", 0, 2, 0),
    MID_LOWER("lower", 0, 1, 0),
    BOTTOM("bottom", 0, 0, 0);

    private final String name;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    Vertical4PartHalf(String name, int offsetX, int offsetY, int offsetZ) {
        this.name = name;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
