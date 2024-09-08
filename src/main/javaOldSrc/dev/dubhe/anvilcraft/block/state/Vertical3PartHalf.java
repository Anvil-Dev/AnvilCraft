package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum Vertical3PartHalf implements MultiplePartBlockState<Vertical3PartHalf> {
    TOP("top", 0, 2, 0),
    MID("mid", 0, 1, 0),
    BOTTOM("bottom", 0, 0, 0);

    private final String name;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    Vertical3PartHalf(String name, int offsetX, int offsetY, int offsetZ) {
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
